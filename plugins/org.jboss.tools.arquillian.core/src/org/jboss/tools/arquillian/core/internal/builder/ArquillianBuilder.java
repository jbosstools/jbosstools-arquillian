/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
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
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.core.ExternalFoldersManager;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.archives.Archive;
import org.jboss.tools.arquillian.core.internal.archives.ArchiveContainer;
import org.jboss.tools.arquillian.core.internal.dependencies.DependencyCache;
import org.jboss.tools.arquillian.core.internal.dependencies.DependencyType;
import org.jboss.tools.arquillian.core.internal.dependencies.TypeLocation;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

/**
 * @author snjeza
 *
 */
public class ArquillianBuilder extends IncrementalProjectBuilder {

	private IProject currentProject;
	static final IProject[] NO_PROJECTS = new IProject[0];

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		this.currentProject = getProject();
		if (currentProject == null || !currentProject.isAccessible()) {
			return NO_PROJECTS;
		}
		if (!currentProject.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)
				|| hasErrors(currentProject)
				|| !ArquillianUtility.isValidatorEnabled(currentProject)) {
			cleanupMarkers(currentProject);
			return NO_PROJECTS;
		}

		try {
			if (kind == FULL_BUILD || DependencyCache.getDependencies().size() == 0) {
				buildAll(monitor);
			} else {
				IResourceDelta delta = getDelta(currentProject);
				if (delta == null) {
					buildAll(monitor);
				} else {
					buildDelta(delta, monitor);
				}
			}
		} catch (CoreException e) {
			ArquillianCoreActivator.log(e);
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		IProject[] requiredProjects = getRequiredProjects(true);
		return requiredProjects;
	}

	private void buildDelta(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		final List<ICompilationUnit> units = new ArrayList<ICompilationUnit>();
		delta.accept(new IResourceDeltaVisitor() {
			
			@Override
			public boolean visit(IResourceDelta delta) throws CoreException {
				IResource res = delta.getResource();
				switch (res.getType()) {
					case IResource.ROOT :
						return true;
					case IResource.PROJECT :
						return true;
					case IResource.FOLDER :
						return true;
					case IResource.FILE :
						IJavaElement element = JavaCore.create(res);
						ICompilationUnit cu = null;
						if (element instanceof IClassFile) {
							cu = getCompilationUnit((IClassFile)element);
						}
						if (element instanceof ICompilationUnit) {
							cu = (ICompilationUnit) element;
						}
						if (cu != null) {
							cleanupMarkers(cu.getUnderlyingResource());
							if (units.contains(cu)) {
								DependencyCache.getDependencies().remove(cu);
								return false;
							}
							if (ArquillianSearchEngine.isArquillianJUnitTest(element, false, false)) {
								units.add(cu);
							} else {
								Set<ICompilationUnit> cus = DependencyCache.getDependencies().keySet();
								IType primaryType = cu == null ? null : cu.findPrimaryType();
								String fqn = primaryType == null ? null : primaryType.getFullyQualifiedName();
								Iterator<ICompilationUnit> iterator = cus.iterator();
								Set<ICompilationUnit> toRemove = new HashSet<ICompilationUnit>();
								toRemove.add(cu);
								while (iterator.hasNext()) {
									ICompilationUnit unit = iterator.next();
									Set<DependencyType> types = DependencyCache.getDependentTypes(unit);
									for (DependencyType type : types) {
										if (type.getName() != null && type.getName().equals(fqn)) {
											if (ArquillianSearchEngine.isArquillianJUnitTest(unit, false, false)) {
												units.add(unit);
											}
											toRemove.add(unit);
										}
									}
								}
								iterator = toRemove.iterator();
								while(iterator.hasNext()) {
									DependencyCache.getDependencies().remove(iterator.next());
								}
							}
						}
						return false;
					default :
						return false; 
				}
			}

			public ICompilationUnit getCompilationUnit(IClassFile classFile) {
				IClassFileReader classFileReader = ToolFactory.createDefaultClassFileReader(classFile,
								IClassFileReader.CLASSFILE_ATTRIBUTES);
				if (classFileReader != null) {
					char[] className = classFileReader.getClassName();
					if (className != null) {
						String fqn = new String(classFileReader.getClassName())
								.replace("/", "."); //$NON-NLS-1$ //$NON-NLS-2$
						IJavaProject javaProject = classFile.getJavaProject();
						IType sourceType = null;
						try {
							sourceType = javaProject.findType(fqn);
							if (sourceType != null) {
								return sourceType.getCompilationUnit();
							}
						} catch (JavaModelException e) {
							// ignore
						}
					}
				}
				return null;
			}
		});
		
		if (units.size() > 0) {
			ArquillianCoreActivator.getDefault().removeProjectLoader(currentProject);
			build(units, monitor);
		}
		
	}

	/**
	 * Check whether the build has been canceled.
	 */
	public void checkCancel(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();
	}
	
	private void buildAll(IProgressMonitor monitor) throws CoreException {
		checkCancel(monitor);
		cleanProject();
		List<ICompilationUnit> units = getCompilationUnits();
		build(units, monitor);
	}

	public void cleanProject() {
		DependencyCache.removeDependencies(currentProject);
		ArchiveContainer.remove(currentProject);
		ArquillianCoreActivator.getDefault().removeProjectLoader(currentProject);
	}

	private void build(List<ICompilationUnit> units, IProgressMonitor monitor)
			throws JavaModelException {
		IProject project = currentProject;
		Iterator<ICompilationUnit> iterator = units.iterator();
		monitor.beginTask("Arquillian validator", units.size());
		while (iterator.hasNext()) {
			ICompilationUnit unit = iterator.next();
			checkCancel(monitor);
			if (unit == null || !(unit.getUnderlyingResource() instanceof IFile) || unit.findPrimaryType() == null) {
				continue;
			}
			if (monitor != null) {
				monitor.subTask("Validating " + unit.getElementName());
				monitor.worked(1);
			}
			cleanupMarkers(unit.getUnderlyingResource());
			checkCancel(monitor);
			IType primaryType = unit.findPrimaryType();
        	String preference = ArquillianUtility.getPreference(ArquillianConstants.MISSING_DEPLOYMENT_METHOD, project);
        	if (!JavaCore.IGNORE.equals(preference) && !ArquillianSearchEngine.hasDeploymentMethod(primaryType)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			storeProblem(unit, "Arquillian test requires at least one method annotated with @Deployment", severity, ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        	}
        	checkCancel(monitor);
        	preference = ArquillianUtility.getPreference(ArquillianConstants.MISSING_TEST_METHOD, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference) && !ArquillianSearchEngine.hasTestMethod(primaryType)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			storeProblem(unit, "Arquillian test requires at least one method annotated with @Test", severity);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        	}
        	checkCancel(monitor);
        	preference = ArquillianUtility.getPreference(ArquillianConstants.INVALID_ARCHIVE_NAME, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference)) {
        		try {
        			Integer severity = ArquillianUtility.getSeverity(preference);
        			IJavaProject javaProject = JavaCore.create(project);
        			validateArchiveName(unit, javaProject, severity);
				} catch (CoreException e) {
					ArquillianCoreActivator.log(e);
				}
        	}
        	checkCancel(monitor);
        	preference = ArquillianUtility.getPreference(ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, project.getProject());
        	if (!JavaCore.IGNORE.equals(preference)) {
				DependencyCache.getDependencies().remove(unit);
				ArchiveContainer.remove(unit.getUnderlyingResource());
				List<Archive> archives = ArquillianSearchEngine.getDeploymentArchives(primaryType, true); 
				if (archives == null) {
					continue;
				}
            	Set<DependencyType> dependencies = DependencyCache.getDependentTypes(unit);
            	Integer severity = ArquillianUtility.getSeverity(preference);
            	checkCancel(monitor);
            	Set<DependencyType> unitDependencies = DependencyCache.getDependencies().get(unit);
				for (DependencyType type : dependencies) {
					checkCancel(monitor);
					if (!getDeployment(type, archives)) {
						checkCancel(monitor);
						boolean direct = unitDependencies != null && unitDependencies.contains(type);
						if (direct) {
							String message = "The " + type.getName() + " type is not included in any deployment.";
							for (TypeLocation location : type.getLocations()) {
								try {
									IMarker marker = storeProblem(unit, message, severity,ArquillianConstants.MARKER_CLASS_ID);
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
								IMarker marker = storeProblem(unit, message, severity,ArquillianConstants.MARKER_CLASS_ID);
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
        	}
		}
	}

	private boolean getDeployment(DependencyType type, List<Archive> archives) {
    	if (type == null || archives == null || archives.size() <= 0) {
    		return false;
    	}
    	for (Archive archive:archives) {
    		String name = type.getName();
    		if (name == null || archive == null) {
    			return false;
    		}
    		archive.getChildren();
    		if (archive.getFullyQuallifiedNames().contains(name)) {
    			return true;
    		}
    	}
		return false;
	}
	
	private List<ICompilationUnit> getCompilationUnits() throws JavaModelException {
		List<ICompilationUnit> units = new ArrayList<ICompilationUnit>();
		IJavaProject javaProject = JavaCore.create(currentProject);
		if (ArquillianSearchEngine.hasArquillianType(javaProject)) {
			IClasspathEntry[] entries = javaProject.getRawClasspath();
			for (IClasspathEntry entry:entries) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(entry);
					for (IPackageFragmentRoot root:roots) {
						if (root.isArchive()) {
							continue;
						}
						IJavaElement[] children = root.getChildren();
						for (IJavaElement child:children) {
							if (child instanceof IPackageFragment) {
								IPackageFragment packageFragment = (IPackageFragment) child;
								ICompilationUnit[] cus = packageFragment.getCompilationUnits();
								for (ICompilationUnit cu:cus) {
									if (!units.contains(cu) && ArquillianSearchEngine.isArquillianJUnitTest(cu, false, false)) {
										units.add(cu);
									}
								}
							}
						}
					}
				}
			}
		}
		return units;
	}

	private boolean hasErrors(IProject project) {
		try {
			IMarker[] markers = project.findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			if (markers != null && markers.length > 0) {
				return true;
			}
			markers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			for (IMarker marker:markers) {
				if (marker == null || marker.getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_ERROR) != IMarker.SEVERITY_ERROR) {
					continue;
				}
				Object object = marker.getAttribute(IJavaModelMarker.ID);
				if (object instanceof Integer) {
					Integer id = (Integer) object;
					if (id.intValue() == IProblem.ConflictingImport) {
						return true;
					}
				}
			}
			return false;
		} catch (CoreException e) {
			ArquillianCoreActivator.log(e);
			return true;
		}
	}

	private void cleanupMarkers(IResource resource) {
		if (resource == null) {
			return;
		}
		try {
			resource.deleteMarkers(ArquillianConstants.MARKER_CLASS_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_RESOURCE_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			ArquillianCoreActivator.log(e);
		}
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		this.currentProject = getProject();
		if (currentProject == null || !currentProject.isAccessible()) {
			return;
		}
		cleanupMarkers(currentProject);
		cleanProject();
	}
	
	private IProject[] getRequiredProjects(boolean includeBinaryPrerequisites) {
		JavaProject javaProject = (JavaProject) JavaCore.create(this.currentProject);
		IWorkspaceRoot workspaceRoot = this.currentProject.getWorkspace().getRoot();
		if (javaProject == null || workspaceRoot == null) return NO_PROJECTS;

		ArrayList projects = new ArrayList();
		ExternalFoldersManager externalFoldersManager = JavaModelManager.getExternalManager();
		try {
			IClasspathEntry[] entries = javaProject.getExpandedClasspath();
			for (int i = 0, l = entries.length; i < l; i++) {
				IClasspathEntry entry = entries[i];
				IPath path = entry.getPath();
				IProject p = null;
				switch (entry.getEntryKind()) {
					case IClasspathEntry.CPE_PROJECT :
						p = workspaceRoot.getProject(path.lastSegment()); // missing projects are considered too
						if (((ClasspathEntry) entry).isOptional() && !JavaProject.hasJavaNature(p)) // except if entry is optional
							p = null;
						break;
					case IClasspathEntry.CPE_LIBRARY :
						if (includeBinaryPrerequisites && path.segmentCount() > 0) {
							// some binary resources on the class path can come from projects that are not included in the project references
							IResource resource = workspaceRoot.findMember(path.segment(0));
							if (resource instanceof IProject) {
								p = (IProject) resource;
							} else {
								resource = externalFoldersManager.getFolder(path);
								if (resource != null)
									p = resource.getProject();
							}
						}
				}
				if (p != null && !projects.contains(p))
					projects.add(p);
			}
		} catch(JavaModelException e) {
			return NO_PROJECTS;
		}
		IProject[] result = new IProject[projects.size()];
		projects.toArray(result);
		return result;
	}
	
	private void storeProblem(ICompilationUnit unit, String message,
			Integer severity) throws CoreException {
		storeProblem(unit, message, severity, ArquillianConstants.MARKER_CLASS_ID);
	}
	 
	private IMarker storeProblem(ICompilationUnit unit, String message, Integer severity, String type)
			throws CoreException {
		if (severity == null || unit == null || unit.getUnderlyingResource() == null) {
			return null;
		}
		IMarker marker = unit.getUnderlyingResource().createMarker(type);
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
		IMember javaElement = unit.findPrimaryType();
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

	private CompilationUnit getAST(final ICompilationUnit unit, IJavaProject project) {
		ASTParser parser= ASTParser.newParser(AST.JLS8);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(project);
		return (CompilationUnit) parser.createAST(null);
	}
	
	private void validateArchiveName(final ICompilationUnit unit, IJavaProject project, final Integer severity) throws CoreException {
		CompilationUnit rootAst = getAST(unit, project);
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
										IMarker marker = storeProblem(unit, "Archive name is invalid. A name with the " + extension + " extension is expected.", severity, ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID);
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

}
