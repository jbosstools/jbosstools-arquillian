/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.refactoring;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.preferences.ArquillianPreferencePage;

/**
 * 
 * @author snjeza
 *
 */
public class AddArquillianSupportWizardPage extends UserInputWizardPage {
	private static final String[] defaultVersions = new String[] {ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT};
	private AddArquillianSupportRefactoring refactoring;
	private Combo versionCombo;
	private Button updatePomButton;
	private Button addProfilesButton;
	private static final String UPDATE_POM = "updatePom"; //$NON-NLS-1$
	private static final String ADD_PROFILES = "addProfiles"; //$NON-NLS-1$
	private static final String UPDATE_BUILD = "updateBuild"; //$NON-NLS-1$
	private static final String UPDATE_DEPENDENCIES = "updateDependencies"; //$NON-NLS-1$
	private static final String ADD_ARQUILLIAN_SUPPORT_SECTION = "addArquillianSupportSection"; //$NON-NLS-1$
	private IDialogSettings dialogSettings;
	private IDialogSettings addArquillianSupportSection;
	private Button updateBuildButton;
	private Button updateDependenciesButton;
	
	/**
	 * @param refactoring
	 */
	public AddArquillianSupportWizardPage(AddArquillianSupportRefactoring refactoring) {
		super(refactoring.getName());
		this.refactoring = refactoring;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;
		composite.setLayout(gridLayout);
		initializeDialogUnits(composite);
		Dialog.applyDialogFont(composite);

		Link link = new Link(composite, SWT.NONE);
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
		
		updatePomButton = new Button(composite, SWT.CHECK);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.horizontalSpan = 2;
		updatePomButton.setText("Update the pom.xml file");
		updatePomButton.setLayoutData(gd);

		dialogSettings = ArquillianUIActivator.getDefault().getDialogSettings();
		addArquillianSupportSection = dialogSettings.getSection(ADD_ARQUILLIAN_SUPPORT_SECTION);
		if (addArquillianSupportSection == null) {
			addArquillianSupportSection = dialogSettings.addNewSection(ADD_ARQUILLIAN_SUPPORT_SECTION);	
		}
		String value = addArquillianSupportSection.get(UPDATE_POM);
		boolean updatePom;
		if (value == null) {
			updatePom = true;
		} else {
			updatePom = addArquillianSupportSection.getBoolean(UPDATE_POM);
		}
		updatePomButton.setSelection(updatePom);

		updateDependenciesButton = new Button(composite, SWT.CHECK);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.horizontalSpan = 2;
		updateDependenciesButton.setText("Update the dependencies section");
		updateDependenciesButton.setLayoutData(gd);

		value = addArquillianSupportSection.get(UPDATE_DEPENDENCIES);
		boolean updateDependencies;
		if (value == null) {
			updateDependencies = true;
		} else {
			updateDependencies = addArquillianSupportSection.getBoolean(UPDATE_DEPENDENCIES);
		}
		updateDependenciesButton.setSelection(updateDependencies);

		updateDependenciesButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateDependencies(updateDependenciesButton.getSelection());
				validate();
			}
			
		});

		updateBuildButton = new Button(composite, SWT.CHECK);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.horizontalSpan = 2;
		updateBuildButton.setText("Update the build section");
		updateBuildButton.setLayoutData(gd);

		value = addArquillianSupportSection.get(UPDATE_BUILD);
		boolean updateBuild;
		if (value == null) {
			updateBuild = true;
		} else {
			updateBuild = addArquillianSupportSection.getBoolean(UPDATE_BUILD);
		}
		updateBuildButton.setSelection(updateBuild);

		updateBuildButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refactoring.setUpdateBuild(updateBuildButton.getSelection());
				validate();
			}
			
		});
		
		addProfilesButton = new Button(composite, SWT.CHECK);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.horizontalSpan = 2;
		addProfilesButton.setText("Add Profiles");
		addProfilesButton.setLayoutData(gd);

		value = addArquillianSupportSection.get(ADD_PROFILES);
		boolean addProfiles;
		if (value == null) {
			addProfiles = true;
		} else {
			addProfiles = addArquillianSupportSection.getBoolean(ADD_PROFILES);
		}
		addProfilesButton.setSelection(addProfiles);

		addProfilesButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refactoring.setAddProfiles(addProfilesButton.getSelection());
				validate();
			}
			
		});
		
		Label label = new Label(composite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		label.setLayoutData(gd);
		label.setText("Arquillian version:");
		versionCombo = new Combo(composite, SWT.READ_ONLY);
		gd = new GridData(SWT.FILL, SWT.FILL,false,false);
		versionCombo.setLayoutData(gd);
		versionCombo.setItems(ArquillianUtility.getVersions(defaultVersions));
		value = ArquillianUtility.getPreference(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
		versionCombo.setText(value);
		refactoring.setVersion(value);
		versionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		versionCombo.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				refactoring.setVersion(versionCombo.getText());
				validate();
			}
		});

		updatePomButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updatePomChanged();
			}
		
		});
		String message = null;
		try {
			IProject project = refactoring.getProject();
			IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
			if (facade != null) {
				MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
				String version = ArquillianUtility.getArquillianVersion(mavenProject);
				if (version != null) {
					updatePomButton.setSelection(false);
					updatePomButton.setEnabled(false);
					message = "The project already includes Arquillian settings";
				}
			} else {
				updatePomButton.setSelection(false);
				updatePomButton.setEnabled(false);
				message = "The project is not a valid maven project";
			}
		} catch (CoreException e1) {
			updatePomButton.setSelection(false);
			updatePomButton.setEnabled(false);
			message = "Some issues encountered.\nCaused by: " + e1.getLocalizedMessage();
		}
		if (message != null) {
			Composite warningComposite = new Composite(composite, SWT.NONE);
			gd = new GridData(SWT.FILL, SWT.FILL,true,false);
			gd.horizontalSpan = 2;
			warningComposite.setLayoutData(gd);
			warningComposite.setLayout(new GridLayout(2, false));
			Label emptyLabel = new Label(warningComposite, SWT.NONE);
			gd = new GridData(SWT.FILL, SWT.FILL,true,true);
			gd.horizontalSpan = 2;
			emptyLabel.setLayoutData(gd);
			Label warningImage = new Label(warningComposite, SWT.NONE);
			gd = new GridData(SWT.FILL, SWT.LEFT,false,false);
			warningImage.setLayoutData(gd);
			warningImage.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
			Label warningText = new Label(warningComposite, SWT.NONE);
			gd = new GridData(SWT.FILL, SWT.FILL,true,false);
			warningText.setLayoutData(gd);
			warningText.setText(message);
		}
		
		updatePomChanged();
		setControl(composite);
		validate();
	}

	private void updatePomChanged() {
		boolean enabled = updatePomButton.getSelection();
		versionCombo.setEnabled(enabled);
		addProfilesButton.setEnabled(enabled);
		updateBuildButton.setEnabled(enabled);
		updateDependenciesButton.setEnabled(enabled);
		refactoring.setUpdatePom(enabled);
		refactoring.setVersion(versionCombo.getText());
		refactoring.setAddProfiles(enabled && addProfilesButton.getSelection());
		refactoring.setUpdateBuild(updateBuildButton.getSelection());
		refactoring.setUpdateDependencies(updateDependenciesButton.getSelection());
		
	}

	private void validate() {
		
	}

	@Override
	protected boolean performFinish() {
		addArquillianSupportSection.put(UPDATE_POM, updatePomButton.getSelection());
		addArquillianSupportSection.put(ADD_PROFILES, addProfilesButton.getSelection());
		addArquillianSupportSection.put(UPDATE_BUILD, updateBuildButton.getSelection());
		addArquillianSupportSection.put(UPDATE_DEPENDENCIES, updateDependenciesButton.getSelection());
		return super.performFinish();
	}
}
