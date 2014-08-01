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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.dialogs.AddArquillianProfilesDialog;

/**
 * 
 * @author snjeza
 * 
 */
public class AddArquillianProfilesCommandHandler extends ArquillianAbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = getProject(event);

		execute(project);
		return null;
	}

	public void execute(IProject project) {
		try {
			if (project == null || !project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				String message;
				if (project != null) {
					message = "The '" + project.getName() + "' project is not an Arquillian project";
				} else {
					message = "Invalid project";
				}
				MessageDialog.openInformation(
						shell,
						"Add Arquillian profiles",
						message);
				return;
			}
			AddArquillianProfilesDialog dialog = new AddArquillianProfilesDialog(getShell(), project);
			dialog.open();
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
	}

}
