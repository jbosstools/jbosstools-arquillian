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
import org.eclipse.ltk.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.wizards.CreateArquillianDeploymentMethodWizard;
import org.jboss.tools.arquillian.ui.internal.wizards.FixArchiveNameRefactoring;
import org.jboss.tools.arquillian.ui.internal.wizards.FixArchiveNameWizard;

/**
 * @see IMarkerResolution
 * 
 * @author Snjeza
 *
 */
public class FixArchiveNameMarkerResolution implements
		IMarkerResolution {
	
	private IMarker marker;

	public FixArchiveNameMarkerResolution(IMarker marker) {
		this.marker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Fix Archive Name";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker) {
		FixArchiveNameRefactoring refactoring = new FixArchiveNameRefactoring(marker);
		RefactoringWizard wizard = new FixArchiveNameWizard(refactoring);
		RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
		try {
			operation.run(getShell(), ""); //$NON-NLS-1$
		} catch (InterruptedException e) {
			ArquillianUIActivator.log(e);
		}
	}

	private Shell getShell() {
		if (Display.getCurrent() != null) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		return null;
	}
}
