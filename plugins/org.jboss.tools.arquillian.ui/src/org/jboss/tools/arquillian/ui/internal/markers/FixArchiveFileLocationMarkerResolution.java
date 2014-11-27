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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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
public class FixArchiveFileLocationMarkerResolution implements
		IMarkerResolution {
	
	private IMarker marker;
	private IFile file;
	private ICompilationUnit icu;
	private MethodDeclaration deploymentMethod;
	private SimpleName oldName;
	private boolean save;

	public FixArchiveFileLocationMarkerResolution(IMarker marker, boolean save) {
		this.marker = marker;
		this.save = save;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel() {
		String newMethodName = marker.getAttribute(ArquillianConstants.NEW_METHOD_NAME, null);
		String oldMethodName = marker.getAttribute(ArquillianConstants.OLD_METHOD_NAME, null);
		if (newMethodName != null && oldMethodName != null) {
			return "Change '" + oldMethodName + "' to '" + newMethodName + "'";
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

		final String newMethodName = marker.getAttribute(ArquillianConstants.NEW_METHOD_NAME, null);

		if (root == null || markerStart <= 0 || markerEnd <= markerStart
				|| newMethodName == null) {
			ArquillianUIActivator.logWarning("Invalid Deployment method resolution");
			return;
		}
		root.accept(new ASTVisitor() {

			@Override
			public boolean visit(MethodDeclaration node) {
				int startPosition = node.getStartPosition();
				int length = node.getLength();
				if (markerStart >= startPosition
						&& markerEnd <= startPosition + length) {
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

		deploymentMethod.accept(new ASTVisitor() {

			@Override
			public boolean visit(SimpleName node) {
				int startPosition = node.getStartPosition();
				int length = node.getLength();
				if (markerStart == startPosition
						&& markerEnd == startPosition + length) {
					oldName = node;
					node.resolveBinding();
					return false;
				}
				return true;
			}
		});

		if (oldName == null) {
			ArquillianUIActivator.logWarning("Invalid Deployment method resolution");
			return;
		}

		IDocumentProvider provider = new TextFileDocumentProvider();
		try {
			provider.connect(file);
			IDocument doc = provider.getDocument(file);
			ASTRewrite rewrite = ASTRewrite.create(root.getAST());

			SimpleName newName = root.getAST().newSimpleName(newMethodName);
			rewrite.replace(oldName, newName, null);
			
			TextEdit edits = rewrite.rewriteAST(doc, icu.getJavaProject()
					.getOptions(true));
			edits.apply(doc);
			
			icu.save(new NullProgressMonitor(), true);
			if (save) {
				provider.saveDocument(new NullProgressMonitor(), file, doc, false);
			}
		} catch (JavaModelException e) {
			ArquillianUIActivator.log(e);
		} catch (IllegalArgumentException e) {
			ArquillianUIActivator.log(e);
		} catch (MalformedTreeException e) {
			ArquillianUIActivator.log(e);
		} catch (BadLocationException e) {
			ArquillianUIActivator.log(e);
		} catch (CoreException e) {
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
