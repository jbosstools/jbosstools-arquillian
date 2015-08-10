/*************************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.filters;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class WorkingSetFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IProject) {
			IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = workbenchWindow.getActivePage();
			ISelection selection = page.getSelection();
			IProject selectedProject = ArquillianUIUtil.getSelectedProject(selection);
			if (selectedProject == null) {
				return false;
			}
			if (element.equals(selectedProject)) {
				return true;
			}
			IWorkingSetManager manager = workbenchWindow.getWorkbench()
		            .getWorkingSetManager();
			IWorkingSet[] workingSets = manager.getWorkingSets();
			List<IWorkingSet> selectedWorkingSets = new ArrayList<>();
			for (IWorkingSet workingSet:workingSets) {
				IAdaptable[] projects = workingSet.getElements();
				for (IAdaptable adaptable:projects) {
					IProject project = adaptable.getAdapter(IProject.class);
					if (selectedProject.equals(project)) {
						selectedWorkingSets.add(workingSet);
					}
				}
			}
			for (IWorkingSet workingSet:selectedWorkingSets) {
				IAdaptable[] projects = workingSet.getElements();
				for (IAdaptable adaptable:projects) {
					IProject project = adaptable.getAdapter(IProject.class);
					if (element.equals(project)) {
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}

}
