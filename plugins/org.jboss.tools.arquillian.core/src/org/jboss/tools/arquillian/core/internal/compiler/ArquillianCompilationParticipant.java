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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.builder.AbstractImageBuilder;
import org.eclipse.jdt.internal.core.builder.BuildNotifier;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.dependencies.DependencyCache;
import org.jboss.tools.arquillian.core.internal.dependencies.DependencyType;
import org.jboss.tools.arquillian.core.internal.dependencies.TypeLocation;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

public class ArquillianCompilationParticipant extends CompilationParticipant {

    private ArquillianNameEnvironment nameEnvironment;
    private ClasspathMultiDirectory[] sourceLocations;
    private BuildNotifier notifier;
    private List problemSourceFiles;

    @Override
    public void buildFinished(IJavaProject project) {
    	if (ArquillianCoreActivator.getDefault() == null) {
    		return;
    	}
    	if (!ArquillianUtility.isValidatorEnabled(project.getProject())) {
    		deleteAllMarkers(project.getProject());
    		return;
    	}
        try {
            IMarker[] markers = project.getProject().findMarkers(IMarker.PROBLEM, true,
                    IResource.DEPTH_INFINITE);
            for (IMarker marker:markers) {
                Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severity != null && severity.intValue() == IMarker.SEVERITY_ERROR && JavaBuilder.SOURCE_ID.equals(marker.getAttribute(IMarker.SOURCE_ID))) {
                    return;
                }
            }
        } catch (CoreException e1) {
            // ignore
        }
        List<SourceFile> sourceFiles = new ArrayList<SourceFile>();
        this.problemSourceFiles = new ArrayList();
        this.notifier = new BuildNotifier(new NullProgressMonitor(), project.getProject());
        this.notifier.begin();
        nameEnvironment = new ArquillianNameEnvironment(project);
        this.notifier.updateProgressDelta(0.05f);

