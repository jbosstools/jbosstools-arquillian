/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jboss.tools.arquillian.core.internal.archives.IEntry;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IEntry) {
			return true;
		}
		if (element instanceof IProject) {
			try {
				if ( ((IProject)element).hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create((IProject) element);
					return hasDeployments(javaProject);
				}
			} catch (CoreException e) {
				// ignore
			}
		} else if (element instanceof IPackageFragment) {
			try {
				return ((IPackageFragment)element).hasSubpackages() || hasDeployments((IPackageFragment) element);
			} catch (JavaModelException e) {
				ArquillianUIActivator.log(e);
			}
		} else if (element instanceof IJavaElement) {
			return hasDeployments((IJavaElement) element);
		}
		return false;
	}

	private boolean hasDeployments(IJavaElement element) {
		try {
			if (element instanceof IJavaProject) {
				return ((IJavaProject) element).getProject().hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID);
			}
			if (element instanceof IPackageFragmentRoot) {
				IJavaElement[] children = ((IPackageFragmentRoot)element).getChildren();
				for (IJavaElement child:children) {
					if (hasDeployments(child)) {
						return true;
					}
				}
			}
			if (element instanceof IPackageFragment) {
				ICompilationUnit[] units = ((IPackageFragment)element).getCompilationUnits();
				for (ICompilationUnit unit:units) {
					if (hasDeployments(unit)) {
						return true;
					}
				}
			}
			if (element instanceof ICompilationUnit) {
				return ArquillianSearchEngine.isArquillianJUnitTest((ICompilationUnit) element, true, false, false);
			}
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return false;
	}

}
