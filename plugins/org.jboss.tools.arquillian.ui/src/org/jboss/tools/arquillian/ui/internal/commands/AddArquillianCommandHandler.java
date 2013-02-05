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
package org.jboss.tools.arquillian.ui.internal.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.preferences.ArquillianPreferencePage;

/**
 * 
 * @author snjeza
 *
 */
public class AddArquillianCommandHandler extends ArquillianAbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = getProject(event);
		try {
			if (project != null
					&& !project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
				AddArquillianNatureDialog dialog = new AddArquillianNatureDialog(getShell(), project);
				int ret = dialog.open();
				if (ret != Window.OK) {
					return null;
				}
				IProjectDescription description = project.getDescription();
				String[] prevNatures = description.getNatureIds();
				String[] newNatures = new String[prevNatures.length + 1];
				System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
				newNatures[prevNatures.length] = ArquillianNature.ARQUILLIAN_NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return null;
	}

	private Shell getShell() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		return shell;
	}

	private class AddArquillianNatureDialog extends MessageDialog {
		

		public AddArquillianNatureDialog(Shell parentShell, IProject project) {
			super(parentShell, "Add Arquillian Support", null,
					"Would you like to add the Arquillian Support to the '" + project.getName() + "' project?",
					MessageDialog.QUESTION, 
					new String[] { IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL }, 0);
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			new Label(parent, SWT.NONE);
			Link link = new Link(parent, SWT.NONE);
			link.setText("<a>Arquillian Settings</a>");
			GridData gd = new GridData(SWT.FILL, GridData.FILL, true, false);
			link.setLayoutData(gd);
			link.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Shell shell = AddArquillianCommandHandler.this.getShell();
					PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(getShell(), ArquillianPreferencePage.ID, null, null);
					preferenceDialog.open();
				}
				
			});
			new Label(parent, SWT.NONE);
			return link;
		}

	}

}
