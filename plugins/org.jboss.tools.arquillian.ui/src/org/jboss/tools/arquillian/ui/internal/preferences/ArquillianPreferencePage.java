/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.preferences;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.launcher.ArquillianLaunchConfigurationDelegate;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.jboss.tools.arquillian.ui.preferences.arquillianPreferencePage"; //$NON-NLS-1$
	private Combo combo;
	private CheckboxTableViewer profilesViewer;
	private List<Container> containers;
	private Text argumentsText;
	private Button enableButton;
	private Button addToJUnitTestNGButton;
	private Button addToExistingButton;
	
	private static final String[] defaultVersions = new String[] {ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT};
	
	@Override
	public void init(IWorkbench workbench) {
		containers = ContainerParser.getContainers();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        composite.setLayout(new GridLayout(2, false));
        
        Label label = new Label(composite, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        label.setLayoutData(gd);
        label.setText("Arquillian version:");
        combo = new Combo(composite, SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        combo.setLayoutData(gd);
        combo.setItems(ArquillianUtility.getVersions(defaultVersions));
        String value = ArquillianUtility.getPreference(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(value);
        
        Font font = parent.getFont();

		Group argumentsGroup = new Group(composite, SWT.NONE);
		argumentsGroup.setLayout(new GridLayout(1, false));
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
	    gd.horizontalSpan = 2;
	    argumentsGroup.setLayoutData(gd);
		argumentsGroup.setFont(font);
		argumentsGroup.setText("Default VM Arguments");
		
		Label argumentsLabel = new Label(argumentsGroup, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        argumentsLabel.setLayoutData(gd);
        argumentsLabel.setText("Select VM arguments you want to include in a new Arquillian launch configuration.");
        new Label(argumentsGroup, SWT.NONE);
		
        enableButton = new Button(argumentsGroup, SWT.CHECK);
        enableButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        enableButton.setText("Enable default VM arguments");
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        boolean enable = prefs.getBoolean(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS);
        enableButton.setSelection(enable);
        enableButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableWidgets();
			}
		
        });
        
		argumentsText = new Text(argumentsGroup, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.heightHint = 50;
		argumentsText.setLayoutData(gd);
		
		argumentsText.setText(prefs.getString(ArquillianConstants.DEFAULT_VM_ARGUMENTS));
		
		addToJUnitTestNGButton = new Button(argumentsGroup, SWT.CHECK);
        addToJUnitTestNGButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        addToJUnitTestNGButton.setText("Add the default VM arguments to the JUnit/TestNG launch configurations");
        
        addToJUnitTestNGButton.setSelection(prefs.getBoolean(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG));

        addToExistingButton = new Button(argumentsGroup, SWT.CHECK);
        addToExistingButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        addToExistingButton.setText("Add the default VM arguments to the existing launch configurations");
        
		Group profilesGroup = new Group(composite, SWT.NONE);
        profilesGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        gd.horizontalSpan = 2;
        profilesGroup.setLayoutData(gd);
        profilesGroup.setText("Profiles");
        Label profilesLabel = new Label(profilesGroup, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        profilesLabel.setLayoutData(gd);
        profilesLabel.setText("Select profiles you want to include automatically when adding the Arquillian support.");
        new Label(profilesGroup, SWT.NONE);
        profilesViewer = ArquillianUIUtil.createProfilesViewer(profilesGroup, containers, 200);
        ArquillianUIUtil.initializeViewer(profilesViewer, containers);
		enableWidgets();
        return composite;
	}

	private void enableWidgets() {
		boolean enabled = enableButton.getSelection();
		argumentsText.setEditable(enabled);
		addToJUnitTestNGButton.setEnabled(enabled);
		addToExistingButton.setEnabled(enabled);
	}
	
	@Override
    protected void performDefaults() {
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        
        prefs.setValue(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS, ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS_VALUE);
        enableButton.setSelection(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS_VALUE);
        
        prefs.setValue(ArquillianConstants.DEFAULT_VM_ARGUMENTS, ArquillianConstants.DEFAULT_VM_ARGUMENTS_VALUE);
        argumentsText.setText(ArquillianConstants.DEFAULT_VM_ARGUMENTS_VALUE);
        
        prefs.setValue(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG, ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG_VALUE);
        addToJUnitTestNGButton.setSelection(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG_VALUE);
        
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        prefs.setValue(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		prefs.setValue(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		ArquillianUIUtil.initializeViewer(profilesViewer, containers);
		profilesViewer.refresh();
		enableWidgets();
		addArgumentsInternal(prefs);
        super.performDefaults();
    }

	public void addArgumentsInternal(IPreferenceStore prefs) {
		try {
			addArguments(prefs);
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
			MessageDialog.openError(getShell(), "Error", e.getMessage());
		}
	}

    @Override
    public boolean performOk() {
    	IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, combo.getText());
        prefs.setValue(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS, enableButton.getSelection());
        prefs.setValue(ArquillianConstants.DEFAULT_VM_ARGUMENTS, argumentsText.getText());
        prefs.setValue(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG, addToJUnitTestNGButton.getSelection());
		StringBuilder aBuilder = new StringBuilder();
		StringBuilder sBuilder = new StringBuilder();
		for (Iterator<Container> iterator = containers.iterator(); iterator.hasNext();) {
			Container container = (Container) iterator.next();
			if (container.isActivate()) {
				if (aBuilder.length() > 0) {
					aBuilder.append(ArquillianConstants.COMMA);
				}
				aBuilder.append(container.getId());
			}
			if (profilesViewer.getChecked(container)) {
				if (sBuilder.length() > 0) {
					sBuilder.append(ArquillianConstants.COMMA);
				}
				sBuilder.append(container.getId());
			}
		}
		prefs.setValue(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, sBuilder.toString());
		prefs.setValue(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, aBuilder.toString());
        addArgumentsInternal(prefs);
		return super.performOk();
    }

	private void addArguments(IPreferenceStore prefs) throws CoreException {
		String arguments = prefs.getString(ArquillianConstants.DEFAULT_VM_ARGUMENTS);
		if (arguments == null || arguments.isEmpty() ) {
			return;
		}
		arguments = arguments.trim();
		if (addToExistingButton.getSelection()) {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			addArguments(arguments, manager, ArquillianLaunchConfigurationDelegate.ID);
			if (addToJUnitTestNGButton.getSelection()) {
				addArguments(arguments, manager, ArquillianConstants.JUNIT_LAUNCHCONFIG_TYPE_ID);
				addArguments(arguments, manager, ArquillianConstants.TESTNG_LAUNCHCONFIG_TYPE_ID);
			}
		}
	}

	public void addArguments(String arguments, ILaunchManager manager, String typeId)
			throws CoreException {
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(typeId);
		ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
		for (ILaunchConfiguration configuration:configurations) {
			ArquillianUtility.addArguments(configuration, arguments, true);
		}
	}

}
