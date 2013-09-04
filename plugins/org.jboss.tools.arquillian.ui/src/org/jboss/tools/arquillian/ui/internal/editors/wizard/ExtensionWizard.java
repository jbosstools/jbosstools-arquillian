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
import org.jboss.tools.arquillian.ui.internal.editors.model.Extension;
import org.jboss.tools.arquillian.ui.internal.editors.model.ExtensionInfo;

/**
 * Wizard for creating or editing extension element. 
 *
 */
public class ExtensionWizard extends Wizard implements INewWizard {
	
	private Extension extension;
	
	private TreeViewer viewer;
	private ExtensionWizardPage page;
	
	public ExtensionWizard(TreeViewer viewer) {
		this(viewer, null);
	}
	
	public ExtensionWizard(TreeViewer viewer, Extension extension) {
		this.viewer = viewer;
		this.extension = extension;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if(extension == null) {
			setWindowTitle("Add Extension");
		} else {
			setWindowTitle("Edit Extension");
		}
	}

	@Override
	public void addPages() {
		page = new ExtensionWizardPage((Arquillian) viewer.getInput(), extension);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		Arquillian arquillian = (Arquillian) viewer.getInput();
		ExtensionInfo info = new ExtensionInfo(page.getType(), page.getProperties());
		if(extension == null) {
			arquillian.add(info);
		} else {
			arquillian.edit(extension, info);
		}
		viewer.refresh();
		return true;
	}
	
}
