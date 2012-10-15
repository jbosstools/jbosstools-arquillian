/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.builder.AbortIncrementalBuildException;
import org.eclipse.jdt.internal.core.builder.BuildNotifier;
import org.eclipse.jdt.internal.core.util.Util;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

public class ArquillianNameEnvironment implements INameEnvironment, SuffixConstants {

    private boolean isIncrementalBuild;
    protected ClasspathMultiDirectory[] sourceLocations;
    private ClasspathLocation[] binaryLocations;
    protected ClasspathMultiDirectory[] baseSourceLocations;
    private ClasspathLocation[] baseBinaryLocations;
    private BuildNotifier notifier;
    private SimpleSet initialTypeNames; // assumed that each name is of the form
                                // "a/b/ClassName"
    private SimpleLookupTable additionalUnits;
    
    public ArquillianNameEnvironment(IJavaProject javaProject) {
        this.isIncrementalBuild = false;
        try {
            computeClasspathLocations(javaProject.getProject().getWorkspace().getRoot(), (JavaProject) javaProject, null);
        } catch(CoreException e) {
            this.sourceLocations = new ClasspathMultiDirectory[0];
            this.binaryLocations = new ClasspathLocation[0];
            this.baseBinaryLocations = new ClasspathLocation[0];
            this.baseSourceLocations = new ClasspathMultiDirectory[0];
        }
        setNames(null, null);
    }

    /* Some examples of resolved class path entries.
    * Remember to search class path in the order that it was defined.
    *
    * 1a. typical project with no source folders:
    *   /Test[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test
    * 1b. project with source folders:
    *   /Test/src1[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test/src1
    *   /Test/src2[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test/src2
    *  NOTE: These can be in any order & separated by prereq projects or libraries
    * 1c. project external to workspace (only detectable using getLocation()):
    *   /Test/src[CPE_SOURCE][K_SOURCE] -> d:/eclipse.zzz/src
    *  Need to search source folder & output folder
    *
    * 2. zip files:
    *   D:/j9/lib/jclMax/classes.zip[CPE_LIBRARY][K_BINARY][sourcePath:d:/j9/lib/jclMax/source/source.zip]
    *      -> D:/j9/lib/jclMax/classes.zip
    *  ALWAYS want to take the library path as is
    *
    * 3a. prereq project (regardless of whether it has a source or output folder):
    *   /Test[CPE_PROJECT][K_SOURCE] -> D:/eclipse.test/Test
    *  ALWAYS want to append the output folder & ONLY search for .class files
    */
    private void computeClasspathLocations(
        IWorkspaceRoot root,
        JavaProject javaProject,
        SimpleLookupTable binaryLocationsPerProject) throws CoreException {

        /* Update cycle marker */
        IMarker cycleMarker = javaProject.getCycleMarker();
        if (cycleMarker != null) {
            int severity = JavaCore.ERROR.equals(javaProject.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH, true))
                ? IMarker.SEVERITY_ERROR
                : IMarker.SEVERITY_WARNING;
            if (severity != cycleMarker.getAttribute(IMarker.SEVERITY, severity))
                cycleMarker.setAttribute(IMarker.SEVERITY, severity);
        }

