
/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.classpath.xpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

/**
 * 
 * Based on org.eclipse.jst.jsp.core.internal.taglib.BuildPathClassLoader
 * 
 * @author snjeza
 *
 */
public class BuildPathClassLoader extends ClassLoader {
	private IJavaProject javaProject;

	public BuildPathClassLoader(ClassLoader parent, IJavaProject project) {
		super(parent);
		javaProject = project;
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

	protected Class findClass(String className) throws ClassNotFoundException {
		try {
			IType type = javaProject.findType(className);
			int offset = -1;
			if (type == null && (offset = className.indexOf('$')) != -1) {
				// Internal classes from source files must be referenced by . instead of $
				String cls = className.substring(0, offset) + className.substring(offset).replace('$', '.');
				type = javaProject.findType(cls);
			}
			if (type != null) {
				IPath path = null;
				IResource resource = type.getResource();

				if (resource != null)
					path = resource.getLocation();
				if (path == null)
					path = type.getPath();

				// needs to be compiled before we can load it
				if ("class".equalsIgnoreCase(path.getFileExtension())) {
					IFile file = null;

					if (resource != null && resource.getType() == IResource.FILE)
						file = (IFile) resource;
					else
						file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);

					if (file != null && file.isAccessible()) {
						byte[] bytes = loadBytes(file);
						return defineClass(className, bytes, 0, bytes.length);
					}
				}
				// Look up the class file based on the output location of the java project
				else if ("java".equalsIgnoreCase(path.getFileExtension()) && resource != null) { //$NON-NLS-1$
					if (resource.getProject() != null) {
						IJavaProject jProject = JavaCore.create(resource.getProject());
						String outputClass = StringUtils.replace(type.getFullyQualifiedName(), ".", "/").concat(".class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						IPath classPath = jProject.getOutputLocation().append(outputClass);
						IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(classPath);
						if (file != null && file.isAccessible()) {
							byte[] bytes = loadBytes(file);
							return defineClass(className, bytes, 0, bytes.length);
						}
					}
				}
				else if ("jar".equalsIgnoreCase(path.getFileExtension())) {
					String expectedFileName = StringUtils.replace(className, ".", "/").concat(".class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					byte[] bytes = getCachedInputStream(path.toOSString(), expectedFileName);
					return defineClass(className, bytes, 0, bytes.length);
				}
			}
		}
		catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
		}
		return super.findClass(className);
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

	/**
	 * @param file
	 * @return
	 */
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

}