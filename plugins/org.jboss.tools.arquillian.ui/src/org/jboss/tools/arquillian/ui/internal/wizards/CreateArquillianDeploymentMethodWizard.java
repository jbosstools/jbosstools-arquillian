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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class CreateArquillianDeploymentMethodWizard extends Wizard implements
		INewWizard {

	private IJavaElement javaElement;
	private NewArquillianJUnitTestCaseDeploymentPage deploymentPage;
	
	public CreateArquillianDeploymentMethodWizard(IJavaElement javaElement) {
		super();
		this.javaElement = javaElement;
		setWindowTitle("New Arquillian Deployment Method");
		setDefaultPageImageDescriptor(ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/arquillian_icon64.png")); //$NON-NLS-1$
		//initDialogSettings();
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		
	}

	@Override
	public boolean performFinish() {
		if (deploymentPage.getType() != null) {
			ICompilationUnit icu = (ICompilationUnit) javaElement;
			try {
				ArquillianUIUtil.createDeploymentMethod(icu, deploymentPage.getType(), null, 
					deploymentPage.isAddComments(), deploymentPage.getDelimiter(),
					deploymentPage, 
					deploymentPage.getElementPosition(), deploymentPage.isForce());
				return true;
			} catch (Exception e) {
				String message = "Creation of element failed.\n\n" + e.getLocalizedMessage();
				MessageDialog.openError(getShell(), "Error", message);
				ArquillianUIActivator.log(e);
			}
		
		}
		return false;
	}

	/*
	 * @see Wizard#createPages
	 */
	@Override
	public void addPages() {
		super.addPages();
		deploymentPage = new NewArquillianJUnitTestCaseDeploymentPage(javaElement);
		addPage(deploymentPage);
	}
}
