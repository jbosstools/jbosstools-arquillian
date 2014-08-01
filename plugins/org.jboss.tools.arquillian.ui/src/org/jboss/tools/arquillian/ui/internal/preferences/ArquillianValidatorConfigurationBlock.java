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
package org.jboss.tools.arquillian.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianValidatorConfigurationBlock extends OptionsConfigurationBlock {

	private static final String SETTINGS_SECTION_NAME= "ArquillianValidatorConfigurationBlock";  //$NON-NLS-1$

	private static final Key MISSING_DEPLOYMENT_METHOD = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.MISSING_DEPLOYMENT_METHOD);
	private static final Key INVALID_ARCHIVE_NAME = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.INVALID_ARCHIVE_NAME);
	
	private static final Key MISSING_TEST_METHOD = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.MISSING_TEST_METHOD);
	private static final Key TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT);
	private static final Key IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT);
	private static final Key DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED);
	private static final Key ENABLE_ARQUILLIAN_VALIDATOR = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR);
	private static final Key TEST_ARQUILLIAN_CONTAINER = getKey(ArquillianCoreActivator.PLUGIN_ID, ArquillianConstants.TEST_ARQUILLIAN_CONTAINER);
	
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private PixelConverter fPixelConverter;

	private FilteredPreferenceTree fFilteredPrefTree;

	private Button enableValidation;

	public ArquillianValidatorConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);
		
	}

	public static Key[] getKeys() {
		return new Key[] {
				MISSING_DEPLOYMENT_METHOD,
				INVALID_ARCHIVE_NAME,
				MISSING_TEST_METHOD,
				TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT,
				IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT,
				DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED,
				ENABLE_ARQUILLIAN_VALIDATOR,
				TEST_ARQUILLIAN_CONTAINER
			};
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);

		String[] enabledDisabled= new String[] { "true", "false" };
		
		addCheckBox(mainComp, "Test Arquillian Container", TEST_ARQUILLIAN_CONTAINER, enabledDisabled, 0);
		
		enableValidation = addCheckBox(mainComp, "Enable Validation", ENABLE_ARQUILLIAN_VALIDATOR, enabledDisabled, 0);
		enableValidation.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnableStates();
			}
		
		});
		
		createIgnoreOptionalProblemsLink(mainComp);
		
		Composite spacer= new Composite(mainComp, SWT.NONE);
		spacer.setLayoutData(new GridData(0, 0));
		
		Composite commonComposite= createStyleTabContent(mainComp);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(30);
		commonComposite.setLayoutData(gridData);

		validateSettings(null, null, null);

		setEnableStates();
		
		return mainComp;
	}

	private Composite createStyleTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		String[] errorWarningIgnoreLabels= new String[] {
			ArquillianConstants.SEVERITY_ERROR,
			ArquillianConstants.SEVERITY_WARNING,
			ArquillianConstants.SEVERITY_IGNORE
		};

		String[] errorWarning= new String[] { ERROR, WARNING };
		String[] errorWarningLabels= new String[] {
				ArquillianConstants.SEVERITY_ERROR,
				ArquillianConstants.SEVERITY_WARNING,
		};
		
		fFilteredPrefTree= new FilteredPreferenceTree(this, folder, "&Select the severity level for the following optional problems:");
		final ScrolledPageContent sc1= fFilteredPrefTree.getScrolledPageContent();
		
		int nColumns= 3;

		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		int indentStep=  fPixelConverter.convertWidthInCharsToPixels(1);

		int defaultIndent= indentStep * 0;
		int extraIndent= indentStep * 3;
		String label;
		ExpandableComposite excomposite;
		Composite inner;
		PreferenceTreeNode section;
		PreferenceTreeNode node;
		Key twistieKey;

		label= "Validation";
		twistieKey= OptionsConfigurationBlock.getLocalKey("ArquillianValidatorConfigurationBlock_potential_problems"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);
		
		inner= createInnerComposite(excomposite, nColumns, composite.getFont());
		
		label= "Missing @Deployment method";
		fFilteredPrefTree.addComboBox(inner, label, MISSING_DEPLOYMENT_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label= "Missing @Test method";
		fFilteredPrefTree.addComboBox(inner, label, MISSING_TEST_METHOD, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label = "Type is not included in any deployment";
		fFilteredPrefTree.addComboBox(inner, label, TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label = "Import is not included in any deployment";
		fFilteredPrefTree.addComboBox(inner, label, IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label = "Deployment archive cannot be created";
		fFilteredPrefTree.addComboBox(inner, label, DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		label = "Invalid archive name";
		fFilteredPrefTree.addComboBox(inner, label, INVALID_ARCHIVE_NAME, errorWarningIgnore, errorWarningIgnoreLabels, defaultIndent, section);
		
		IDialogSettings settingsSection= ArquillianUIActivator.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(settingsSection);

		return sc1;
	}

	private Composite createInnerComposite(ExpandableComposite excomposite, int nColumns, Font font) {
		Composite inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(font);
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		return inner;
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */
	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// FIXME
	}

	private static boolean lessSevere(String errorWarningIgnore, String errorWarningIgnore2) {
		if (IGNORE.equals(errorWarningIgnore))
			return ! IGNORE.equals(errorWarningIgnore2);
		else if (WARNING.equals(errorWarningIgnore))
			return ERROR.equals(errorWarningIgnore2);
		else
			return false;
	}
	
	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= "Arquillian Validator Settings Changed";
		String message;
		if (workspaceSettings) {
			message= "The Arquillian Validator settings have changed. A full rebuild is required for changes to take effect. Do the full build now?";
		} else {
			message= "The Arquillian Validator settings have changed. A rebuild of the project is required for changes to take effect. Build the project now?";
		}
		return new String[] { title, message };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#dispose()
	 */
	@Override
	public void dispose() {
		IDialogSettings section= ArquillianUIActivator.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(section);
		super.dispose();
	}

	private void setEnableStates() {
		boolean enabled = enableValidation.getSelection();
		setComboEnabled(MISSING_DEPLOYMENT_METHOD, enabled);
		setComboEnabled(INVALID_ARCHIVE_NAME, enabled);
		setComboEnabled(MISSING_TEST_METHOD, enabled);
		setComboEnabled(TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, enabled);
		setComboEnabled(IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, enabled);
		setComboEnabled(DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED, enabled);
	}

}
