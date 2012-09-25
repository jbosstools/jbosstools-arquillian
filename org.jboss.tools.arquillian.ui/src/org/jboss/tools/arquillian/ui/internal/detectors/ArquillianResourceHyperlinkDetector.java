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
package org.jboss.tools.arquillian.ui.internal.detectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianResourceHyperlinkDetector extends
		AbstractHyperlinkDetector {

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
		if (region == null || textEditor == null)
			return null;

		IEditorSite site= textEditor.getEditorSite();
		if (site == null)
			return null;

		ITypeRoot javaElement= JavaUI.getEditorInputTypeRoot(textEditor.getEditorInput());
		if (javaElement == null )
			return null;

		if (!ArquillianUIUtil.isArquillianJUnitTestCase(javaElement.findPrimaryType())) {
			return null;
		}
		CompilationUnit ast= SharedASTProvider.getAST(javaElement, SharedASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, region.getOffset(), 1);
		if (!(node instanceof StringLiteral))
			return null;

		if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
			return null;

		ASTNode parent = node.getParent();
		if (!(parent instanceof MethodInvocation)) {
			return null;
		}
		MethodInvocation methodInvocation = (MethodInvocation) parent;
		SimpleName name = methodInvocation.getName();
		String methodName = name.getFullyQualifiedName();
		if (!ArquillianUIUtil.ADD_AS_RESOURCE_METHOD.equals(methodName) 
				&& !ArquillianUIUtil.ADD_AS_MANIFEST_RESOURCE_METHOD.equals(methodName)
				&& !ArquillianUIUtil.ADD_AS_WEB_INF_RESOURCE_METHOD.equals(methodName)) {
			return null;
		}
		while (parent != null) {
			parent = parent.getParent();
			if (parent instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
				
				int modifiers = methodDeclaration.getModifiers();
				if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
					IMethodBinding binding = methodDeclaration.resolveBinding();
					IMethod method = (IMethod) binding.getJavaElement();
					try {
						String signature = method.getSignature();
						if (!"()QArchive<*>;".equals(signature)) {
							break;
						}
					} catch (JavaModelException e) {
						ArquillianUIActivator.log(e);
					}
					IAnnotationBinding[] annotations = binding.getAnnotations();
					for (IAnnotationBinding annotationBinding:annotations) {
						ITypeBinding typeBinding = annotationBinding.getAnnotationType();
						if (ArquillianUIUtil.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT.equals(typeBinding.getQualifiedName())) {
							StringLiteral stringLiteral = (StringLiteral) node;
							String resource = stringLiteral.getLiteralValue();
							IFile file = getFile(resource, javaElement);
							if (file != null) {
								int start = node.getStartPosition();
								int length = node.getLength();
								if (length > 2) {
									start++;
									length-=2;
								}
								IRegion stringLiteralRegion= new Region(start, length);
								return new IHyperlink[] {new ArquillianResourceHyperlink(resource, stringLiteralRegion, file)};
							}
							break;
						}
						
					}
				}
			}
		}
		
		return null;
	}

	private IFile getFile(String resource, ITypeRoot javaElement) {
		IJavaProject project = javaElement.getJavaProject();
		try {
			IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
			for (IPackageFragmentRoot root:roots) {
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					IPath path = root.getPath();
					path = path.append(resource);
					path = path.removeFirstSegments(1);
					IFile file = project.getProject().getFile(path);
					if (file != null && file.exists()) {
						return file;
					}
				}
			}
		} catch (JavaModelException e) {
			ArquillianUIActivator.log(e);
		}
		return null;
	}

}
