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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.tools.arquillian.core.internal.protocol.ProtocolParser;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditor;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.PropertyLabelProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.Protocol;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

/**
 * Wizard page for creating or editing protocol element. 
 *
 */
public class ProtocolWizardPage extends WizardPage {

	private Combo protocols;
	private TableViewer properties;
	
	private Protocol protocol;
	
	private String selectedProtocol;
	
	private List<Protocol> containerProtocols;

	public ProtocolWizardPage(Protocol protocol) {
		super("Protocol Wizard Page");
		setTitle("Protocol");
		if(protocol == null) {
			setDescription("Create a new protocol");
		} else {
			setDescription("Edit a protocol");
		}
		this.protocol = protocol;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new TableWrapLayout());

		// protocol type
		Composite typeComposite = new Composite(composite, SWT.NONE);
		typeComposite.setLayout(LayoutUtil.createTableWrapLayout(2));
		new Label(typeComposite, SWT.NONE).setText("type:");
		protocols = new Combo(typeComposite, SWT.READ_ONLY);
		if(protocol != null) {
			protocols.add(protocol.getType());
			protocols.setEnabled(false);
		} else {
			protocols.add("");
			List<String> addedProtocols = new ArrayList<String>();
			for(Protocol protocol : containerProtocols) {
				addedProtocols.add(protocol.getType());
			}
			for(org.jboss.tools.arquillian.core.internal.protocol.Protocol protocol : ProtocolParser.getProtocols()) {
				if(!addedProtocols.contains(protocol.getType())) {
					protocols.add(protocol.getType());
				}
			}
		}
		
		// properties
		new Label(composite, SWT.NONE).setText("properties:");
		properties = new TableViewer(new Table(composite, SWT.BORDER | SWT.FULL_SELECTION));
		properties.getTable().setLayoutData(LayoutUtil.createTableWrapData(90));
		properties.getTable().setLinesVisible(true);
		properties.getTable().setHeaderVisible(true);
		ArquillianEditor.createPropertyColumns(properties);
		
		properties.setLabelProvider(new PropertyLabelProvider());
		properties.setContentProvider(new ArrayContentProvider());
		
		//---------- attach event listener
		protocols.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do nothing if same type is selected.
				if(protocols.getText().equals(selectedProtocol)) {
					return;
				}
				selectedProtocol = protocols.getText();
				
				properties.getTable().removeAll();
				List<Property> props = new ArrayList<Property>();
				org.jboss.tools.arquillian.core.internal.protocol.Protocol protocol = ProtocolParser.getProtocol(selectedProtocol);
				if(protocol != null && protocol.getConfigurations() != null) {
					for(Configuration configuration : protocol.getConfigurations()) {
						props.add(new Property(configuration.getName(), "", configuration.getType()));
					}
				}
				properties.setInput(props);
			}
		});
		protocols.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doValidate();
			}
		});
		
		// restore value
		if(protocol != null) {
			protocols.setText(protocol.getType());
			selectedProtocol = protocol.getType();
			org.jboss.tools.arquillian.core.internal.protocol.Protocol proto = ProtocolParser.getProtocol(protocol.getType());
			if(proto != null && proto.getConfigurations() != null) {
				properties.setInput(
						ArquillianEditor.restoreProperties(
								proto.getConfigurations(),
								protocol.getProperties()));
			}
		}
		
		doValidate();
		setControl(composite);
	}
	
	private void doValidate() {
		if(StringUtils.isEmpty(protocols.getText())) {
			setErrorMessage("type is empty.");
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	public String getType() {
		return protocols.getText();
	}
	
	@SuppressWarnings("unchecked")
	public List<Property> getProperties() {
		return (List<Property>) properties.getInput();
	}
	
	public void setContainerProtocols(List<Protocol> containerProtocols) {
		this.containerProtocols = containerProtocols;
	}
}
