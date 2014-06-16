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
package org.jboss.tools.arquillian.ui.internal.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * @author snjeza
 *
 */
public class AddArquillianSupportWizard extends RefactoringWizard {

	private AddArquillianSupportRefactoring refactoring;

	/**
	 * @param refactoring
	 */
	public AddArquillianSupportWizard(AddArquillianSupportRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE|CHECK_INITIAL_CONDITIONS_ON_OPEN);
		this.refactoring = refactoring;
	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new AddArquillianSupportWizardPage(refactoring));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		boolean ret = super.performFinish();
		if (!ret) {
			return ret;
		}
		try {
			ArquillianUtility.addArquillianNature(refactoring.getProject());
			ArquillianUtility.updateProject(refactoring.getProject());
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return ret;
	}

}