        IClasspathEntry[] classpathEntries = javaProject.getExpandedClasspath();
        ArrayList sLocations = new ArrayList(classpathEntries.length);
        ArrayList bLocations = new ArrayList(classpathEntries.length);
        nextEntry : for (int i = 0, l = classpathEntries.length; i < l; i++) {
            ClasspathEntry entry = (ClasspathEntry) classpathEntries[i];
            IPath path = entry.getPath();
            Object target = JavaModel.getTarget(path, true);
            if (target == null) continue nextEntry;

            switch(entry.getEntryKind()) {
                case IClasspathEntry.CPE_SOURCE :
                    if (!(target instanceof IContainer)) continue nextEntry;
                    IPath outputPath = entry.getOutputLocation() != null
                        ? entry.getOutputLocation()
                        : javaProject.getOutputLocation();
                    IContainer outputFolder;
                    if (outputPath.segmentCount() == 1) {
                        outputFolder = javaProject.getProject();
                    } else {
                        outputFolder = root.getFolder(outputPath);
                        if (!outputFolder.exists())
                            createOutputFolder(outputFolder);
                    }
                    sLocations.add(
                        ClasspathLocation.forSourceFolder((IContainer) target, outputFolder, entry.fullInclusionPatternChars(), entry.fullExclusionPatternChars()));
                    continue nextEntry;

                case IClasspathEntry.CPE_PROJECT :
                    if (!(target instanceof IProject)) continue nextEntry;
                    IProject prereqProject = (IProject) target;
                    if (!JavaProject.hasJavaNature(prereqProject)) continue nextEntry; // if project doesn't have java nature or is not accessible

                    JavaProject prereqJavaProject = (JavaProject) JavaCore.create(prereqProject);
                    IClasspathEntry[] prereqClasspathEntries = prereqJavaProject.getRawClasspath();
                    ArrayList seen = new ArrayList();
                    nextPrereqEntry: for (int j = 0, m = prereqClasspathEntries.length; j < m; j++) {
                        IClasspathEntry prereqEntry = prereqClasspathEntries[j];
                        if (prereqEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                            Object prereqTarget = JavaModel.getTarget(prereqEntry.getPath(), true);
                            if (!(prereqTarget instanceof IContainer)) continue nextPrereqEntry;
                            IPath prereqOutputPath = prereqEntry.getOutputLocation() != null
                                ? prereqEntry.getOutputLocation()
                                : prereqJavaProject.getOutputLocation();
                            IContainer binaryFolder = prereqOutputPath.segmentCount() == 1
                                ? (IContainer) prereqProject
                                : (IContainer) root.getFolder(prereqOutputPath);
                            if (binaryFolder.exists() && !seen.contains(binaryFolder)) {
                                seen.add(binaryFolder);
                                ClasspathLocation bLocation = ClasspathLocation.forBinaryFolder(binaryFolder, true, entry.getAccessRuleSet());
                                bLocations.add(bLocation);
                                if (binaryLocationsPerProject != null) { // normal builder mode
                                    ClasspathLocation[] existingLocations = (ClasspathLocation[]) binaryLocationsPerProject.get(prereqProject);
                                    if (existingLocations == null) {
                                        existingLocations = new ClasspathLocation[] {bLocation};
                                    } else {
                                        int size = existingLocations.length;
                                        System.arraycopy(existingLocations, 0, existingLocations = new ClasspathLocation[size + 1], 0, size);
                                        existingLocations[size] = bLocation;
                                    }
                                    binaryLocationsPerProject.put(prereqProject, existingLocations);
                                }
                            }
                        }
                    }
                    continue nextEntry;

                case IClasspathEntry.CPE_LIBRARY :
                    if (target instanceof IResource) {
                        IResource resource = (IResource) target;
                        ClasspathLocation bLocation = null;
                        if (resource instanceof IFile) {
                            AccessRuleSet accessRuleSet =
                                (JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
                                && JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
                                    ? null
                                    : entry.getAccessRuleSet();
                            bLocation = ClasspathLocation.forLibrary((IFile) resource, accessRuleSet);
                        } else if (resource instanceof IContainer) {
                            AccessRuleSet accessRuleSet =
                                (JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
                                && JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
                                    ? null
                                    : entry.getAccessRuleSet();
                            bLocation = ClasspathLocation.forBinaryFolder((IContainer) target, false, accessRuleSet);    // is library folder not output folder
                        }
                        bLocations.add(bLocation);
                        if (binaryLocationsPerProject != null) { // normal builder mode
                            IProject p = resource.getProject(); // can be the project being built
                            ClasspathLocation[] existingLocations = (ClasspathLocation[]) binaryLocationsPerProject.get(p);
                            if (existingLocations == null) {
                                existingLocations = new ClasspathLocation[] {bLocation};
                            } else {
                                int size = existingLocations.length;
                                System.arraycopy(existingLocations, 0, existingLocations = new ClasspathLocation[size + 1], 0, size);
                                existingLocations[size] = bLocation;
                            }
                            binaryLocationsPerProject.put(p, existingLocations);
                        }
                    } else if (target instanceof File) {
                        
                        AccessRuleSet accessRuleSet =
                            (JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
                                && JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
                                    ? null
                                    : entry.getAccessRuleSet();
                        bLocations.add(ClasspathLocation.forLibrary(path.toString(), accessRuleSet));
                    }
                    continue nextEntry;
            }
        }

        // now split the classpath locations... place the output folders ahead of the other .class file folders & jars
        ArrayList outputFolders = new ArrayList(1);
        this.sourceLocations = new ClasspathMultiDirectory[sLocations.size()];
        if (!sLocations.isEmpty()) {
            sLocations.toArray(this.sourceLocations);

            // collect the output folders, skipping duplicates
            next : for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
                ClasspathMultiDirectory md = this.sourceLocations[i];
                IPath outputPath = md.binaryFolder.getFullPath();
                for (int j = 0; j < i; j++) { // compare against previously walked source folders
                    if (outputPath.equals(this.sourceLocations[j].binaryFolder.getFullPath())) {
                        md.hasIndependentOutputFolder = this.sourceLocations[j].hasIndependentOutputFolder;
                        continue next;
                    }
                }
                outputFolders.add(md);

                // also tag each source folder whose output folder is an independent folder & is not also a source folder
                for (int j = 0, m = this.sourceLocations.length; j < m; j++)
                    if (outputPath.equals(this.sourceLocations[j].sourceFolder.getFullPath()))
                        continue next;
                md.hasIndependentOutputFolder = true;
            }
        }

        // combine the output folders with the binary folders & jars... place the output folders before other .class file folders & jars
//        this.binaryLocations = new ClasspathLocation[outputFolders.size() + bLocations.size()];
//        int index = 0;
//        for (int i = 0, l = outputFolders.size(); i < l; i++)
//            this.binaryLocations[index++] = (ClasspathLocation) outputFolders.get(i);
//        for (int i = 0, l = bLocations.size(); i < l; i++)
//            this.binaryLocations[index++] = (ClasspathLocation) bLocations.get(i);

        this.binaryLocations = new ClasspathLocation[bLocations.size()];
        int index = 0;
//        for (int i = 0, l = outputFolders.size(); i < l; i++)
//            this.binaryLocations[index++] = (ClasspathLocation) outputFolders.get(i);
        for (int i = 0, l = bLocations.size(); i < l; i++)
            this.binaryLocations[index++] = (ClasspathLocation) bLocations.get(i);
        
        this.baseBinaryLocations = new ClasspathLocation[binaryLocations.length];
        System.arraycopy(binaryLocations, 0, baseBinaryLocations, 0, binaryLocations.length);
        this.baseSourceLocations = new ClasspathMultiDirectory[sourceLocations.length];
        System.arraycopy(sourceLocations, 0, baseSourceLocations, 0, sourceLocations.length);
        
    }

    private void createOutputFolder(IContainer outputFolder) throws CoreException {
        createParentFolder(outputFolder.getParent());
        ((IFolder) outputFolder).create(IResource.FORCE | IResource.DERIVED, true, null);
    }

    private void createParentFolder(IContainer parent) throws CoreException {
        if (!parent.exists()) {
            createParentFolder(parent.getParent());
            ((IFolder) parent).create(true, true, null);
        }
    }
    protected void setNames(String[] typeNames, SourceFile[] additionalFiles) {
        // convert the initial typeNames to a set
        if (typeNames == null) {
            this.initialTypeNames = null;
        } else {
            this.initialTypeNames = new SimpleSet(typeNames.length);
            for (int i = 0, l = typeNames.length; i < l; i++)
                this.initialTypeNames.add(typeNames[i]);
        }
        // map the additional source files by qualified type name
        if (additionalFiles == null) {
            this.additionalUnits = null;
        } else {
            this.additionalUnits = new SimpleLookupTable(additionalFiles.length);
            for (int i = 0, l = additionalFiles.length; i < l; i++) {
                SourceFile additionalUnit = additionalFiles[i];
                if (additionalUnit != null)
                    this.additionalUnits.put(additionalUnit.initialTypeName, additionalFiles[i]);
            }
        }

        for (int i = 0, l = this.sourceLocations.length; i < l; i++)
            this.sourceLocations[i].reset();
        for (int i = 0, l = this.binaryLocations.length; i < l; i++)
            this.binaryLocations[i].reset();
    }

    private NameEnvironmentAnswer findClass(String qualifiedTypeName, char[] typeName) {
        if (this.notifier != null)
            this.notifier.checkCancelWithinCompiler();

        if (this.initialTypeNames != null && this.initialTypeNames.includes(qualifiedTypeName)) {
            if (this.isIncrementalBuild)
                // catch the case that a type inside a source file has been
                // renamed but other class files are looking for it
                throw new AbortCompilation(true, new AbortIncrementalBuildException(
                        qualifiedTypeName));
            return null; // looking for a file which we know was provided at the
                         // beginning of the compilation
        }

        if (this.additionalUnits != null && this.sourceLocations.length > 0) {
            // if an additional source file is waiting to be compiled, answer it
            // BUT not if this is a secondary type search
            // if we answer X.java & it no longer defines Y then the binary type
            // looking for Y will think the class path is wrong
            // let the recompile loop fix up dependents when the secondary type
            // Y has been deleted from X.java
            SourceFile unit = (SourceFile) this.additionalUnits.get(qualifiedTypeName); // doesn't have file extension
            if (unit != null)
                return new NameEnvironmentAnswer(unit, null /* no access restriction*/);
        }

        String qBinaryFileName = qualifiedTypeName + SUFFIX_STRING_class;
        String binaryFileName = qBinaryFileName;
        String qPackageName = ""; //$NON-NLS-1$
        if (qualifiedTypeName.length() > typeName.length) {
            int typeNameStart = qBinaryFileName.length() - typeName.length - 6; // size of ".class"
            qPackageName = qBinaryFileName.substring(0, typeNameStart - 1);
            binaryFileName = qBinaryFileName.substring(typeNameStart);
        }
        NameEnvironmentAnswer suggestedAnswer = null;
        for (int i = 0, l = this.binaryLocations.length; i < l; i++) {
            NameEnvironmentAnswer answer = this.binaryLocations[i].findClass(binaryFileName,
                    qPackageName, qBinaryFileName);
            if (answer != null) {
                if (!answer.ignoreIfBetter()) {
                    if (answer.isBetter(suggestedAnswer))
                        return answer;
                } else if (answer.isBetter(suggestedAnswer))
                    suggestedAnswer = answer;
            }
        }
        if (suggestedAnswer != null)
            return suggestedAnswer;
        return null;
    }

    public NameEnvironmentAnswer findType(char[][] compoundName) {
        if (compoundName != null)
            return findClass(new String(CharOperation.concatWith(compoundName, '/')),
                    compoundName[compoundName.length - 1]);
        return null;
    }

    public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
        if (typeName != null)
            return findClass(new String(CharOperation.concatWith(packageName, typeName, '/')),
                    typeName);
        return null;
    }