        this.notifier.subTask(Messages.build_analyzingSources);
        sourceLocations = nameEnvironment.sourceLocations;
        try {
            addAllSourceFiles(sourceFiles, project);
        } catch (CoreException e) {
            ArquillianCoreActivator.log(e);
        }
        this.notifier.updateProgressDelta(0.10f);
        if (sourceFiles.size()  <= 0) {
        	return;
        }
        Iterator<SourceFile> iterator = sourceFiles.iterator();
        while (iterator.hasNext()) {
        	SourceFile sourceFile = iterator.next();
        	deleteAllMarkers(sourceFile.resource);

        	boolean remove = false;
        	String preference = ArquillianUtility.getPreference(ArquillianConstants.MISSING_DEPLOYMENT_METHOD, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference) && !ArquillianSearchEngine.hasDeploymentMethod(sourceFile, project)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			storeProblem(sourceFile, "Arquillian test requires at least one method annotated with @Deployment", severity, ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        		remove = true;
        	}
        	
        	preference = ArquillianUtility.getPreference(ArquillianConstants.MISSING_TEST_METHOD, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference) && !ArquillianSearchEngine.hasTestMethod(sourceFile, project)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			storeProblem(sourceFile, "Arquillian test requires at least one method annotated with @Test", severity);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        		remove = true;
        	}
        	preference = ArquillianUtility.getPreference(ArquillianConstants.INVALID_ARCHIVE_NAME, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			validateArchiveName(sourceFile, project, severity);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        		remove = true;
        	}
        	preference = ArquillianUtility.getPreference(ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference)) {
        		IFile file = sourceFile.resource;
            	IJavaElement element = JavaCore.create(file);
        		if (!(element instanceof ICompilationUnit)) {
        			return;
        		}
        		ICompilationUnit unit = (ICompilationUnit) element;
            	DependencyCache.getDependencies().remove(unit);
            	Set<DependencyType> dependencies = DependencyCache.getDependentTypes(unit);
            	List<File> archives = getArchives(sourceFile, project);
            	Map<File,JarFile> jarFiles = new HashMap<File, JarFile>();
            	Integer severity = ArquillianUtility.getSeverity(preference);
				for (DependencyType type : dependencies) {
					if (!getDeployment(type, archives, jarFiles)) {
						boolean direct = DependencyCache.getDependencies().get(unit).contains(type);
						if (direct) {
							String message = "The " + type.getName() + " type is not included in any deployment.";
							for (TypeLocation location : type.getLocations()) {
								try {
									IMarker marker = storeProblem(sourceFile, message, severity,ArquillianConstants.MARKER_CLASS_ID);
									if (marker != null) {
										marker.setAttribute(IMarker.CHAR_START, location.getCharStart());
										marker.setAttribute(IMarker.CHAR_END, location.getCharEnd());
										marker.setAttribute(IMarker.LINE_NUMBER, location.getLineNumber());
										marker.setAttribute(ArquillianConstants.MARKER_CLASS_NAME, type.getName());
									}
								} catch (CoreException e) {
									ArquillianCoreActivator.log(e);
								}
							}
						} else {
							String message = "The " + type.getName() + " type is not included in any deployment. It is indirectly referenced from required .class files";
							try {
								IMarker marker = storeProblem(sourceFile, message, severity,ArquillianConstants.MARKER_CLASS_ID);
								if (marker != null) {
									marker.setAttribute(IMarker.CHAR_START, 0);
									marker.setAttribute(IMarker.CHAR_END, 0);
									marker.setAttribute(IMarker.LINE_NUMBER, 0);
									marker.setAttribute(ArquillianConstants.MARKER_CLASS_NAME, type.getName());
								}
							} catch (CoreException e) {
								ArquillianCoreActivator.log(e);
							}
						}
					}
				}
            	for (JarFile jar:jarFiles.values()) {
            		try {
						jar.close();
					} catch (IOException e) {
						// ignore
					}
            	}
            	remove = true;
        	}
        	if (remove) {
        		iterator.remove();
        	}
        }
    }

    private boolean getDeployment(DependencyType type, List<File> archives, Map<File, JarFile> jarFiles) {
    	if (type == null || archives == null || archives.size() <= 0) {
    		return false;
    	}
    	for (File file:archives) {
    		if (file.isDirectory()) {
    			boolean ret = getDirectory(type.getName(), file);
    			if (ret) {
    				return true;
    			}
    		} else if (file.isFile()) {
    			boolean ret = getFile(type.getName(), file, jarFiles);
    			if (ret) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
	
    private boolean getDirectory(String name, File file) {
		name = name.replace(".", File.separator) + ".class";  //$NON-NLS-1$//$NON-NLS-2$
		return new File(file,name).exists();
	}
    
    private boolean getFile(String name, File file, Map<File, JarFile> jarFiles) {
		JarFile jarFile = jarFiles.get(file);
		if (jarFile == null) {
			try {
				jarFile = new JarFile(file);
				jarFiles.put(file, jarFile);
			} catch (IOException e) {
				ArquillianCoreActivator.log(e);
				return false;
			}
		}
		name = name.replace(".", "/") + ".class";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		return jarFile.getEntry(name) != null;
	}

	private void deleteAllMarkers(IResource resource) {
		try {
			resource.deleteMarkers(ArquillianConstants.MARKER_CLASS_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_RESOURCE_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			ArquillianCoreActivator.log(e);
		}
	}

	private CompilationUnit getAST(final SourceFile sourceFile, IJavaProject project) {
		IFile file = sourceFile.resource;
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return null;
		}
		ICompilationUnit cu = (ICompilationUnit) element;
		ASTParser parser= ASTParser.newParser(AST.JLS4);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(project);
		if (project == null) {
			parser.setEnvironment(nameEnvironment.getBinaryClasspath(), new String[] {}, null, true);
		}
		return (CompilationUnit) parser.createAST(null);
	}
    
    private void validateArchiveName(final SourceFile sourceFile, IJavaProject project, final Integer severity) throws CoreException {
		CompilationUnit rootAst = getAST(sourceFile, project);
		final List<MethodDeclaration> deploymentMethods = new ArrayList<MethodDeclaration>();
		final CompilationUnit root = rootAst;
		rootAst.accept(new ASTVisitor() {

			@Override
			public boolean visit(MethodDeclaration node) {
				IMethodBinding binding = node.resolveBinding();
				if (ArquillianSearchEngine.isDeploymentMethod(binding)) {
					deploymentMethods.add(node);
				}
				return false;
			}
		
		});
		for (MethodDeclaration methodDeclaration:deploymentMethods) {
			methodDeclaration.accept(new ASTVisitor() {

				@Override
				public boolean visit(MethodInvocation node) {
					boolean isCreateMethod = false;
					StringLiteral archiveName = null;
					if (node.getName() != null && "create".equals(node.getName().getIdentifier()) ) { //$NON-NLS-1$
						List arguments = node.arguments();
						if (arguments.size() == 2) {
							Object o = arguments.get(1);
							if (o instanceof StringLiteral) {
								archiveName = (StringLiteral) o;
							}
						}
						Expression expression = node.getExpression();
						if (expression instanceof SimpleName) {
							if ("ShrinkWrap".equals(((SimpleName)expression).getIdentifier()) ) { //$NON-NLS-1$
								isCreateMethod = true;
							}
						}
						if (expression instanceof QualifiedName) {
							if ("org.jboss.shrinkwrap.api.ShrinkWrap".equals(((QualifiedName)expression).getName().getIdentifier()) ) { //$NON-NLS-1$
								isCreateMethod = true;
							}
						}
					}
					if (isCreateMethod && archiveName != null) {
						IMethodBinding binding = node.resolveMethodBinding();
						ITypeBinding[] types = binding.getParameterTypes();
						if (types.length == 2) {
							ITypeBinding archiveType = types[0];
							if (archiveType.isClass()) {
								String extension = null;
								ITypeBinding[] typeArguments = archiveType.getTypeArguments();
								if (typeArguments.length == 1) {
									ITypeBinding typeArgument = typeArguments[0];
									if (ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_WEB_ARCHIVE.equals(typeArgument.getBinaryName())) {
										extension = ".war"; //$NON-NLS-1$
									}
									if (ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_JAVA_ARCHIVE.equals(typeArgument.getBinaryName())) {
										extension = ".jar"; //$NON-NLS-1$
									}
									if (ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_ENTERPRISE_ARCHIVE.equals(typeArgument.getBinaryName())) {
										extension = ".ear"; //$NON-NLS-1$
									}
									if (ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_RESOURCEADAPTER_ARCHIVE.equals(typeArgument.getBinaryName())) {
										extension = ".rar"; //$NON-NLS-1$
									}
								}
								String value = archiveName.getLiteralValue();
								if (extension != null && value != null && (!value.endsWith(extension) || value.trim().length() <= 4) ) {
									try {
										IMarker marker = storeProblem(sourceFile, "Archive name is invalid. A name with the " + extension + " extension is expected.", severity, ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID);
										if (marker != null) {
											int start = archiveName
													.getStartPosition();
											int end = start + archiveName.getLength();
											int lineNumber = root.getLineNumber(start);
											marker.setAttribute(IMarker.CHAR_START, start+1);
											marker.setAttribute(IMarker.CHAR_END, end-1);
											marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
											marker.setAttribute(ArquillianConstants.OLD_ARCHIVE_NAME, value);
											marker.setAttribute(ArquillianConstants.ARCHIVE_EXTENSION, extension);
										}
									} catch (CoreException e) {
										ArquillianCoreActivator.log(e);
									}
								}
							}
							
						}
						
					}
					return true;
				}
				
			});
		}
		
	}
    
    private void storeProblem(SourceFile sourceFile, String message, Integer severity)
			throws CoreException {
    	storeProblem(sourceFile, message, severity, ArquillianConstants.MARKER_CLASS_ID);
    }
    
    private IMarker storeProblem(SourceFile sourceFile, String message, Integer severity, String type)
			throws CoreException {
		if (severity == null) {
			return null;
		}
		IMarker marker = sourceFile.resource.createMarker(type);
		String[] attributeNames = ArquillianConstants.ARQUILLIAN_PROBLEM_MARKER_ATTRIBUTE_NAMES;
		String[] allNames = attributeNames;

		Object[] allValues = new Object[allNames.length];
		// standard attributes
		int index = 0;
		StringBuffer sb = new StringBuffer();
		sb.append("Arquillian: "); //$NON-NLS-1$
		sb.append(message);

		allValues[index++] = sb.toString(); // message
		allValues[index++] = severity;
		
		ISourceRange range = null;
		IMember javaElement = ArquillianSearchEngine.getType(sourceFile);
		if (javaElement != null) {
			try {
				range = javaElement.getNameRange();
			} catch (JavaModelException e) {
				if (e.getJavaModelStatus().getCode() != IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST) {
					throw e;
				}
				if (!CharOperation.equals(javaElement.getElementName()
						.toCharArray(), TypeConstants.PACKAGE_INFO_NAME)) {
					throw e;
				}

			}
		}
		int start = range == null ? 0 : range.getOffset();
		int end = range == null ? 1 : start + range.getLength();

		allValues[index++] = new Integer(
				ArquillianConstants.ARQUILLIAN_PROBLEM_ID); // ID
		allValues[index++] = new Integer(start); // start

		allValues[index++] = new Integer(end > 0 ? end + 1 : end); // end

		allValues[index++] = new Integer(CategorizedProblem.CAT_TYPE); // category
																		// ID
		allValues[index++] = ArquillianConstants.SOURCE_ID;
		if (javaElement != null) {
			allValues[index++] = ((IType) javaElement).getFullyQualifiedName();
		}

		marker.setAttributes(allNames, allValues);
		return marker;
	}

    protected void addAllSourceFiles(final List<SourceFile> sourceFiles, final IJavaProject project) throws CoreException {
        for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
            final ClasspathMultiDirectory sourceLocation = this.sourceLocations[i];
            final char[][] exclusionPatterns = sourceLocation.exclusionPatterns;
            final char[][] inclusionPatterns = sourceLocation.inclusionPatterns;
            final boolean isAlsoProject = sourceLocation.sourceFolder.equals(project.getProject());
            final int segmentCount = sourceLocation.sourceFolder.getFullPath().segmentCount();
            final IContainer outputFolder = sourceLocation.binaryFolder;
            final boolean isOutputFolder = sourceLocation.sourceFolder.equals(outputFolder);
            sourceLocation.sourceFolder.accept(
                new IResourceProxyVisitor() {
                    public boolean visit(IResourceProxy proxy) throws CoreException {
                        switch(proxy.getType()) {
                            case IResource.FILE :
                                if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(proxy.getName())) {
                                    IResource resource = proxy.requestResource();
                                    if (exclusionPatterns != null || inclusionPatterns != null)
                                        if (Util.isExcluded(resource.getFullPath(), inclusionPatterns, exclusionPatterns, false))
                                            return false;
                                    if (!ArquillianSearchEngine.isArquillianJUnitTest(proxy, project)) {
                                    	return false;
                                    }
                                    sourceFiles.add(new SourceFile((IFile) resource, sourceLocation));
                                }
                                return false;
                            case IResource.FOLDER :
                                IPath folderPath = null;
                                if (isAlsoProject)
                                    if (isExcludedFromProject(folderPath = proxy.requestFullPath(),project))
                                        return false;
                                if (exclusionPatterns != null) {
                                    if (folderPath == null)
                                        folderPath = proxy.requestFullPath();
                                    if (Util.isExcluded(folderPath, inclusionPatterns, exclusionPatterns, true)) {
                                        // must walk children if inclusionPatterns != null, can skip them if == null
                                        // but folder is excluded so do not create it in the output folder
                                        return inclusionPatterns != null;
                                    }
                                }
                                if (!isOutputFolder) {
                                    if (folderPath == null)
                                        folderPath = proxy.requestFullPath();
                                    String packageName = folderPath.lastSegment();
                                    if (packageName.length() > 0) {
                                        String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
                                        String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
                                        if (JavaConventions.validatePackageName(packageName, sourceLevel, complianceLevel).getSeverity() != IStatus.ERROR)
                                            createFolder(folderPath.removeFirstSegments(segmentCount), outputFolder);
                                    }
                                }
                        }
                        return true;
                    }
                },
                IResource.NONE
            );
            this.notifier.checkCancel();
        }
    }

    protected IContainer createFolder(IPath packagePath, IContainer outputFolder) throws CoreException {
        if (packagePath.isEmpty()) return outputFolder;
        IFolder folder = outputFolder.getFolder(packagePath);
        if (!folder.exists()) {
            createFolder(packagePath.removeLastSegments(1), outputFolder);
            folder.create(IResource.FORCE | IResource.DERIVED, true, null);
        }
        return folder;
    }

    protected boolean isExcludedFromProject(IPath childPath, IJavaProject project) throws JavaModelException {
        // answer whether the folder should be ignored when walking the project as a source folder
        if (childPath.segmentCount() > 2) return false; // is a subfolder of a package

        for (int j = 0, k = this.sourceLocations.length; j < k; j++) {
            if (childPath.equals(this.sourceLocations[j].binaryFolder.getFullPath())) return true;
            if (childPath.equals(this.sourceLocations[j].sourceFolder.getFullPath())) return true;
        }
        // skip default output folder which may not be used by any source folder
        return childPath.equals(project.getOutputLocation());
    }

    @Override
	public boolean isActive(IJavaProject project) {
		if (project == null || project.getProject() == null) {
			return false;
		}
		return true;
	}
 
    public List<File> getArchives(SourceFile sourceFile, IJavaProject javaProject) {
		IType type = ArquillianSearchEngine.getType(sourceFile);
        List<File> files = new ArrayList<File>();
        List<File> archives = null;
		if (type != null) {
        	archives = ArquillianSearchEngine.getDeploymentArchives(type, true);
        	if (archives == null || archives.size() <= 0) {
        		return files;
        	}
        }
        
		for (File archive : archives) {
			if (archive.getName().endsWith(".jar")) { //$NON-NLS-1$
				files.add(archive);
			} else if (archive.getName().endsWith("war")) { //$NON-NLS-1$
				File destination = new File(archive.getParentFile(), "archive"); //$NON-NLS-1$
				if (!destination.isDirectory()) {
					ArquillianUtility.deleteFile(destination);
					ArquillianUtility.exportFile(archive, destination);
				}

				File webInf = new File(destination, "WEB-INF"); //$NON-NLS-1$
				if (webInf.isDirectory()) {
					File classes = new File(webInf, "classes"); //$NON-NLS-1$
					if (classes.isDirectory()) {
						files.add(classes);
					}
					File lib = new File(webInf, "lib"); //$NON-NLS-1$
					if (lib.isDirectory()) {
						File[] libs = lib.listFiles();
						if (libs != null) {
							for (File libFile : libs) {
								if (libFile.isFile()) {
									files.add(libFile);
								}
							}
						}
					}
				}
			}
		}
        return files;
	}
}
