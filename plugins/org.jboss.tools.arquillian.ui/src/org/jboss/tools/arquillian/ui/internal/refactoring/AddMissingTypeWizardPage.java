/*************************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class AddMissingTypeWizardPage extends UserInputWizardPage {
	private static final String ADD_ALL_CLASSES = "addAllClasses"; //$NON-NLS-1$
	private static final String MISSING_TYPE_SECTION = "missingTypeSection"; //$NON-NLS-1$
	private AddMissingTypeRefactoring refactoring;
	private Combo deploymentMethodsCombo;
	private IDialogSettings dialogSettings;
	private IDialogSettings section;
	private Button addAllClassesButton;
	
	/**
	 * @param refactoring
	 */
	public AddMissingTypeWizardPage(AddMissingTypeRefactoring refactoring) {
		super(refactoring.getName());
		this.refactoring = refactoring;
		dialogSettings = ArquillianUIActivator.getDefault().getDialogSettings();
		section = dialogSettings.getSection(MISSING_TYPE_SECTION);
		if (section == null) {
			section = dialogSettings.addNewSection(MISSING_TYPE_SECTION);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;
		composite.setLayout(gridLayout);
		initializeDialogUnits(composite);
		Dialog.applyDialogFont(composite);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Deployment method:");

		deploymentMethodsCombo = new Combo(composite, SWT.NONE);
		deploymentMethodsCombo.setItems(refactoring.getDeploymentMethodsNames());
		deploymentMethodsCombo.select(0);
		deploymentMethodsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deploymentMethodsCombo.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				refactoring.setDeploymentMethod(deploymentMethodsCombo.getText());
				validate();
			}
		});

		addAllClassesButton = new Button(composite, SWT.CHECK);
		addAllClassesButton.setText("Add dependent classes");
		addAllClassesButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		boolean addAllClasses = section.getBoolean(ADD_ALL_CLASSES);
		addAllClassesButton.setSelection(addAllClasses);
		refactoring.setAddAllDependentClasses(addAllClasses);
		addAllClassesButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refactoring.setAddAllDependentClasses(addAllClassesButton.getSelection());
			}
		
		});
		setControl(composite);
		validate();
	}

	private void validate() {
		// FIXME
	}

	@Override
	protected boolean performFinish() {
		section.put(ADD_ALL_CLASSES, addAllClassesButton.getSelection());
		return super.performFinish();
	}
}
