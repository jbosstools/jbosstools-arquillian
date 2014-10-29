/*************************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * @see IMarkerResolution
 * 
 * @author Snjeza
 *
 */
public class FixInvalidDeploymentMethodMarkerResolution implements
		IMarkerResolution {
	
	private IMarker marker;
	protected MethodDeclaration deploymentMethod;
	private IFile file;
	private ICompilationUnit icu;

	public FixInvalidDeploymentMethodMarkerResolution(IMarker marker) {
		this.marker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel() {
		int modifier = marker.getAttribute(ArquillianConstants.MODIFIER_TYPE, ArquillianConstants.MODIFIER_TYPE_UNKNOWN);
		String name = marker.getAttribute(ArquillianConstants.METHOD_NAME, "");
		switch (modifier) {
		case Modifier.PUBLIC:
			return "Change " + name + " to public";

		case Modifier.STATIC:
			return "Change " + name + " to static";

		case Modifier.STATIC & Modifier.PUBLIC:
			return "Change " + name + " to public static";

		default:
			break;
		}
		return "";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker) {
		CompilationUnit root = getAST();
		final int markerStart = marker.getAttribute(IMarker.CHAR_START, 0);
		final int markerEnd = marker.getAttribute(IMarker.CHAR_END, 0);
		
		if (root == null || markerStart <= 0 || markerEnd <= markerStart) {
			ArquillianUIActivator.logWarning("Invalid Deployment method resolution");
			return;
		}
		root.accept(new ASTVisitor() {

			@Override
			public boolean visit(MethodDeclaration node) {
				int startPosition = node.getStartPosition();
				int length = node.getLength();
				if (markerStart >= startPosition && markerEnd <= startPosition + length) {
					deploymentMethod = node;
					node.resolveBinding();
					return false;
				}
				return true;
			}

		});
		if (deploymentMethod == null) {
			ArquillianUIActivator.logWarning("Invalid Deployment method resolution");
			return;
		}
		ASTRewrite rewriter = ASTRewrite.create(deploymentMethod.getAST());
		MethodDeclaration newMethodDeclaration = (MethodDeclaration) rewriter.createCopyTarget(deploymentMethod);
		ModifierRewrite listRewrite= ModifierRewrite.create(rewriter, deploymentMethod);
		listRewrite.setModifiers(Modifier.PUBLIC|Modifier.STATIC, Modifier.PRIVATE|Modifier.PROTECTED, null);
		rewriter.replace(deploymentMethod, newMethodDeclaration, null);
		
		IDocumentProvider provider = new TextFileDocumentProvider();
		try {
			provider.connect(file);
			IDocument doc = provider.getDocument(file);
			TextEdit edits = rewriter.rewriteAST(doc , icu.getJavaProject().getOptions(true));
			
	        edits.apply(doc);
	        
	        CodeFormatter formatter = ToolFactory.createCodeFormatter(icu.getJavaProject().getOptions(true));
	        
			TextEdit formatEdits = formatter.format(CodeFormatter.K_COMPILATION_UNIT, doc.get(),
					edits.getOffset(), edits.getLength(), 1, TextUtilities.getDefaultLineDelimiter(doc));
	
			formatEdits.apply(doc);
	        
			icu.save(new NullProgressMonitor(), true);
	    } catch (IllegalArgumentException e) {
			ArquillianUIActivator.log(e);
		} catch (MalformedTreeException e) {
			ArquillianUIActivator.log(e);
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		} catch (BadLocationException e) {
			ArquillianUIActivator.log(e);
		} finally {
			provider.disconnect(file);
		}
	}

	private CompilationUnit getAST() {
		IResource resource= marker.getResource();
		if ( !(resource instanceof IFile)) {
			return null;
		}
		file = (IFile) resource;
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return null;
		}
		icu = (ICompilationUnit) element;
		ASTParser parser= ASTParser.newParser(AST.JLS8);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
		return astRoot;
	}
}
