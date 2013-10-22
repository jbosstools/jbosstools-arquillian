/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Nippon Telegraph and Telephone Corporation - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.editors.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.model.GroupInfo;

/**
 * Wizard for creating or editing group element. 
 *
 */
public class GroupWizard extends Wizard implements INewWizard {

	private Group group;
	
	private TreeViewer viewer;
	private GroupWizardPage page;
	
	private boolean preference;
	
	public GroupWizard(TreeViewer viewer) {
		this(viewer, null);
	}
	
	public GroupWizard(TreeViewer viewer, Group group) {
		this.viewer = viewer;
		this.group = group;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if(group == null) {
			setWindowTitle("Add Group");
		} else {
			setWindowTitle("Edit Group");
		}
	}
	
	@Override
	public void addPages() {
		Arquillian arquillian = (Arquillian) viewer.getInput();
		page = new GroupWizardPage(arquillian, group);
		page.setPreference(preference);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		Arquillian arquillian = (Arquillian) viewer.getInput();
		GroupInfo info = null;
		if(page.getPreferenceContainers() != null) {
			info = new GroupInfo(page.getQualifier(), page.getPreferenceContainers());
		} else {
			info = new GroupInfo(page.getQualifier());
		}
		if(group == null) {
			arquillian.add(info);
		} else {
			arquillian.edit(group, info);
		}
		viewer.refresh();
		return true;
	}

	public void preference() {
		preference = true;
	}
}
