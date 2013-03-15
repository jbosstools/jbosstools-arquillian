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
package org.jboss.tools.arquillian.ui.internal.filters;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.model.ArquillianZipEntry;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof ArquillianZipEntry) {
			return true;
		}
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject == null || !javaProject.exists()) {
				return false;
			}
			if (!ArquillianSearchEngine.hasArquillianType(javaProject)) {
				return false;
			}
			
			return hasDeplyments(javaProject);
		}
		if (element instanceof IPackageFragmentRoot) {
			return hasDeplyments((IPackageFragmentRoot) element);
		}
		if (element instanceof IPackageFragment) {
			return hasDeplyments((IPackageFragment) element);
		}
		if (element instanceof ICompilationUnit) {
			return ArquillianSearchEngine.isArquillianJUnitTest((ICompilationUnit) element, true, false);
		}
		return false;
	}

	private boolean hasDeplyments(IJavaElement javaProject) {
		Set<IJavaElement> result = new HashSet<IJavaElement>();
		try {
			ArquillianSearchEngine.findTestsInContainer(javaProject, result , null, true, false, false);
			return result.size() > 0;
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
			return false;
		}
	}

}
