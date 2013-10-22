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

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.Protocol;

/**
 * Wizard for creating or editing protocol element. 
 *
 */
public class ProtocolWizard extends Wizard implements INewWizard {

	private ProtocolWizardPage page;
	private TreeViewer viewer;
	private Protocol protocol;
	
	public ProtocolWizard(TreeViewer viewer) {
		this(viewer, null);
	}
	
	public ProtocolWizard(TreeViewer viewer, Protocol protocol) {
		this.viewer = viewer;
		this.protocol = protocol;
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if(protocol == null) {
			setWindowTitle("Add Protocol");
		} else {
			setWindowTitle("Edit Protocol");
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void addPages() {
		page = new ProtocolWizardPage(protocol);
		page.setContainerProtocols((List<Protocol>) viewer.getInput());
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if(protocol == null) {
			@SuppressWarnings("unchecked")
			List<Protocol> protocols = (List<Protocol>) viewer.getInput();
			Protocol newProtocol = new Protocol(page.getType());
			for(Property property : page.getProperties()) {
				newProtocol.addProperty(property);
			}
			protocols.add(newProtocol);
		} else {
			protocol.getProperties().clear();
			for(Property property : page.getProperties()) {
				protocol.addProperty(property);
			}
		}
		viewer.refresh();
		return true;
	}

}
