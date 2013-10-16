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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

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
import org.eclipse.jdt.core.compiler.ReconcileContext;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.core.builder.AbstractImageBuilder;
import org.eclipse.jdt.internal.core.builder.BuildNotifier;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.jdt.internal.core.builder.ProblemFactory;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

public class ArquillianCompilationParticipant extends CompilationParticipant implements ICompilerRequestor {

    private static final String JAVA_LANG_OBJECT = "java.lang.Object"; //$NON-NLS-1$
	private static final String UNRESOLVED_TYPE = "Unresolved type"; //$NON-NLS-1$
	private ArquillianNameEnvironment nameEnvironment;
    private ClasspathMultiDirectory[] sourceLocations;
    private BuildNotifier notifier;
    private List problemSourceFiles;
    private Compiler compiler;

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
        //compiler = newCompiler(project);
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
        	
        	if (remove) {
        		iterator.remove();
        	}
        }
        final String typePreference = ArquillianUtility.getPreference(ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, project.getProject());
    	
        for (final SourceFile sourceFile:sourceFiles) {
        	CompilationUnit rootAst = null;
        	if (nameEnvironment.setEnvironment(sourceFile, project)) {
        		//compile(new SourceFile[] { sourceFile });
        		rootAst = getAST(sourceFile);
        		if (rootAst == null) {
        			return;
        		}
        		final CompilationUnit root = rootAst;
        		IProblem[] problems = rootAst.getProblems();
        		
        		IFile resource = sourceFile.resource;
                try {
                    //resource.deleteMarkers(ArquillianConstants.MARKER_CLASS_ID, false, IResource.DEPTH_INFINITE);
                    for (IProblem problem : problems) {
                        storeProblem(problem, resource);
                    }
                } catch (CoreException e) {
                    ArquillianCoreActivator.log(e);
                }
        		
				if (!JavaCore.IGNORE.equals(typePreference)) {
					rootAst.accept(new ASTVisitor() {
						
						@Override
						public boolean visit(FieldDeclaration fieldDeclaration) {
							ITypeBinding binding = fieldDeclaration.getType().resolveBinding();
							if (binding != null) {
								ITypeBinding superclass = binding.getSuperclass();
								binding.getDeclaredFields();
								while (superclass != null && !root.getAST().resolveWellKnownType(JAVA_LANG_OBJECT).equals(superclass)) {
									superclass.getDeclaredFields();
									superclass = superclass.getSuperclass();
								}
								superclass = binding.getSuperclass();
								StringBuilder builder = new StringBuilder();
								builder.append(binding.toString());
								while (superclass != null && !root.getAST().resolveWellKnownType(JAVA_LANG_OBJECT).equals(superclass)) {
									builder.append("\n"); //$NON-NLS-1$
									builder.append(superclass.toString());
									superclass = superclass.getSuperclass();
								}
								String source = builder.toString();
								Scanner scanner = null;
								try {
									scanner = new Scanner(source);
									while (scanner.hasNextLine()) {
										String line = scanner.nextLine();
										if (line.contains(UNRESOLVED_TYPE)) {
											int index = line.indexOf(UNRESOLVED_TYPE) + UNRESOLVED_TYPE.length();
											String str = line.substring(index);
											String[] elements = str.trim().split(" "); //$NON-NLS-1$
											if (elements.length >= 1) {
												String type = elements[0];
												String message = "The type " + type
														+ " is not included in any deployment. "
														+ "It is indirectly referenced from required .class files";
												final Integer severity = ArquillianUtility.getSeverity(typePreference);
												IMarker marker = storeProblem(sourceFile, message, severity, ArquillianConstants.MARKER_CLASS_ID);
												if (marker != null) {
													int start = fieldDeclaration.getStartPosition();
													int end = start + fieldDeclaration.getLength();
													int lineNumber = root.getLineNumber(start);
													marker.setAttribute(IMarker.CHAR_START, start + 1);
													marker.setAttribute(IMarker.CHAR_END, end - 1);
													marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
													marker.setAttribute(ArquillianConstants.MARKER_CLASS_NAME, type);
													
												}
											}
										}
									}
								} catch (CoreException e) {
									ArquillianCoreActivator.log(e);
								} finally {
									if(scanner != null) {
										scanner.close();
									}
								}
							}
							return false;
						}

					});
				}
        	}
        	
        	String preference = ArquillianUtility.getPreference(ArquillianConstants.INVALID_ARCHIVE_NAME, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			validateArchiveName(rootAst, sourceFile, project, severity);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        	}
        }
        
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

	private CompilationUnit getAST(final SourceFile sourceFile) {
		CompilationUnit rootAst;
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
		parser.setProject(null);
		
		parser.setEnvironment(nameEnvironment.getBinaryClasspath(), new String[] {}, null, true);
		rootAst = (CompilationUnit) parser.createAST(null);
		return rootAst;
	}
    
    private void validateArchiveName(CompilationUnit rootAst, final SourceFile sourceFile, IJavaProject project, final Integer severity) throws CoreException {
		if (rootAst == null) {
			rootAst = getAST(sourceFile);
		}
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
		IMarker marker = sourceFile.resource
				.createMarker(type);
		String[] attributeNames = ArquillianConstants.ARQUILLIAN_PROBLEM_MARKER_ATTRIBUTE_NAMES;
		String[] allNames = attributeNames;

		Object[] allValues = new Object[allNames.length];
		// standard attributes
		int index = 0;
		StringBuffer sb = new StringBuffer();
		sb.append("Arquillian: ");
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

	private void compile(SourceFile[] units) {
        if (units.length == 0) return;
        SourceFile[] additionalUnits = null;
        String message = Messages.bind(Messages.build_compiling, units[0].resource.getFullPath().removeLastSegments(1).makeRelative().toString());
        this.notifier.subTask(message);

        // extend additionalFilenames with all hierarchical problem types found during this entire build
        if (!this.problemSourceFiles.isEmpty()) {
            int toAdd = this.problemSourceFiles.size();
            int length = additionalUnits == null ? 0 : additionalUnits.length;
            if (length == 0)
                additionalUnits = new SourceFile[toAdd];
            else
                System.arraycopy(additionalUnits, 0, additionalUnits = new SourceFile[length + toAdd], 0, length);
            for (int i = 0; i < toAdd; i++)
                additionalUnits[length + i] = (SourceFile) this.problemSourceFiles.get(i);
        }
        String[] initialTypeNames = new String[units.length];
        for (int i = 0, l = units.length; i < l; i++)
            initialTypeNames[i] = units[i].initialTypeName;
        this.nameEnvironment.setNames(initialTypeNames, additionalUnits);
        this.notifier.checkCancel();
        try {
            this.compiler.compile(units);
        } catch (AbortCompilation ignored) {
            // ignore the AbortCompilcation coming from BuildNotifier.checkCancelWithinCompiler()
            // the Compiler failed after the user has chose to cancel... likely due to an OutOfMemory error
        } finally {

        }
        // Check for cancel immediately after a compile, because the compiler may
        // have been cancelled but without propagating the correct exception
        this.notifier.checkCancel();
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
    public void reconcile(ReconcileContext context) {
        // TODO Auto-generated method stub
        super.reconcile(context);
    }

    @Override
	public boolean isActive(IJavaProject project) {
		if (project == null || project.getProject() == null) {
			return false;
		}
		// FIXME

		return true;
	}

    protected Compiler newCompiler(IJavaProject javaProject) {
        Map projectOptions = javaProject.getOptions(true);
        String option = (String) projectOptions.get(JavaCore.COMPILER_PB_INVALID_JAVADOC);
        if (option == null || option.equals(JavaCore.IGNORE)) {
            option = (String) projectOptions.get(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS);
            if (option == null || option.equals(JavaCore.IGNORE)) {
                option = (String) projectOptions.get(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS);
                if (option == null || option.equals(JavaCore.IGNORE)) {
                    option = (String) projectOptions.get(JavaCore.COMPILER_PB_UNUSED_IMPORT);
                    if (option == null || option.equals(JavaCore.IGNORE)) {
                        projectOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.DISABLED);
                    }
                }
            }
        }

        CompilerOptions compilerOptions = new CompilerOptions(projectOptions);
        compilerOptions.performMethodsFullRecovery = true;
        compilerOptions.performStatementsRecovery = true;
        nameEnvironment = new ArquillianNameEnvironment(javaProject);
        Compiler newCompiler = new Compiler(
            nameEnvironment,
            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
            compilerOptions,
            this,
            ProblemFactory.getProblemFactory(Locale.getDefault()));
        CompilerOptions options = newCompiler.options;
        // temporary code to allow the compiler to revert to a single thread
        String setting = System.getProperty("jdt.compiler.useSingleThread"); //$NON-NLS-1$
        newCompiler.useSingleThread = setting != null && setting.equals("true"); //$NON-NLS-1$

        // enable the compiler reference info support
        options.produceReferenceInfo = true;

        return newCompiler;
    }

    public void acceptResult(CompilationResult result) {
        CategorizedProblem[] problems = result.getErrors();
        if (problems == null)
            return;
        SourceFile sourceFile = (SourceFile) result.compilationUnit;
        IFile resource = sourceFile.resource;
        try {
            resource.deleteMarkers(ArquillianConstants.MARKER_CLASS_ID, false, IResource.DEPTH_INFINITE);
            for (CategorizedProblem problem : problems) {
                storeProblem(problem, resource);
            }
        } catch (CoreException e) {
            ArquillianCoreActivator.log(e);
        }
    }

    private void storeProblem(IProblem problem, IFile resource)
            throws CoreException {
    	if ((problem.getID() & IProblem.TypeRelated) == 0 && (problem.getID() & IProblem.ImportRelated) == 0) {
    		// ignore
    		return;
    	}
    	String typePreference = ArquillianUtility.getPreference(ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, resource.getProject());
    	String importPreference = ArquillianUtility.getPreference(ArquillianConstants.IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, resource.getProject());
    	
    	if (JavaCore.IGNORE.equals(typePreference) && JavaCore.IGNORE.equals(importPreference)) {
    		return;
    	}
    	int id = problem.getID();
    	if (id != IProblem.IsClassPathCorrect &&  id != IProblem.UndefinedType && id != IProblem.ImportNotFound) {
    		// ignore
    		return;
    	}
   	
        IMarker marker = resource.createMarker(ArquillianConstants.MARKER_CLASS_ID);
        String[] attributeNames = AbstractImageBuilder.JAVA_PROBLEM_MARKER_ATTRIBUTE_NAMES;
        int standardLength = attributeNames.length;
        String[] allNames = attributeNames;
        int managedLength = 1;
        int extraLength = 0;
        String[] extraAttributeNames;
        if (problem instanceof CategorizedProblem) {
        	extraAttributeNames = ((CategorizedProblem)problem).getExtraMarkerAttributeNames();
        	extraLength = extraAttributeNames == null ? 0 : extraAttributeNames.length;
        } else {
        	extraAttributeNames = new String[0];
        }
        if (managedLength > 0 || extraLength > 0) {
            allNames = new String[standardLength + managedLength + extraLength + 1];
            System.arraycopy(attributeNames, 0, allNames, 0, standardLength);
            if (managedLength > 0)
                allNames[standardLength] = IMarker.SOURCE_ID;
            System.arraycopy(extraAttributeNames, 0, allNames, standardLength + managedLength, extraLength);
        }
        
        allNames[allNames.length-1] = ArquillianConstants.MARKER_CLASS_NAME;

        Object[] allValues = new Object[allNames.length];
        // standard attributes
        int index = 0;
        String[] arguments = problem.getArguments();
    	String message = "Arquillian: " + problem.getMessage();
    	Integer severity = null;
    	if (arguments != null && arguments.length > 0) {
			if (id == IProblem.IsClassPathCorrect) {
				// Pb(324) The type a.b.C cannot be resolved. It is indirectly referenced from required .class files
				message = "Arquillian: The " + arguments[0] + " type is not included in any deployment. It is indirectly referenced from required .class files";
				severity = ArquillianUtility.getSeverity(typePreference);
			} else if (id == IProblem.UndefinedType) {
				// Pb(2) XXX cannot be resolved to a type
				message = "Arquillian: The " + arguments[0] + " type is not included in any deployment.";
				severity = ArquillianUtility.getSeverity(typePreference);
			} else if (id == IProblem.ImportNotFound) {
				// Pb(390) The import a.b.C cannot be resolved
				message = "Arquillian: The " + arguments[0] + " import is not included in any deployment.";
				severity = ArquillianUtility.getSeverity(importPreference);
			}
			allValues[allNames.length-1] = arguments[0];
		}        
    	if (severity == null) {
    		return;
    	}
        allValues[index++] = message; // message
        allValues[index++] = severity;
        
        allValues[index++] = new Integer(problem.getID()); // ID
        allValues[index++] = new Integer(problem.getSourceStart()); // start
        int end = problem.getSourceEnd();
        allValues[index++] = new Integer(end > 0 ? end + 1 : end); // end
        allValues[index++] = new Integer(problem.getSourceLineNumber()); // line
        allValues[index++] = Util.getProblemArgumentsForMarker(problem.getArguments()); // arguments
        if (problem instanceof CategorizedProblem) {
        	allValues[index++] = new Integer(((CategorizedProblem)problem).getCategoryID()); // category ID
        } else {
        	allValues[index++] = new Integer(0); // CAT_UNSPECIFIED
        }

        allValues[index++] = ArquillianConstants.SOURCE_ID;
        
        // optional extra attributes
        if (extraLength > 0 && problem instanceof CategorizedProblem) {
            System.arraycopy(((CategorizedProblem)problem).getExtraMarkerAttributeValues(), 0, allValues, index, extraLength);
        }

        marker.setAttributes(allNames, allValues);
    }

    private int searchColumnNumber(int[] startLineIndexes, int lineNumber, int position) {
        switch(lineNumber) {
            case 1 :
                return position + 1;
            case 2:
                return position - startLineIndexes[0];
            default:
                int line = lineNumber - 2;
                int length = startLineIndexes.length;
                if (line >= length) {
                    return position - startLineIndexes[length - 1];
                }
                return position - startLineIndexes[line];
        }
    }
}
