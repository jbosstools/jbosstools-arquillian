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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.preferences.ContainerGroupPreferencePage;
import org.jboss.tools.arquillian.ui.internal.preferences.model.PreferenceArquillian;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

/**
 * Wizard page for creating or editing group element. 
 *
 */
public class GroupWizardPage extends WizardPage {
	
	private Arquillian arquillian;
	private Group group;
	
	private Text qualifier;
	
	private List<Container> preferenceContainers;
	
	private boolean preference;
	
	public GroupWizardPage(Arquillian arquillian, Group group) {
		super("Group Wizard Page");
		setTitle("Group");
		if(group == null) {
			setDescription("Create a new group");
		} else {
			setDescription("Edit a group");
		}
		this.arquillian = arquillian;
		this.group = group;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		new Label(composite, SWT.NONE).setText("qualifier:");
		qualifier = new Text(composite, SWT.BORDER | SWT.SINGLE);
		qualifier.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		qualifier.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doValidate();
			}
		});
		
		if(!preference) {
			Link loadPreferenceLink = new Link(composite, SWT.NONE);
			loadPreferenceLink.setText("<A>load group from preference</A>");
			loadPreferenceLink.setLayoutData(new TableWrapData(TableWrapData.RIGHT, TableWrapData.TOP, 1, 2));
			loadPreferenceLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ContainerGroupPreferencePage page = new ContainerGroupPreferencePage(PreferenceArquillian.TYPE_GROUP);
					page.setTitle("JBoss Test Plugin");
					page.init(ArquillianUIActivator.getDefault().getWorkbench());
					if(ArquillianUIUtil.openPreferenceDialog(page) == IDialogConstants.OK_ID) {
						Group group = page.getSelectedGroup();
						if(group == null) {
							return;
						}
						qualifier.setText(group.getQualifier());
						preferenceContainers = group.getContainers();
					}
				}
			});
		}
		
		if(group != null) {
			qualifier.setText(group.getQualifier());
		}
		
		doValidate();
		setControl(composite);
	}
	
	protected void doValidate() {
		if(StringUtils.isEmpty(qualifier.getText())) {
			setErrorMessage("qualifier is empty.");
			setPageComplete(false);
			return;
		}
		ContainerGroupModel model = arquillian.getContainerOrGroup(qualifier.getText());
		if(model != null && model != group) {
			setErrorMessage("qualifier is already used.");
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}
	
	public void setPreference(boolean preference) {
		this.preference = preference;
	}

	public String getQualifier() {
		return qualifier.getText();
	}
	
	public List<Container> getPreferenceContainers() {
		return preferenceContainers;
	}
}
