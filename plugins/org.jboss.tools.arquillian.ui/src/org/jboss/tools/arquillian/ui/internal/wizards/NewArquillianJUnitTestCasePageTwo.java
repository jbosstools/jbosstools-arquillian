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

import org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageTwo;
import org.eclipse.jface.wizard.IWizard;

/**
 * 
 * @author snjeza
 *
 */
public class NewArquillianJUnitTestCasePageTwo extends
		NewTestCaseWizardPageTwo {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
	 */
	@Override
	public boolean canFlipToNextPage() {
		if (!super.canFlipToNextPage()) {
			return false;
		}
		IWizard wizard = getWizard();
		if (wizard instanceof NewArquillianJUnitTestWizard) {
			NewArquillianJUnitTestWizard arquillianWizard = (NewArquillianJUnitTestWizard) wizard;
			NewArquillianJUnitTestCasePageOne pageOne = arquillianWizard.getNewArquillianJUnitTestCasePageOne();
			if (pageOne.isGenerateDeploymentMethod()) {
				return true;
			}
		}
		return false;
	}

}
