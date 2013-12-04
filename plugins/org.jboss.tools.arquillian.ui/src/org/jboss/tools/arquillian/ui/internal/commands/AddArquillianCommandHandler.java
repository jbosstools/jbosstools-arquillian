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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.preferences.ArquillianPreferencePage;

/**
 * 
 * @author snjeza
 *
 */
public class AddArquillianCommandHandler extends ArquillianAbstractHandler {

	private boolean updatePom;
	private static final String UPDATE_POM = "updatePom"; //$NON-NLS-1$
	private static final String ADD_ARQUILLIAN_SUPPORT_SECTION = "addArquillianSupportSection"; //$NON-NLS-1$
	private IDialogSettings dialogSettings;
	private IDialogSettings addArquillianSupportSection;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = getProject(event);
		return execute(project);
	}

	public Object execute(IProject project) {
		try {
			if (project != null
					&& !project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
				AddArquillianNatureDialog dialog = new AddArquillianNatureDialog(getShell(), project);
				int ret = dialog.open();
				if (ret != Window.OK) {
					return null;
				}
				ArquillianUtility.addArquillianNature(project, updatePom);
				if (addArquillianSupportSection != null) {
					addArquillianSupportSection.put(UPDATE_POM, updatePom);
				}
			}
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return null;
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
			final Button updatePomButton = new Button(parent, SWT.CHECK);
			updatePomButton.setText("Update the pom.xml file");
			dialogSettings = ArquillianUIActivator.getDefault().getDialogSettings();
			addArquillianSupportSection = dialogSettings.getSection(ADD_ARQUILLIAN_SUPPORT_SECTION);
			if (addArquillianSupportSection == null) {
				addArquillianSupportSection = dialogSettings.addNewSection(ADD_ARQUILLIAN_SUPPORT_SECTION);	
			}
			String value = addArquillianSupportSection.get(UPDATE_POM);
			if (value == null) {
				updatePom = true;
			} else {
				updatePom = addArquillianSupportSection.getBoolean(UPDATE_POM);
			}
			updatePomButton.setSelection(updatePom);
			updatePomButton.addSelectionListener(new SelectionAdapter() {
			
				@Override
				public void widgetSelected(SelectionEvent e) {
					updatePom = updatePomButton.getSelection();
				}
			
			});

			Link link = new Link(parent, SWT.NONE);
			link.setText("<a>Arquillian Settings</a>");
			GridData gd = new GridData(SWT.FILL, GridData.FILL, true, false);
			link.setLayoutData(gd);
			link.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(getShell(), ArquillianPreferencePage.ID, null, null);
					preferenceDialog.open();
				}
				
			});
			new Label(parent, SWT.NONE);
			return link;
		}

	}

}
