package org.jboss.tools.arquillian.core.internal.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
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
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

public class ArquillianSearchEngine {

	private static final String ARQUILLIAN_JUNIT_ARQUILLIAN = "org.jboss.arquillian.junit.Arquillian";
	public static final String CONTAINER_DEPLOYABLE_CONTAINER = "org.jboss.arquillian.container.spi.client.container.DeployableContainer";

	private static class Annotation {
	
		private static final Annotation RUN_WITH = new Annotation("org.junit.runner.RunWith"); //$NON-NLS-1$
		private static final Annotation TEST = new Annotation("org.junit.Test"); //$NON-NLS-1$
		private static final Annotation DEPLOYMENT = new Annotation(ArquillianUtils.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT);
		private final String fName;
	
		private Annotation(String name) {
			fName= name;
		}
		
		private String getName() {
			return fName;
		}
	
		private boolean annotates(IAnnotationBinding[] annotations) {
			for (int i= 0; i < annotations.length; i++) {
				ITypeBinding annotationType= annotations[i].getAnnotationType();
				if (annotationType != null && (annotationType.getQualifiedName().equals(fName))) {
					IMemberValuePairBinding[] pairs = annotations[i].getAllMemberValuePairs();
					if (pairs != null) {
						for (IMemberValuePairBinding pair : pairs) {
							if ("value".equals(pair.getName())) {
								Object object = pair.getValue();
								if (object instanceof ITypeBinding) {
									ITypeBinding value = (ITypeBinding) object;
									if (ARQUILLIAN_JUNIT_ARQUILLIAN.equals(value.getQualifiedName())) {
										return true;
									}
								}
							}
						}
					}
					return true;
				}
			}
			return  false;
		}
	
		public boolean annotatesTypeOrSuperTypes(ITypeBinding type) {
			while (type != null) {
				if (annotates(type.getAnnotations())) {
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
					if (annotates(curr.getAnnotations())) {
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


	public static boolean isArquillianJUnitTest(IJavaElement element, boolean checkDeployment) {
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
				return isArquillianJunitTest(testType, checkDeployment);
			}
		} catch (CoreException e) {
			// ignore, return false
		}
		return false;
	}

	public static boolean isAccessibleClass(IType type) throws JavaModelException {
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
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(ArquillianUtils.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}

	private static boolean isArquillianJunitTest(IType type, boolean checkDeployment) throws JavaModelException {
		if (isAccessibleClass(type)) {
			if (hasSuiteMethod(type)) {
				return true;
			}
			ITypeBinding binding = getTypeBinding(type);
			if (binding != null) {
				return isTest(binding, checkDeployment);
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

	static boolean isTest(ITypeBinding binding, boolean checkDeployment) {
		if (Modifier.isAbstract(binding.getModifiers()))
			return false;
	
		if (Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding) && Annotation.TEST.annotatesAtLeastOneMethod(binding)) {
			if (!checkDeployment) {
				return true;
			}
			return Annotation.DEPLOYMENT.annotatesAtLeastOneMethod(binding);
		}
		return isTestImplementor(binding);
	}

	public static boolean isTestImplementor(ITypeBinding type) {
		ITypeBinding superType= type.getSuperclass();
		if (superType != null && isTestImplementor(superType)) {
			return true;
		}
		ITypeBinding[] interfaces= type.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			ITypeBinding curr= interfaces[i];
			if (ArquillianUtils.TEST_INTERFACE_NAME.equals(curr.getQualifiedName()) || isTestImplementor(curr)) {
				return true;
			}
		}
		return false;
	}

	public static IStatus validateDeployableContainer(IJavaProject javaProject) {
		try {
			IType type = javaProject.findType(CONTAINER_DEPLOYABLE_CONTAINER);
			if (type == null) {
				return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, "Cannot find 'org.jboss.arquillian.container.spi.client.container.DeployableContainer' on project build path. Arquillian tests can only be run if DeployableContainer is on the build path.");
			}
			ITypeHierarchy hierarchy = type.newTypeHierarchy(new NullProgressMonitor());
            IType[] subTypes = hierarchy.getAllSubtypes(type);
            int count = 0;
            for (IType subType:subTypes) {
            	if (isNonAbstractClass(subType)) {
            		count++;
            	}
            }
            if (count != 1) {
            	return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, 
            			"Arquillian tests require exactly one implementation of DeploymentContainer on the build path." +
            			" Please check classpath for conflicting jar versions.");
            }
		} catch (JavaModelException e) {
			return new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
		}
		return Status.OK_STATUS;
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
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}

		if (element instanceof IType) {
			if (isArquillianJUnitTest((IType) element, true)) {
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
					if (binding != null && isTest(binding, true)) {
						result.add(curr);
					}
				}
			}

			// add all classes implementing JUnit 3.8's Test interface in the region
//			IType testInterface= element.getJavaProject().findType(JUnitCorePlugin.TEST_INTERFACE_NAME);
//			if (testInterface != null) {
//				CoreTestSearchEngine.findTestImplementorClasses(hierarchy, testInterface, region, result);
//			}

			//JUnit 4.3 can also run JUnit-3.8-style public static Test suite() methods:
			CoreTestSearchEngine.findSuiteMethods(element, result, new SubProgressMonitor(pm, 1));
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

}
