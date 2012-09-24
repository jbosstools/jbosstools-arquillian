package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageTwo;
import org.eclipse.jface.wizard.IWizard;

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
