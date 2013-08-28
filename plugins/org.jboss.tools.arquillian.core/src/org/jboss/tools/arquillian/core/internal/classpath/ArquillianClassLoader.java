/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.classpath;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianClassLoader extends ClassLoader implements
		PrivilegedAction {
	
	private static final int DEFAULT_READING_SIZE = 8192;
	private ClassLoader sourceLoader;
	private Set<URL> URLs = new HashSet<URL>();
	
	private static final class Finder extends SecurityManager {
		public Class[] getClassContext() {
			return super.getClassContext();
		}
	}

	private IJavaProject jProject;
	private List<IPath> jars;
	private List<IJavaProject> dependentProjects = new ArrayList<IJavaProject>();
	private Map<String, URL> found = new HashMap<String, URL>();
	private List<String> notFound = new ArrayList<String>();
	private Set<IPath> outputLocations;
	static ClassLoader finderClassLoader;
	static Finder contextFinder;
	static {
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				finderClassLoader = ArquillianClassLoader.class
						.getClassLoader();
				contextFinder = new Finder();
				return null;
			}
		});
	}

	public void clear() {
		jars.clear();
		found.clear();
		notFound.clear();
		URLs.clear();
		dependentProjects.clear();
		outputLocations.clear();
		jProject = null;
	}

	public ArquillianClassLoader(ClassLoader contextClassLoader, IJavaProject jProject) {
		super(contextClassLoader);
		this.jProject = jProject;
		this.jars = getJarPaths(jProject);
		
		if (URLs.size() > 0) {
			URL[] urls = (URL[]) URLs.toArray(new URL[0]);
			sourceLoader = new URLClassLoader(urls,contextClassLoader);
		}
		this.outputLocations = getOutpuLocations();
		try {
			outputLocations.add(jProject.getOutputLocation());
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
		}
		
	}

	// Return a list of all classloaders on the stack that are neither the
	// ContextFinder classloader nor the boot classloader. The last classloader
	// in the list is either a bundle classloader or the framework's classloader
	// We assume that the bootclassloader never uses the context classloader to
	// find classes in itself.
	ArrayList basicFindClassLoaders() {
		Class[] stack = contextFinder.getClassContext();
		ArrayList result = new ArrayList(1);
		for (int i = 1; i < stack.length; i++) {
			ClassLoader tmp = stack[i].getClassLoader();
			if (stack[i] != ArquillianClassLoader.class && tmp != null
					&& tmp != this) {
				if (checkClassLoader(tmp))
					result.add(tmp);
				// stop at the framework classloader or the first bundle
				// classloader
				if (tmp == finderClassLoader
						|| tmp instanceof BundleClassLoader)
					break;
			}
		}
		return result;
	}

	// ensures that a classloader does not have the ContextFinder as part of the
	// parent hierachy. A classloader which has the ContextFinder as a parent
	// must
	// not be used as a delegate, otherwise we endup in endless recursion.
	private boolean checkClassLoader(ClassLoader classloader) {
		if (classloader == null || classloader == getParent())
			return false;
		for (ClassLoader parent = classloader.getParent(); parent != null; parent = parent
				.getParent())
			if (parent == this)
				return false;
		return true;
	}

	private ArrayList findClassLoaders() {
		if (System.getSecurityManager() == null)
			return basicFindClassLoaders();
		return (ArrayList) AccessController.doPrivileged(this);
	}

	public Object run() {
		return basicFindClassLoaders();
	}

	@Override
	protected Class findClass(String className) throws ClassNotFoundException {
		try {
			IType type = jProject.findType(className);
			int offset = -1;
			if (type == null && (offset = className.indexOf('$')) != -1) {
				// Internal classes from source files must be referenced by . instead of $
				String cls = className.substring(0, offset) + className.substring(offset).replace('$', '.');
				type = jProject.findType(cls);
			}
			if (type != null) {
				IPath path = null;
				IResource resource = type.getResource();

				if (resource != null)
					path = resource.getLocation();
				if (path == null)
					path = type.getPath();

				// needs to be compiled before we can load it
				if ("class".equalsIgnoreCase(path.getFileExtension())) { //$NON-NLS-1$
					IFile file = null;

					if (resource != null && resource.getType() == IResource.FILE)
						file = (IFile) resource;
					else
						file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);

					if (file != null && file.isAccessible()) {
						byte[] bytes = loadBytes(file);
						definePackage(className);
						return defineClass(className, bytes, 0, bytes.length);
					}
				}
				// Look up the class file based on the output location of the java project
				else if ("java".equalsIgnoreCase(path.getFileExtension()) && resource != null) { //$NON-NLS-1$
					if (resource.getProject() != null) {
						String outputClass = StringUtils.replace(type.getFullyQualifiedName(), ".", "/").concat(".class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						for (IPath outputLocation : outputLocations) {
							IPath classPath = outputLocation
									.append(outputClass);
							IFile file = ResourcesPlugin.getWorkspace()
									.getRoot().getFile(classPath);
							if (file != null && file.isAccessible()) {
								byte[] bytes = loadBytes(file);
								definePackage(className);
								return defineClass(className, bytes, 0,
										bytes.length);
							}
						}
					}
				}
				else if ("jar".equalsIgnoreCase(path.getFileExtension())) { //$NON-NLS-1$
					String expectedFileName = StringUtils.replace(className, ".", "/").concat(".class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					byte[] bytes = getCachedInputStream(path.toOSString(), expectedFileName);
					definePackage(className);
					return defineClass(className, bytes, 0, bytes.length);
				}
			}
		}
		catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
		}
		return super.findClass(className);
	}

	private void definePackage(String className) {
		int i = className.lastIndexOf('.');
		if (i != -1) {
		    String pkgName = className.substring(0, i);
		    Package pkg = getPackage(pkgName);
			if (pkg == null) {
				definePackage(pkgName, null, null, null, null, null, null, null);
			}
		}
	}

	private byte[] getCachedInputStream(String jarFilename, String entryName) {
		ByteArrayOutputStream buffer = null;

		File testFile = new File(jarFilename);
		if (!testFile.exists())
			return null;

		ZipFile jarfile = null;
		try {
			jarfile = new ZipFile(jarFilename);
			
			if (jarfile != null) {
				ZipEntry zentry = jarfile.getEntry(entryName);
				if (zentry != null) {
					InputStream entryInputStream = null;
					try {
						entryInputStream = jarfile.getInputStream(zentry);
					}
					catch (IOException e) {
						ArquillianCoreActivator.log(e);
					}

					if (entryInputStream != null) {
						int c;
						if (zentry.getSize() > 0) {
							buffer = new ByteArrayOutputStream((int) zentry.getSize());
						}
						else {
							buffer = new ByteArrayOutputStream();
						}
						// array dim restriction?
						byte bytes[] = new byte[2048];
						try {
							while ((c = entryInputStream.read(bytes)) >= 0) {
								buffer.write(bytes, 0, c);
							}
						}
						catch (IOException ioe) {
							// no cleanup can be done
						}
						finally {
							try {
								entryInputStream.close();
							}
							catch (IOException e) {
							}
						}
					}
				}
			}
		}
		catch (IOException e) {
			ArquillianCoreActivator.log(e);
		}
		finally {
			closeJarFile(jarfile);
		}

		if (buffer != null) {
			return buffer.toByteArray();
		}
		return new byte[0];
	}
	
	public void closeJarFile(ZipFile file) {
		if (file == null)
			return;
		try {
			file.close();
		}
		catch (IOException e) {
			ArquillianCoreActivator.log(e);
		}
	}
	private byte[] loadBytes(IFile file) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		try {
			in = file.getContents();
			byte[] buffer = new byte[4096];
			int read = 0;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
		catch (CoreException e) {
			ArquillianCoreActivator.log(e);
		}
		catch (IOException e) {
			ArquillianCoreActivator.log(e);
		}
		finally {
			try {
				if (in != null)
					in.close();
			}
			catch (IOException e) {
			}
		}
		return out.toByteArray();
	}
	private Set<IPath> getOutpuLocations() {
		Set<IPath> paths = new HashSet<IPath>();
		IClasspathEntry[] entries;
		try {
			entries = jProject.getRawClasspath();
		} catch (JavaModelException e) {
			// ignore
			return paths;
		}
		for (IClasspathEntry entry:entries) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath outputPath = entry.getOutputLocation();
				if (outputPath != null && !outputPath.isEmpty()) {
					paths.add(outputPath);
				}
			}
		}
		return paths;
	}	

	/*
	 * partially copied from org.eclipse.jdt.internal.compiler.util.Util.getInputStreamAsByteArray
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream)
			throws IOException {
		byte[] contents;
		contents = new byte[0];
		int contentsLength = 0;
		int amountRead = -1;
		do {
			int amountRequested = Math.max(stream.available(),
					DEFAULT_READING_SIZE);
			if (contentsLength + amountRequested > contents.length) {
				System.arraycopy(contents, 0,
						contents = new byte[contentsLength + amountRequested],
						0, contentsLength);
			}
			amountRead = stream.read(contents, contentsLength, amountRequested);
			if (amountRead > 0) {
				contentsLength += amountRead;
			}
		} while (amountRead != -1);
		if (contentsLength < contents.length) {
			System.arraycopy(contents, 0, contents = new byte[contentsLength],
					0, contentsLength);
		}
		return contents;
	}
	
	private byte[] getBytes(String clazz) throws JavaModelException {
		IType type = jProject.findType(clazz);
		int offset = -1;
		if (type == null && (offset = clazz.indexOf('$')) != -1) {
			// Internal classes from source files must be referenced by . instead of $
			String cls = clazz.substring(0, offset) + clazz.substring(offset).replace('$', '.');
			type = jProject.findType(cls);
		}
		if (type == null) {
			for (IJavaProject project:dependentProjects) {
				type = project.findType(clazz);
				if (type != null)
					break;
			}
		}
		IClassFile classFile = type.getClassFile();
		if (classFile != null) {
			byte[] bytes = classFile.getBytes();
			return bytes;
		}
		return null;
	}

	protected URL findResource(String name) {
		ArrayList toConsult = findClassLoaders();
		for (Iterator loaders = toConsult.iterator(); loaders.hasNext();) {
			URL result = ((ClassLoader) loaders.next()).getResource(name);
			if (result != null)
				return result;
			// go to the next class loader
		}
		URL result = sourceLoader.getResource(name);
		if (result != null)
			return result;
		
		result = findURL(name);
		if (result != null)
			return result;
		return super.findResource(name);
	}

	protected Enumeration findResources(String name) throws IOException {
		ArrayList toConsult = findClassLoaders();
		Enumeration result;
		for (Iterator loaders = toConsult.iterator(); loaders.hasNext();) {
			result = ((ClassLoader) loaders.next())
					.getResources(name);
			if (result != null && result.hasMoreElements())
				return result;
			// go to the next class loader
		}
		result = sourceLoader.getResources(name);
		if (result != null)
			return result;
		// FIXME find all resources
		URL resultURL = findURL(name);
		if (resultURL != null) {
			return new ArrayEnumeration(resultURL);
		}
		return super.findResources(name);
	}

	private URL findURL(String name) {
		if (found.containsKey(name))
			return (URL) found.get(name);
		if (notFound.contains(name))
			return null;
		for (Iterator iter = jars.iterator(); iter.hasNext();) {
			IPath path = (IPath) iter.next();
			File file = getRawLocationFile(path);
			if (file.exists() && file.isDirectory()) {
				File resource = new File(file, name);
				if (resource.exists()) {
					try {
						URL url = file.toURI().toURL();
						found.put(name, url);
						return url;
					} catch (MalformedURLException e) {
						// ignore
					}

				}
			} else if (file.exists()) {
				JarFile jarFile = null;
				try {
					jarFile = new JarFile(file);
					Enumeration entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry jarEntry = (JarEntry) entries.nextElement();
						if (jarEntry.getName().equals(name)) {
							URL url = creatFileResource(jarEntry, jarFile, file);
							found.put(name, url);
							return url;
						}
					}
					jarFile.close();
				} catch (Exception e) {
					// ignore
				} finally {
					if (jarFile != null) {
						try {
							jarFile.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
		}
		notFound.add(name);
		return null;
	}

	private URL creatFileResource(JarEntry jarEntry, JarFile jarFile, File file)
			throws ClassNotFoundException {
		//File rootFile = ArquillianCoreActivator.getRootFile();
		File projectDirectory = ArquillianCoreActivator.getLoaderDirectory(jProject.getProject());
	
		projectDirectory = makeNewDirectory(projectDirectory);
		File dest = null;
		try {
			dest = getDestination(projectDirectory, file);
		} catch (IOException e) {
			return null;
		}
		createFileResource(jarEntry, dest, jarFile, file);
		URL url = null;
		try {
			url = new File(dest, jarEntry.getName()).toURI().toURL();
		} catch (MalformedURLException e) {
			return null;
		}
		return url;
	}

	private void createFileResource(JarEntry jarEntry, File dest, JarFile jarFile,
			File file) throws ClassNotFoundException {
		File dir = dest;
		String separator = "/";
		String name = jarEntry.getName();
		while (name.indexOf(separator) > -1) {
			String dirString = name.substring(0, name.indexOf(separator));
			File newDir = new File(dir, dirString);
			newDir = makeNewDirectory(newDir);
			name = name.substring(name.indexOf(separator) + 1);
			dir = newDir;
		}

		InputStream input = null;
		OutputStream output = null;
		try {
			input = jarFile.getInputStream(jarEntry);
			File outputFile = new File(dir, name);
			if (!outputFile.exists()
					|| outputFile.lastModified() < file.lastModified()) {
				outputFile.delete();
				if (!outputFile.createNewFile())
					throw new ClassNotFoundException(
							"Could not create the " + file.getName() + " file"); 
				output = new FileOutputStream(outputFile);
				int c;
				while (((c = input.read()) != -1))
					output.write(c);
			}
		} catch (Exception e) {
			ArquillianCoreActivator.log(e);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
				}
			if (output != null)
				try {
					output.close();
				} catch (IOException e) {
				}
		}
	}

	private File getDestination(File baseUnpackDir, File file)
			throws IOException {
		File dest = new File(baseUnpackDir, getOutputFile(file.getName()));
		if (dest.exists() || file.lastModified() < dest.lastModified())
			return dest;
		ArquillianUtility.deleteFile(dest);
		dest.mkdirs();
		return dest;
	}

	private String getOutputFile(String name) {
		if (name == null)
			return null;
		if (name.endsWith(".jar")) 
			name = name.substring(0, name.length() - 4);
		String out = name.replace('.', '_');

		return out;
	}

	private File makeNewDirectory(File dir) {

		if (!dir.isDirectory()) {
			dir.delete();
		}
		if (!dir.exists())
			dir.mkdirs();
		return dir;
	}

	private File getRawLocationFile(IPath simplePath) {
		IResource resource = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(simplePath);
		File file = null;
		if (resource != null) {
			file = ResourcesPlugin.getWorkspace().getRoot().findMember(
					simplePath).getLocation().toFile();
		} else {
			file = simplePath.toFile();
		}
		return file;
	}

	private List<IPath> getJarPaths(IJavaProject jProject) {
		List<IPath> classPaths = new ArrayList<IPath>();
		List<IPath> sourcePaths = new ArrayList<IPath>();
		if (jProject == null)
			return classPaths;
		try {
			IClasspathEntry[] entries = jProject.getRawClasspath();

			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getOutputLocation();
					if (path == null) {
						path = jProject.getOutputLocation();
					}
					File file = getRawLocationFile(path);
					if (file.exists()) {
						URLs.add(file.toURI().toURL());
					}
					sourcePaths.add(entry.getPath());
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IClasspathEntry resLib = JavaCore
							.getResolvedClasspathEntry(entry);
					addClassPath(classPaths, resLib);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					IClasspathEntry projectEntry = JavaCore
							.getResolvedClasspathEntry(entry);
					IPath path = projectEntry.getPath();
					String name = path.segment(0);
					IProject project = ResourcesPlugin.getWorkspace().getRoot()
							.getProject(name);
					if (project.exists()) {
						IJavaProject javaProject = JavaCore.create(project);
						if (javaProject.exists()) {
							dependentProjects.add(javaProject);
							classPaths.addAll(getJarPaths(javaProject));
						}
					}
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
					IClasspathEntry resLib = JavaCore
							.getResolvedClasspathEntry(entry);
					addClassPath(classPaths, resLib);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					if (!entry.getPath().toString().endsWith(
							"JRE_CONTAINER")) {
						IClasspathEntry[] resLibs = JavaCore
								.getClasspathContainer(entry.getPath(),
										jProject).getClasspathEntries();
						for (int j = 0; j < resLibs.length; j++) {
							addClassPath(classPaths, resLibs[j]);
						}
					}
				}
			}

		} catch (Exception e) {
			ArquillianCoreActivator.log(e);
		} 
		for (IPath path:sourcePaths) {
			File file = getRawLocationFile(path);
			if (file.exists()) {
				try {
					URLs.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
					// ignore
				}
			}
		}
		return classPaths;
	}

	private void addClassPath(List<IPath> classPaths, IClasspathEntry resLibs) {
		IPath path = resLibs.getPath();
		String ls = path.lastSegment();
		if (ls != null && ls.length() > 0) {
			classPaths.add(path);
		}
	}
	
	class ArrayEnumeration implements Enumeration {
		private Object[] array;
		int cur = 0;

		public ArrayEnumeration(Object object) {
			this.array = new Object[1];
			System.arraycopy(array, 0, this.array, 0, this.array.length);
		}

		public boolean hasMoreElements() {
			return cur < array.length;
		}

		public Object nextElement() {
			return array[cur++];
		}
	}

}

