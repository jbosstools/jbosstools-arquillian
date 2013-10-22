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
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IExtension;
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
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.tools.arquillian.core.internal.extension.ExtensionParser;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditor;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.Extension;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.PropertyLabelProvider;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

/**
 * Wizard page for creating or editing extension element. 
 *
 */
public class ExtensionWizardPage extends WizardPage {

	private Arquillian arquillian;
	private Extension extension;
	
	private Combo extensions;
	private TableViewer properties;
	
	private String selectedExtension;
	
	public ExtensionWizardPage(Arquillian arquillian, Extension extension) {
		super("Extension Wizard Page");
		this.arquillian = arquillian;
		this.extension = extension;
		setTitle("Extension");
		if(extension == null) {
			setDescription("Create a new extension");
		} else {
			setDescription("Edit an extension");
		}
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		new Label(composite, SWT.NONE).setText("type:");
		extensions = new Combo(composite, SWT.READ_ONLY);
		if(extension != null) {
			extensions.add(extension.getQualifier());
			extensions.setEnabled(false);
		} else {
			extensions.add("");
			List<String> addedExtensions = arquillian.getExtensionQualifiers();
			// Added extensions remove from combo list.
			for(org.jboss.tools.arquillian.core.internal.extension.Extension ext : ExtensionParser.getExtensions()) {
				if(addedExtensions.contains(ext.getQualifier())) {
					continue;
				}
				extensions.add(ext.getQualifier());
			}
			
		}
		
		Label label = new Label(composite, SWT.NONE);
		label.setText("property:");
		label.setLayoutData(LayoutUtil.createTableWrapData(2, SWT.DEFAULT));
		
		properties = new TableViewer(new Table(composite, SWT.BORDER | SWT.FULL_SELECTION));
		properties.getTable().setLinesVisible(true);
		properties.getTable().setHeaderVisible(true);
		properties.getTable().setLayoutData(LayoutUtil.createTableWrapData(2, 160));
		properties.setContentProvider(new ArrayContentProvider());
		ArquillianEditor.createPropertyColumns(properties);
		
		properties.setLabelProvider(new PropertyLabelProvider());
		
		extensions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do nothing if same type is selected.
				if(extensions.getText().equals(selectedExtension)) {
					return;
				}
				selectedExtension = extensions.getText();
				
				properties.getTable().removeAll();
				List<Property> props = new ArrayList<Property>();
				org.jboss.tools.arquillian.core.internal.extension.Extension ext = ExtensionParser.getExtension(selectedExtension);
				if(ext != null && ext.getConfigurations() != null) {
					for(Configuration configuration : ext.getConfigurations()) {
						props.add(new Property(configuration.getName(), "", configuration.getType())); //$NON-NLS-1$
					}
				}
				properties.setInput(props);
			}
		});
		extensions.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doValidate();
			}
		});
		
		// restore value
		if(extension != null) {
			extensions.setText(extension.getQualifier());
			selectedExtension = extension.getQualifier();
			org.jboss.tools.arquillian.core.internal.extension.Extension ext = ExtensionParser.getExtension(selectedExtension);
			if(ext != null) {
				properties.setInput(ArquillianEditor.restoreProperties(ext.getConfigurations(), extension.getProperties()));
			}
		}

		doValidate();
		setControl(composite);
	}
	
	private void doValidate() {
		if(StringUtils.isEmpty(extensions.getText())) {
			setErrorMessage("type is empty.");
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	public String getType() {
		return extensions.getText();
	}
	
	@SuppressWarnings("unchecked")
	public List<Property> getProperties() {
		return (List<Property>) properties.getInput();
	}
}
