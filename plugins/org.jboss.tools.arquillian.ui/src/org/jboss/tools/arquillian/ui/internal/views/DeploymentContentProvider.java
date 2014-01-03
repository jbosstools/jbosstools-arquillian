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
package org.jboss.tools.arquillian.ui.internal.views;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.arquillian.core.internal.archives.Archive;
import org.jboss.tools.arquillian.core.internal.archives.IEntry;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;

/**
 * 
 * @author snjeza
 *
 */
public class DeploymentContentProvider implements ITreeContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ICompilationUnit) {
			ICompilationUnit cu = (ICompilationUnit) parentElement;
			if (ArquillianSearchEngine.isArquillianJUnitTest(cu, true, false, false)) {
				IType type = cu.findPrimaryType();
				if (type == null) {
					return null;
				}
				//boolean create = !ArquillianUtility.isValidatorEnabled(cu.getJavaProject().getProject());
				boolean create = false;
				List<Archive> archives = ArquillianSearchEngine.getDeploymentArchives(type, create);
				if (archives == null || archives.size() <= 0) {
					return null;
				}
				for (Archive archive:archives) {
					archive.getChildren();
				}
				return archives.toArray(new IEntry[0]);
			}
		}
		if (parentElement instanceof IEntry) {
			IEntry entry = (IEntry) parentElement;
			return entry.getChildren().toArray(new IEntry[0]);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof IEntry) {
			((IEntry) element).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] elements = getChildren(element);
		return elements != null && elements.length > 0;
	}

}
