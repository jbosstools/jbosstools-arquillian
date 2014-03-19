/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.editor.internal.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * 
 * @author snjeza
 *
 */
public class AddProfilesWizardPage extends UserInputWizardPage {
	private AddProfilesRefactoring refactoring;
	
	/**
	 * @param refactoring
	 */
	public AddProfilesWizardPage(AddProfilesRefactoring refactoring) {
		super(refactoring.getName());
		this.refactoring = refactoring;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;
		composite.setLayout(gridLayout);
		initializeDialogUnits(composite);
		Dialog.applyDialogFont(composite);

		if (refactoring != null && refactoring.getProfiles() != null && refactoring.getProfiles().size() > 0) {
			if (refactoring.getProfiles().size() == 1) {
				new Label(composite, SWT.NONE).setText("The following profile will be added:");
			} else {
				new Label(composite, SWT.NONE).setText("The following profilees will be added:");
			}
			new Label(composite, SWT.NONE);
			for (String profile:refactoring.getProfiles()) {
				new Label(composite, SWT.NONE).setText(profile == null ? "" : profile);
			}
			
		}
		
		setControl(composite);
	}

}
