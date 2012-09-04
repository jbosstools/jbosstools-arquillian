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
package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * @author snjeza
 *
 */
public class NewArquillianJUnitTestCaseDeploymentPage extends WizardPage {

	public static final String ORG_JBOSS_TOOLS_ARQUILLIAN_UI_DEPLOYMENT_PAGE = "org.jboss.tools.arquillian.ui.deploymentPage";
	public static String[] archiveTypes = { "jar", "war", "ear" };
	private Text methodNameText;
	private Combo archiveTypeCombo;
	private Button beansXmlButton;
	private Text archiveNameText;

	public NewArquillianJUnitTestCaseDeploymentPage() {
		super(ORG_JBOSS_TOOLS_ARQUILLIAN_UI_DEPLOYMENT_PAGE); //$NON-NLS-1$
		
		setTitle("Create Arquillian Deployment Method");
		setDescription("Create Arquillian Deployment Method");
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2,false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		Dialog.applyDialogFont(composite);
		setControl(composite);
		
		// method name
		Label methodNameLabel = new Label(composite, SWT.NONE);
		methodNameLabel.setText("Method name:");
		
		methodNameText = new Text(composite, SWT.BORDER);
		methodNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// archive type
		Label  archiveTypeLabel = new Label(composite, SWT.NONE);
		archiveTypeLabel.setText("Archive type:");
		
		archiveTypeCombo = new Combo(composite, SWT.READ_ONLY);
		archiveTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		archiveTypeCombo.setItems(archiveTypes);
		
		//archive name
		Label archiveNameLabel = new Label(composite, SWT.NONE);
		archiveNameLabel.setText("Archive name:");
		
		archiveNameText = new Text(composite, SWT.BORDER);
		archiveNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		// beans.xml
		beansXmlButton  = new Button(composite, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		beansXmlButton.setText("Add an empty beans.xml file");
		
		// FIXME 
		methodNameText.setText("createDeployment");
		archiveTypeCombo.select(0);
		archiveNameText.setText("test");
		beansXmlButton.setSelection(true);
	}

	@Override
	public IWizardPage getPreviousPage() {
		IWizard wizard = getWizard();
		if (wizard instanceof NewArquillianJUnitTestWizard) {
			NewArquillianJUnitTestWizard arquillianWizard = (NewArquillianJUnitTestWizard) wizard;
			NewArquillianJUnitTestCasePageOne pageOne = arquillianWizard.getNewArquillianJUnitTestCasePageOne();
			if (pageOne.getClassUnderTest() != null) {
				return super.getPreviousPage();
			} else {
				return arquillianWizard.getNewArquillianJUnitTestCasePageOne();
			}
		}
		return super.getPreviousPage();
	}

	public String getMethodName() {
		return methodNameText.getText();
	}
	
	public String getArchiveType() {
		return archiveTypeCombo.getText();
	}
	
	public String getArchiveName() {
		return archiveNameText.getText();
	}

	public boolean addBeansXml() {
		return beansXmlButton.getSelection();
	}
}
