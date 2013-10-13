/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
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
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * @author snjeza
 *
 */
public class FixArchiveNameWizardPage extends UserInputWizardPage {
	private FixArchiveNameRefactoring refactoring;
	private Text newArchiveNameText;

	public FixArchiveNameWizardPage(FixArchiveNameRefactoring refactoring) {
		super(refactoring.getName());
		this.refactoring = refactoring;
	}

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
		label.setText("New Archive name:");

		newArchiveNameText = new Text(composite, SWT.BORDER);
		newArchiveNameText.setText(refactoring.getNewArchiveName());
		newArchiveNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		newArchiveNameText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				refactoring.setNewArchiveName(newArchiveNameText.getText());
				validate();
			}
		});

		setControl(composite);
		validate();
	}

	private void validate() {
		String txt = newArchiveNameText.getText();
		setPageComplete(txt.length() > 0 && !txt.equals(refactoring.getOldArchiveName()) && txt.trim().length() > 4 && txt.endsWith(refactoring.getExtension()));
	}
}
