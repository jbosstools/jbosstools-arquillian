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
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;

/**
 * Wizard for creating or editing container element. 
 *
 */
public class ContainerWizard extends Wizard implements INewWizard {

	private TreeViewer viewer;
	private ContainerWizardPage page;
	
	private Container container;
	private Group group;
	
	private boolean preference;
	private boolean replication;
	
	public ContainerWizard(TreeViewer viewer) {
		this(viewer, null);
	}
	
	public ContainerWizard(TreeViewer viewer, Container container) {
		this.viewer = viewer;
		this.container = container;
		TreeSelection selection = (TreeSelection) viewer.getSelection();
		if(container != null && container.getGroup() != null) {
			group = container.getGroup();
		} else if(selection.getFirstElement() instanceof Group) {
			group = (Group) selection.getFirstElement();
		}
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if(container == null || replication) {
			setWindowTitle("Add Container");
		} else {
			setWindowTitle("Edit Container");
		}
	}

	@Override
	public boolean performFinish() {
		Arquillian arquillian = (Arquillian) viewer.getInput();
		ContainerInfo info = new ContainerInfo(
									group,
									page.getQualifier(),
									page.getContainerType(),
									page.getMode(),
									page.getConfigurations(),
									page.getProtocols());
		if(container == null || replication) {
			arquillian.add(info);
		} else {
			arquillian.edit(container, info);
		}
		viewer.refresh();
		viewer.expandAll();
		return true;
	}

	@Override
	public void addPages() {
		Arquillian arquillian = (Arquillian) viewer.getInput();
		page = new ContainerWizardPage(arquillian, container, group);
		page.setPreference(preference);
		page.setReplication(replication);
		addPage(page);
	}
	
	public void preference() {
		this.preference = true;
	}
	
	public void replication() {
		this.replication = true;
	}
}
