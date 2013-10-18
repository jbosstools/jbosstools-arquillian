/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;

/**
 * @see IMarkerResolutionGenerator2
 * 
 * @author snjeza
 *
 */
public class AddMissingTypeResolutionGenerator implements
		IMarkerResolutionGenerator2 {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IResource resource = marker.getResource();
		if (resource == null || !resource.exists() || ! (resource instanceof IFile)) {
			return null;
		}
		IJavaElement element = JavaCore.create(resource);
		if (element != null ) {
			String name = RefactoringUtil.getQuickFixName(marker);
			return new IMarkerResolution[] {
				new AddMissingTypeMarkerResolution(name)	
			};
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			String className = RefactoringUtil.getMissingClassName(marker);
			if (className != null) {
				IResource resource = marker.getResource();
				if (resource != null && resource.exists()) {
					IProject project = resource.getProject();
					IJavaProject javaProject = JavaCore.create(project);
					IType type = javaProject.findType(className);
					if (type == null) {
						int id = marker.getAttribute(IJavaModelMarker.ID, -1);
						if (id == IProblem.ImportNotFound) {
							IJavaElement javaElement = JavaCore.create(resource);
							if (javaElement instanceof ICompilationUnit) {
								ICompilationUnit cu = (ICompilationUnit) javaElement;
								IImportDeclaration[] imports = cu.getImports();
								int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
								int start = marker.getAttribute(IMarker.CHAR_START, -1);
								int end = marker.getAttribute(IMarker.CHAR_END, -1);
								
								for (IImportDeclaration importDeclaration:imports) {
									if (importDeclaration.isOnDemand()) {
										continue;
									}
									ISourceRange name = importDeclaration.getNameRange();
									if (name.getOffset() == start) {
										className = importDeclaration.getElementName();
										type = javaProject.findType(className);
										if (type != null) {
											marker.setAttribute(IMarker.CHAR_END, start + name.getLength());
											marker.setAttribute(ArquillianConstants.MARKER_CLASS_NAME, className);
										}
									}
								}
							}
						}
						if (id == IProblem.UndefinedType) {
							IJavaElement javaElement = JavaCore.create(resource);
							if (javaElement instanceof ICompilationUnit) {
								ICompilationUnit cu = (ICompilationUnit) javaElement;
								IImportDeclaration[] imports = cu.getImports();
								String end = "." + className; //$NON-NLS-1$
								for (IImportDeclaration importDeclaration:imports) {
									if (importDeclaration.isOnDemand()) {
										continue;
									}
									String elementName = importDeclaration.getElementName();
									if (elementName.endsWith(end)) {
										type = javaProject.findType(elementName);
										if (type != null) {
											className = elementName;
											marker.setAttribute(ArquillianConstants.MARKER_CLASS_NAME, className);
										}
									}
								}
							}
						}
					}
					return type != null; // && type.getCompilationUnit();
				}
			}
		} catch (CoreException e) {
			// ignore
		}
		return false;
	}

}
