/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.markers.RefactoringUtil;
import org.jboss.tools.arquillian.ui.internal.refactoring.AddArquillianSupportRefactoring;
import org.jboss.tools.arquillian.ui.internal.refactoring.AddArquillianSupportWizard;

/**
 * 
 * @author snjeza
 *
 */
public class AddArquillianCommandHandler extends ArquillianAbstractHandler {
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = getProject(event);
		return execute(project);
	}

	public Object execute(IProject project) {
		try {
			if (project != null && !project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
				AddArquillianSupportRefactoring refactoring = new AddArquillianSupportRefactoring(project);
				RefactoringWizard wizard = new AddArquillianSupportWizard(refactoring);
				RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
				try {
					operation.run(RefactoringUtil.getShell(), ""); //$NON-NLS-1$
				} catch (InterruptedException e) {
					ArquillianUIActivator.log(e);
				}
			}
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return null;
	}

}
