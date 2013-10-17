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
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.refactoring.AddMissingTypeRefactoring;
import org.jboss.tools.arquillian.ui.internal.refactoring.AddMissingTypeWizard;

/**
 * @see IMarkerResolution
 * 
 * @author Snjeza
 *
 */
public class AddMissingTypeMarkerResolution implements
		IMarkerResolution {
	
	private String name;

	public AddMissingTypeMarkerResolution(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker) {
		AddMissingTypeRefactoring refactoring = new AddMissingTypeRefactoring(marker);
		RefactoringWizard wizard = new AddMissingTypeWizard(refactoring);
		RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
		try {
			operation.run(RefactoringUtil.getShell(), ""); //$NON-NLS-1$
		} catch (InterruptedException e) {
			ArquillianUIActivator.log(e);
		}
	}

}
