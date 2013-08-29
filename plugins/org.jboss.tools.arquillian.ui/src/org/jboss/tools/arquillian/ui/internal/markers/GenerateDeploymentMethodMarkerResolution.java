
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
package org.jboss.tools.arquillian.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.ui.internal.wizards.CreateArquillianDeploymentMethodWizard;

/**
 * @see IMarkerResolution
 * 
 * @author Snjeza
 *
 */
public class GenerateDeploymentMethodMarkerResolution implements
		IMarkerResolution {
	
	private IJavaElement javaElement;

	public GenerateDeploymentMethodMarkerResolution(IJavaElement element) {
		javaElement = element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Generate Arquillian Deployment Method";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker) {
		WizardDialog dialog = new WizardDialog(getShell(), new CreateArquillianDeploymentMethodWizard(javaElement));
		dialog.open();
	}

	private Shell getShell() {
		if (Display.getCurrent() != null) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		return null;
	}
}
