/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.jboss.tools.arquillian.core.internal.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.compiler.SourceFile;
import org.jboss.tools.arquillian.core.internal.util.xpl.ArquillianSecurityException;
import org.jboss.tools.arquillian.core.internal.util.xpl.ArquillianSecurityManager;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianSearchEngine {

	private static final String ARCHIVE_WAR = "archive.war"; //$NON-NLS-1$
	private static final String ARCHIVE_JAR = "archive.jar"; //$NON-NLS-1$
	private static final String VALUE = "value"; //$NON-NLS-1$
	public static final String ARQUILLIAN_JUNIT_ARQUILLIAN = "org.jboss.arquillian.junit.Arquillian"; //$NON-NLS-1$
	public static final String CONTAINER_DEPLOYABLE_CONTAINER = "org.jboss.arquillian.container.spi.client.container.DeployableContainer"; //$NON-NLS-1$

	public static final int CONTAINER_DEPLOYABLE_CONTAINER_NOT_EXISTS = 0;
	
	private static class Annotation {
	
		private static final Annotation RUN_WITH = new Annotation("org.junit.runner.RunWith"); //$NON-NLS-1$
		private static final Annotation TEST = new Annotation("org.junit.Test"); //$NON-NLS-1$
		private static final Annotation DEPLOYMENT = new Annotation(ArquillianUtility.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT);
		private final String fName;
	
		private Annotation(String name) {
			fName= name;
		}
		
		private String getName() {
			return fName;
		}
	
		public boolean annotatesTypeOrSuperTypes(ITypeBinding type, String value) {
			while (type != null) {
				if (annotates(type.getAnnotations(), fName, value)) {
					return true;
				}
				type= type.getSuperclass();
			}
			return false;
		}
	
		public boolean annotatesAtLeastOneMethod(ITypeBinding type) {
			while (type != null) {
				IMethodBinding[] declaredMethods= type.getDeclaredMethods();
				for (int i= 0; i < declaredMethods.length; i++) {
					IMethodBinding curr= declaredMethods[i];
					if (annotates(curr.getAnnotations(), fName, null)) {
						return true;
					}
				}
				type= type.getSuperclass();
			}
			return false;
		}
		
		public boolean annotatesAtLeastOneMethod(ITypeBinding type, String value) {
			while (type != null) {
				IMethodBinding[] declaredMethods= type.getDeclaredMethods();
				for (int i= 0; i < declaredMethods.length; i++) {
					IMethodBinding curr= declaredMethods[i];
					if (annotates(curr.getAnnotations(), fName, value)) {
						return true;
					}
				}
				type= type.getSuperclass();
			}
			return false;
		}
	}
	
	private static class AnnotationSearchRequestor extends SearchRequestor {

		private final Collection fResult;
		private final ITypeHierarchy fHierarchy;

		public AnnotationSearchRequestor(ITypeHierarchy hierarchy, Collection result) {
			fHierarchy= hierarchy;
			fResult= result;
		}

		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				Object element= match.getElement();
				if (element instanceof IType || element instanceof IMethod) {
					IMember member= (IMember) element;
					IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
					addTypeAndSubtypes(type);
				}
			}
		}

		private void addTypeAndSubtypes(IType type) {
			if (fResult.add(type)) {
				IType[] subclasses= fHierarchy.getSubclasses(type);
				for (int i= 0; i < subclasses.length; i++) {
					addTypeAndSubtypes(subclasses[i]);
				}
			}
		}
	}

	/**
	 * Returns whether a Java element is an Arquillian JUnit test
	 * 
	 * @param element the Java element
	 * @param checkDeployment check if there is a deployment method
	 * @param checkTest check if there is a test method
	 * @return true if a Java element is an Arquillian JUnit test
	 */
	public static boolean isArquillianJUnitTest(IJavaElement element, boolean checkDeployment, boolean checkTest) {
		return isArquillianJUnitTest(element, checkDeployment, checkTest, true);
	}
	
	/**
	 * Returns whether a Java element is an Arquillian JUnit test
	 * 
	 * @param element the Java element
	 * @param checkDeployment check if there is a deployment method
	 * @param checkTest check if there is a test method
	 * @param checkAbstract check an abstract class
	 * @return true if a Java element is an Arquillian JUnit test
	 */
	public static boolean isArquillianJUnitTest(IJavaElement element, boolean checkDeployment, boolean checkTest, boolean checkAbstract) {
		try {
			IType testType= null;
			if (element instanceof ICompilationUnit) {
				testType= (((ICompilationUnit) element)).findPrimaryType();
			} else if (element instanceof IClassFile) {
				testType= (((IClassFile) element)).getType();
			} else if (element instanceof IType) {
				testType= (IType) element;
			} else if (element instanceof IMember) {
				testType= ((IMember) element).getDeclaringType();
			}
			if (testType != null && testType.exists()) {
				return isArquillianJUnitTest(testType, checkDeployment, checkTest, checkAbstract);
			}
		} catch (CoreException e) {
			// ignore, return false
		}
		return false;
	}

	public static boolean isAccessibleClass(IType type) throws JavaModelException {
		if (type == null) {
			return false;
		}
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		IJavaElement parent= type.getParent();
		while (true) {
			if (parent instanceof ICompilationUnit || parent instanceof IClassFile) {
				return true;
			}
			if (!(parent instanceof IType) || !Flags.isStatic(flags) || !Flags.isPublic(flags)) {
				return false;
			}
			flags= ((IType) parent).getFlags();
			parent= parent.getParent();
		}
	}

	public static boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (!method.exists())
			return false;
	
		if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags())) {
			return false;
		}
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(ArquillianUtility.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}

	private static boolean isArquillianJUnitTest(IType type, boolean checkDeployment, boolean checkTest, boolean checkAbstract) throws JavaModelException {
		if (isAccessibleClass(type)) {
			ITypeBinding binding = getTypeBinding(type);
			if (binding != null) {
				return isTest(binding, checkDeployment, checkTest, checkAbstract);
			}
		}
		return false;
	
	}

	private static ITypeBinding getTypeBinding(IType type)
			throws JavaModelException {
		ASTParser parser= ASTParser.newParser(AST.JLS4);
		
		if (type.getCompilationUnit() != null) {
			parser.setSource(type.getCompilationUnit());
		} else if (!isAvailable(type.getSourceRange())) { // class file with no source
			parser.setProject(type.getJavaProject());
			IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, null);
			if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
				return (ITypeBinding) bindings[0];
			}
			return null;
		} else {
			parser.setSource(type.getClassFile());
		}
		parser.setFocalPosition(0);
		parser.setResolveBindings(true);
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		ASTNode node= root.findDeclaringNode(type.getKey());
		if (node instanceof TypeDeclaration) {
			return ((TypeDeclaration) node).resolveBinding();
		}
		return null;
	}

	static boolean isAvailable(ISourceRange range) {
		return range != null && range.getOffset() != -1;
	}

	static boolean isTest(ITypeBinding binding, boolean checkDeployment, boolean checkTest, boolean checkAbstract) {
		if (checkAbstract && Modifier.isAbstract(binding.getModifiers()))
			return false;
	
		if (Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding, ARQUILLIAN_JUNIT_ARQUILLIAN)) {
			if (checkDeployment && !Annotation.DEPLOYMENT.annotatesAtLeastOneMethod(binding)) {
				return false;
			}
			if (checkTest && !Annotation.TEST.annotatesAtLeastOneMethod(binding)) {
				return false;
			}
			return true;
		}
		return isTestImplementor(binding);
	}

	private static boolean isImplementor(ITypeBinding type, String interfaceName) {
		ITypeBinding superType= type.getSuperclass();
		if (superType != null && isTestImplementor(superType)) {
			return true;
		}
		ITypeBinding[] interfaces= type.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			ITypeBinding curr= interfaces[i];
			if (interfaceName.equals(curr.getQualifiedName()) || isTestImplementor(curr)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isTestImplementor(ITypeBinding type) {
		return isImplementor(type, ArquillianUtility.TEST_INTERFACE_NAME);
	}

	/**
	 * Validates a deployable container.</br>
	 * The validation works in the following way:
	 * <ul>
	 * <li>scan a classpath</li>
     * <li>if there is no implementation of the DeployableContainer interface, it reports an error</li>
     * <li>if there is exactly one implementation of this interface, nothing reported (the Arquillian environment is valid)</li>
     * <li>if there is more than one implementation of this interface, we would check if there is the META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension resource on the classpath as well as if there is a class defined in this resource.</li>
     * </ul>
     * If both exist, nothing is reported.
     * Otherwise, Arquillian Eclipse reports an error.
     * 
	 * @param javaProject the Java Project
	 * @return true if a project contains a valid deployable container 
	 */
	public static IStatus validateDeployableContainer(IJavaProject javaProject) {
		try {
			IType type = javaProject.findType(CONTAINER_DEPLOYABLE_CONTAINER);
			if (type == null) {
				return new Status(
						IStatus.ERROR,
						ArquillianCoreActivator.PLUGIN_ID,
						CONTAINER_DEPLOYABLE_CONTAINER_NOT_EXISTS,
						"Cannot find 'org.jboss.arquillian.container.spi.client.container.DeployableContainer' on project build path. Do you want to add it.",
						null);
			}
			ITypeHierarchy hierarchy = type.newTypeHierarchy(javaProject, new NullProgressMonitor());
            IType[] subTypes = hierarchy.getAllSubtypes(type);
            int count = 0;
            for (IType subType:subTypes) {
            	if (isNonAbstractClass(subType)) {
            		count++;
            	}
            }
            if (count == 0) {
            	return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, 1 ,  
            			"Arquillian tests require exactly one implementation of DeploymentContainer on the build path. Do you want to configure it?", null);
            }
            if (count == 1) {
            	return Status.OK_STATUS;
            }
            
            IFile file = getFile(javaProject, "META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension");
			if (file != null) {
				InputStream is = null;
				try {
					is = file.getContents();
					if (is != null) {
						String content = IOUtil.toString(is);
						if (content != null && !content.isEmpty()) {
							content = content.trim();
							IType loadableExtension = javaProject
									.findType(content);
							if (loadableExtension != null
									&& loadableExtension.exists()) {
								ITypeBinding binding = getTypeBinding(loadableExtension);
								if (isImplementor(binding, "org.jboss.arquillian.core.spi.LoadableExtension")) {
									return Status.OK_STATUS;
								}
							}
						}
					}
				} catch (Exception e) {
					ArquillianCoreActivator.log(e);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
		} catch (JavaModelException e) {
			return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
		}
		return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, 1 ,  
    			"Arquillian tests require exactly one implementation of DeploymentContainer on the build path. Do you want to configure it?", null);
	}
	
	private static IFile getFile(IJavaProject javaProject, String fileName) throws JavaModelException {
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		for (IClasspathEntry entry : rawClasspath) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPackageFragmentRoot[] roots = javaProject
						.findPackageFragmentRoots(entry);
				if (roots == null) {
					continue;
				}
				for (IPackageFragmentRoot root : roots) {
					IPath path = root.getPath();
					path = path.append(fileName);
					IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
					IFile file = wsRoot.getFile(path);
					if (file != null && file.exists()) {
						return file;
					}			
				}
			}
		}
		return null;
	}

	public static boolean isNonAbstractClass(IType type) throws JavaModelException {
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		if (Flags.isAbstract(flags)) {
			return false;
		}
		return true;
	}
	
	public static IType[] findTests(IRunnableContext context, final IJavaElement element, final ITestKind testKind) throws InvocationTargetException, InterruptedException {
		final Set<IType> result= new HashSet<IType>();

		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InterruptedException, InvocationTargetException {
				try {
					findTestsInContainer(element, result, pm);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		context.run(true, true, runnable);
		return result.toArray(new IType[result.size()]);
	}
	
	public static void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		findTestsInContainer(element, result, pm, true, true, true);
	}
	
	public static void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm,
			boolean checkDeployment, boolean checkTest, boolean checkSuite) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}

		if (element instanceof IType) {
			if (isArquillianJUnitTest((IType) element, checkDeployment, checkTest, true)) {
				result.add(element);
				return;
			}
		}

		if (pm == null)
			pm= new NullProgressMonitor();

		try {
			pm.beginTask(JUnitMessages.JUnit4TestFinder_searching_description, 4);

			IRegion region= CoreTestSearchEngine.getRegion(element);
			ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, new SubProgressMonitor(pm, 1));
			IType[] allClasses= hierarchy.getAllClasses();

			// search for all types with references to RunWith and Test and all subclasses
			HashSet candidates= new HashSet(allClasses.length);
			SearchRequestor requestor= new AnnotationSearchRequestor(hierarchy, candidates);

			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(allClasses, IJavaSearchScope.SOURCES);
			int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			SearchPattern runWithPattern= SearchPattern.createPattern(Annotation.RUN_WITH.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
			//SearchPattern testPattern= SearchPattern.createPattern(Annotation.TEST.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);

			//SearchPattern annotationsPattern= SearchPattern.crateOrPattern(runWithPattern, testPattern);
			SearchPattern annotationsPattern = runWithPattern;
			SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
			new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));

			// find all classes in the region
			for (Iterator iterator= candidates.iterator(); iterator.hasNext();) {
				IType curr= (IType) iterator.next();
				if (isAccessibleClass(curr) && !Flags.isAbstract(curr.getFlags()) && region.contains(curr)) {
					ITypeBinding binding = getTypeBinding(curr);
					if (binding != null && isTest(binding, true, true, true)) {
						result.add(curr);
					}
				}
			}

			if (checkSuite) {
				CoreTestSearchEngine.findSuiteMethods(element, result, new SubProgressMonitor(pm, 1));
			}
		} finally {
			pm.done();
		}
	}
	
	public static boolean hasArquillianType(IJavaProject javaProject) {
		if (javaProject == null) {
			return false;
		}
		try {
			IType type = javaProject.findType(ARQUILLIAN_JUNIT_ARQUILLIAN);
			return type != null;
		} catch (JavaModelException e) {
			// ignore
		}
		return false;
	}

	public static boolean isArquillianJUnitTest(IResourceProxy proxy,
			IJavaProject project) {
		if (proxy == null || project == null || ! (proxy.requestResource() instanceof IFile)) {
			return false;
		}
		IFile file = (IFile) proxy.requestResource();
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return false;
		}
		ICompilationUnit cu = (ICompilationUnit) element;
		IType type = cu.findPrimaryType();
		ITypeBinding binding;
		try {
			if (!isAccessibleClass(type)) {
				return false;
			}
			binding = getTypeBinding(type);
			if (binding == null) {
				return false;
			}
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
			return false;
		}
		if (Modifier.isAbstract(binding.getModifiers())) {
			return false;
		}
		
		if (!Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding, ARQUILLIAN_JUNIT_ARQUILLIAN)) {
			return false;
		}
		
		return true;
	}

	public static boolean hasDeploymentMethod(SourceFile sourceFile,
			IJavaProject project) {
		IType type = getType(sourceFile);
		if (type == null) {
			return false;
		}
		return hasDeploymentMethod(type);
	}

	public static IType getType(SourceFile sourceFile) {
		IFile file = sourceFile.resource;
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return null;
		}
		ICompilationUnit cu = (ICompilationUnit) element;
		IType type = cu.findPrimaryType();
		return type;
	}

	private static boolean hasDeploymentMethod(IType type) {
		if (type == null) {
			return false;
		}
		try {
			return getDeploymentMethods(type).size() > 0;
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
			return false;
		}
	}
	
	public static boolean hasTestMethod(SourceFile sourceFile,
			IJavaProject project) {
		IType type = getType(sourceFile);
		try {
			ITypeBinding binding = getTypeBinding(type);
			return Annotation.TEST.annotatesAtLeastOneMethod(binding);
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
			return false;
		}
	}
	
	public static boolean annotates(IAnnotationBinding[] annotations, String fName, String val) {
		for (int i= 0; i < annotations.length; i++) {
			ITypeBinding annotationType= annotations[i].getAnnotationType();
			if (annotationType != null && (annotationType.getQualifiedName().equals(fName))) {
				if (val == null) {
					return true;
				}
				IMemberValuePairBinding[] pairs = annotations[i].getAllMemberValuePairs();
				if (pairs != null) {
					for (IMemberValuePairBinding pair : pairs) {
						if (VALUE.equals(pair.getName())) {
							Object object = pair.getValue();
							if (object instanceof ITypeBinding) {
								ITypeBinding value = (ITypeBinding) object;
								if (val.equals(value.getQualifiedName())) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return  false;
	}
	
	public static List<File> getDeploymentArchives(IType type, boolean create) {
		List<File> archives = new ArrayList<File>();
		if (type == null || !hasDeploymentMethod(type)) {
			return archives;
		}
		Bundle bundle = Platform.getBundle(ArquillianCoreActivator.PLUGIN_ID);
		if (bundle == null) {
			ArquillianCoreActivator.log("The " + ArquillianCoreActivator.PLUGIN_ID + "bundle is invalid.");
			return archives;
		}
		IPath stateLocation = InternalPlatform.getDefault().getStateLocation(
				bundle, true);
		String projectName = null;
		ICompilationUnit cu = type.getCompilationUnit();
		IJavaProject javaProject = null;
		if (cu != null) {
			IResource resource = cu.getResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project != null) {
					projectName = project.getName();
					javaProject = JavaCore.create(project);
				}
			}
		}

		if (projectName == null) {
			ArquillianCoreActivator.log("Cannot find any project for the " + type.getElementName() + "type.");
			return archives;
		}
		IPath location = stateLocation.append(projectName);
		location = location.append("arquillianDeploymentArchives"); //$NON-NLS-1$
		String fqn = type.getFullyQualifiedName();
		fqn = fqn.replace(".", "/");  //$NON-NLS-1$//$NON-NLS-2$
		location = location.append(fqn);
		try {
			List<IMethodBinding> deploymentMethods = getDeploymentMethods(type);
			for (IMethodBinding deploymentMethod : deploymentMethods) {
				String name = deploymentMethod.getName();
				IPath methodLocation = location.append(name);
				File file = methodLocation.toFile();
				if (!file.exists() && create) {
					SecurityManager orig = System.getSecurityManager();
					try {
						System.setSecurityManager(new ArquillianSecurityManager(orig, Thread.currentThread()));
						createArchive(javaProject, type, deploymentMethod, file);
					} catch (ArquillianSecurityException e) {
						ArquillianCoreActivator.log(e);
					} finally {
						System.setSecurityManager(orig);
					}
				}
				if (file.isDirectory()) {
					File[] files = file.listFiles(new FileFilter() {

						@Override
						public boolean accept(File pathname) {
							return pathname.isFile()
									&& pathname.getName().startsWith("archive"); //$NON-NLS-1$
						}
					});
					if (files != null && files.length > 0 && files[0].isFile()) {
						archives.add(files[0]);
					}
				}
			}
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
		}

		return archives;
	}

	private static File createArchive(IJavaProject javaProject, IType type, IMethodBinding deploymentMethod,
			File file) {
		String className = type.getFullyQualifiedName();
		String methodName = deploymentMethod.getName();
		ClassLoader loader = ArquillianCoreActivator.getDefault().getClassLoader(javaProject);
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> clazz = Class.forName(className, true, loader);
			Object object = clazz.newInstance();
			Method method = clazz.getMethod(methodName, new Class[0]);
			
			Object archiveObject = method.invoke(object, new Object[0]);
			Class<?> archiveClass = archiveObject.getClass();
			
			Class<?> exporterClass = Class.forName("org.jboss.shrinkwrap.api.exporter.ZipExporter", true, loader); //$NON-NLS-1$
			Method asMethod = archiveClass.getMethod("as", new Class[] { Class.class }); //$NON-NLS-1$
			Object asObject = asMethod.invoke(archiveObject, new Object[] {exporterClass});
			Class<?> asClass = asObject.getClass();
			Method exportToMethod = asClass.getMethod("exportTo", new Class[] {File.class, boolean.class }); //$NON-NLS-1$
			Class<?> jarClass = Class.forName(ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_JAVA_ARCHIVE, true, loader);
			if (jarClass.isAssignableFrom(archiveClass)) {
				File jarFile = new File(file, ARCHIVE_JAR);
				file.mkdirs();
				exportToMethod.invoke(asObject, new Object[] {jarFile, Boolean.TRUE});
				return jarFile;
			}
			Class<?> warClass = Class.forName(ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_WEB_ARCHIVE, true, loader);
			if (warClass.isAssignableFrom(archiveClass)) {
				File warFile = new File(file, ARCHIVE_WAR);
				
				file.mkdirs();
				exportToMethod.invoke(asObject, new Object[] {warFile, Boolean.TRUE});
				
				return warFile;
			}
		} catch (Exception e) {
			//ArquillianCoreActivator.log(e);
			String message = e.getLocalizedMessage();
			Throwable cause = e.getCause();
			int i = 0;
			while (cause != null && i++ < 5) {
				message = cause.getClass().getName() + ": " + cause.getLocalizedMessage(); //$NON-NLS-1$
				cause = cause.getCause();
			}
			ArquillianCoreActivator.log(message);
			try {
				Integer severity = ArquillianUtility.getSeverity(ArquillianUtility.getPreference(ArquillianConstants.DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED));
				createProblem(message, type, deploymentMethod, severity);
			} catch (CoreException e1) {
				ArquillianCoreActivator.log(e1);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
		return null;
	}

	private static void createProblem(String message, IType type,
			IMethodBinding deploymentMethod, Integer severity) throws CoreException {
		if (severity == null || type == null || type.getJavaProject() == null) {
			return;
		}
		boolean enable = ArquillianUtility.isValidatorEnabled(type.getJavaProject().getProject());
		if (!enable) {
			return;
		}
		ICompilationUnit cu = type.getCompilationUnit();
		if (cu == null) {
			return;
		}
		IResource resource = cu.getResource();
		if (resource == null) {
			return;
		}
		IMarker marker = resource
				.createMarker(ArquillianConstants.MARKER_RESOURCE_ID);
    	
		String[] allNames =  {
		    	IMarker.MESSAGE,
		    	IMarker.SEVERITY,
		    	IJavaModelMarker.ID,
		    	IMarker.CHAR_START,
		    	IMarker.CHAR_END,
		    	IMarker.SOURCE_ID,
		    };
		
		Object[] allValues = new Object[allNames.length];
		int index = 0;
		allValues[index++] = message;
		
		allValues[index++] = severity;
        
		allValues[index++] = ArquillianConstants.ARQUILLIAN_PROBLEM_ID;
		
		IJavaElement javaElement = deploymentMethod.getJavaElement();
		ISourceRange range = null;
		if (javaElement instanceof IMember) {
			IMember member = (IMember) javaElement;
			if (javaElement != null) {
				try {
					range = member.getNameRange();
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
		}
		int start = range == null ? 0 : range.getOffset();
		int end = range == null ? 1 : start + range.getLength();
		
		allValues[index++] = new Integer(start); // start
		allValues[index++] = new Integer(end > 0 ? end + 1 : end); // end

		allValues[index++] = ArquillianConstants.SOURCE_ID;
		
		marker.setAttributes(allNames, allValues);
	}

	public static List<IMethodBinding> getDeploymentMethods(IType type) throws JavaModelException {
		List<IMethodBinding> methodBindings = new ArrayList<IMethodBinding>();
		if (type == null) {
			return methodBindings;
		}
		ITypeBinding binding = getTypeBinding(type);
		while (binding != null) {
			IMethodBinding[] declaredMethods= binding.getDeclaredMethods();
			for (IMethodBinding curr:declaredMethods) {
				if (isDeploymentMethod(curr)) {
					methodBindings.add(curr);
				}
			}
			binding = binding.getSuperclass();
		}
		return methodBindings;
	}
	
	public static boolean isDeploymentMethod(IMethodBinding methodBinding) {
		if (methodBinding == null) {
			return false;
		}
		if (annotates(methodBinding.getAnnotations(), ArquillianUtility.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT, null)) {
			int modifiers = methodBinding.getModifiers();
			if ( (modifiers & Modifier.PUBLIC) != 0 &&
					(modifiers & Modifier.STATIC) != 0 &&
					methodBinding.getParameterTypes().length == 0) {
				ITypeBinding returnType = methodBinding.getReturnType();
				if (isArchiveType(returnType)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isArchiveType(ITypeBinding returnType) {
		if (returnType == null) {
			return false;
		}
		if ("org.jboss.shrinkwrap.api.Archive".equals(returnType
				.getBinaryName())) {
			return true;
		}
		ITypeBinding[] interfaces = returnType.getInterfaces();
		for (ITypeBinding t:interfaces) {
			if (isArchiveType(t)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isDeploymentMethod(IMethod method) {
		if (method == null || !method.exists())
			return false;
	
		try {
			if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags())) {
				return false;
			}
			if (method.getParameters().length > 0) {
				return false;
			}
			String type = method.getReturnType();
			if (type == null) {
				return false;
			}
			String typeSig = Signature.toString(type);
			if (!"Archive<?>".equals(typeSig)) {
				return false;
			}
			IAnnotation deployment = method.getAnnotation("Deployment");
			if (deployment != null && deployment.exists()) {
				return true;
			}
		} catch (JavaModelException e) {
			ArquillianCoreActivator.log(e);
			return false;
		}
		
		return false;
	}

}
