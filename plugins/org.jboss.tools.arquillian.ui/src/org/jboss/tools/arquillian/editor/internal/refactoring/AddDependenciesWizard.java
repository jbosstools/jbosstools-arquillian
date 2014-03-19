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

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * @author snjeza
 *
 */
public class AddDependenciesWizard extends RefactoringWizard {

	private AddDependenciesRefactoring refactoring;

	/**
	 * @param refactoring
	 */
	public AddDependenciesWizard(AddDependenciesRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE|CHECK_INITIAL_CONDITIONS_ON_OPEN);
		this.refactoring = refactoring;
	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new AddDependenciesWizardPage(refactoring));
	}

}
