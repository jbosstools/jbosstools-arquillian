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

import org.apache.maven.model.Dependency;
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
public class AddDependenciesWizardPage extends UserInputWizardPage {
	private AddDependenciesRefactoring refactoring;
	
	/**
	 * @param refactoring
	 */
	public AddDependenciesWizardPage(AddDependenciesRefactoring refactoring) {
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

		if (refactoring != null && refactoring.getDependencies() != null && refactoring.getDependencies().size() > 0) {
			if (refactoring.getDependencies().size() == 1) {
				new Label(composite, SWT.NONE).setText("The following dependency will be added:");
			} else {
				new Label(composite, SWT.NONE).setText("The following dependencies will be added:");
			}
			new Label(composite, SWT.NONE);
			
			for (Dependency dependency:refactoring.getDependencies()) {
				StringBuilder builder = new StringBuilder();
				builder.append(dependency.getGroupId() == null ? "" : dependency.getGroupId());
				builder.append(":");
				builder.append(dependency.getArtifactId() == null ? "" : dependency.getArtifactId());
				if (dependency.getVersion() != null) {
					builder.append(":");
					builder.append(dependency.getVersion());
				}
				new Label(composite, SWT.NONE).setText(builder.toString());
			}
		}
		
		setControl(composite);
	}

}