    public boolean isPackage(char[][] compoundName, char[] packageName) {
        return isPackage(new String(CharOperation.concatWith(compoundName, packageName, '/')));
    }

    public boolean isPackage(String qualifiedPackageName) {
        for (int i = 0, l = this.binaryLocations.length; i < l; i++)
            if (this.binaryLocations[i].isPackage(qualifiedPackageName))
                return true;
        return false;
    }

    public void cleanup() {
        this.initialTypeNames = null;
        this.additionalUnits = null;
        for (int i = 0, l = this.sourceLocations.length; i < l; i++)
            this.sourceLocations[i].cleanup();
        for (int i = 0, l = this.binaryLocations.length; i < l; i++)
            this.binaryLocations[i].cleanup();
    }

	public boolean setEnvironment(SourceFile sourceFile, IJavaProject javaProject) {
		IType type = ArquillianSearchEngine.getType(sourceFile);
        boolean ret = false;
        List<File> archives = null;
        if (type != null) {
        	archives = ArquillianSearchEngine.getDeploymentArchives(type);
        	if (archives != null) {
        		ret = true;
        	}
        }
        if (ret) {
        	this.binaryLocations = new ClasspathLocation[baseBinaryLocations.length];
        	System.arraycopy(baseBinaryLocations, 0, binaryLocations, 0, baseBinaryLocations.length);
        	//this.sourceLocations = new ClasspathMultiDirectory[baseSourceLocations.length];
        	//System.arraycopy(baseSourceLocations, 0, sourceLocations, 0, baseSourceLocations.length);
        	List<File> files = new ArrayList<File>();
        	for (File archive:archives) {
        		if (archive.getName().endsWith(".jar")) {
        			files.add(archive);
        		} else if (archive.getName().endsWith("war")) {
        			File destination = new File(archive.getParentFile(), "archive");
        			if (!destination.isDirectory()) {
        				ArquillianUtility.deleteFile(destination);
        				exportFile(archive, destination);
        			}
        			
        			File webInf = new File(destination, "WEB-INF");
        			if (webInf.isDirectory()) {
        				File classes = new File(webInf, "classes");
        				if (classes.isDirectory()) {
        					IPath path = new Path(classes.getAbsolutePath());
        					JavaModelManager.getExternalManager().removeFolder(path);
        					JavaModelManager.getExternalManager().removePendingFolder(path);
        					JavaModelManager.getExternalManager().addFolder(path, true);
        					files.add(classes);
        				}
        				File lib = new File(webInf, "lib");
        				if (lib.isDirectory()) {
        					File[] libs = lib.listFiles();
        					if (libs != null) {
        						for (File libFile:libs) {
        							if (libFile.isFile()) {
        								//JavaModelManager.getExternalManager().addFolder(new Path(libFile.getAbsolutePath()), true);
        								files.add(libFile);
        							}
        						}
        					}
        				}
        			}
        		}
        		try {
        			JavaModelManager.getExternalManager().createPendingFolders(new NullProgressMonitor());
    			}
    			catch(JavaModelException jme) {
    				// Creation of external folder project failed. Log it and continue;
    				Util.log(jme, "Error while processing external folders"); //$NON-NLS-1$
    			}
        	}
        	if (files.size() > 0) {
        		List<ClasspathLocation> classpathLocations = new ArrayList<ClasspathLocation>();
        		for (File file:files) {
        			IPath path = new Path(file.getAbsolutePath());
        			Object target = JavaModel.getTarget(path, true);
					if (target instanceof IResource) {
						IResource resource = (IResource) target;
						if (resource instanceof IFile) {
							classpathLocations.add(ClasspathLocation
									.forLibrary((IFile) resource, null));
						} else if (resource instanceof IContainer) {
							classpathLocations.add(ClasspathLocation
									.forBinaryFolder((IContainer) target,
											false, null));
						}
					} else if (target instanceof File) {
						classpathLocations.add(ClasspathLocation.forLibrary(
								path.toString(), null));
					}      		
        		}
        		if (classpathLocations.size() > 0) {
        			this.binaryLocations = new ClasspathLocation[baseBinaryLocations.length + classpathLocations.size()];
                	System.arraycopy(baseBinaryLocations, 0, binaryLocations, 0, baseBinaryLocations.length);
                	int index = baseBinaryLocations.length;                			
                	for (ClasspathLocation location:classpathLocations) {
						this.binaryLocations[index++] = location;
					}
        			return true;
        		}
        	}
        }
        return false;
	}

	public static boolean exportFile(File file, File destination) {
		ZipFile zipFile = null;
		destination.mkdirs();
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				
				ZipEntry entry = (ZipEntry) entries.nextElement();
				if (entry.isDirectory()) {
					File dir = new File(destination, entry.getName());
					dir.mkdirs();
					continue;
				}
				File entryFile = new File(destination, entry.getName());
				entryFile.getParentFile().mkdirs();
				InputStream in = null;
				OutputStream out = null;
				try {
					in = zipFile.getInputStream(entry);
					out = new FileOutputStream(entryFile);
					copy(in, out);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (Exception e) {
							// ignore
						}
					}
					if (out != null) {
						try {
							out.close();
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}
		} catch (IOException e) {
			ArquillianCoreActivator.log(e);
			return false;
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return true;
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[16 * 1024];
		int len;
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
	}
	
}
