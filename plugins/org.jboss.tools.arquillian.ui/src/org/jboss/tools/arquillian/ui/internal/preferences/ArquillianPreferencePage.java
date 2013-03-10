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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.preferences.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
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
	
	private static final String[] defaultVersions = new String[] {ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT};
	
	private static final String COORDS = ArquillianUtility.ARQUILLIAN_GROUP_ID + ":" + ArquillianUtility.ARQUILLIAN_BOM_ARTIFACT_ID + ":[0,)";  //$NON-NLS-1$ //$NON-NLS-2$
	
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
        combo.setItems(ArquillianUtility.getVersions(COORDS, defaultVersions));
        String value = ArquillianUtility.getPreference(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(value);
        
        Group profilesGroup = new Group(composite, SWT.NONE);
        profilesGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        gd.horizontalSpan = 2;
        
        profilesGroup.setLayoutData(gd);
        profilesGroup.setText("Profiles");
        Label profilesLabel = new Label(profilesGroup, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        profilesLabel.setLayoutData(gd);
        profilesLabel.setText("Select those profiles that you want to include automatically when adding the Arquillian support.");
        new Label(profilesGroup, SWT.NONE);
        profilesViewer = ArquillianUIUtil.createProfilesViewer(profilesGroup, containers, 300);
        ArquillianUIUtil.initializeViewer(profilesViewer, containers);
		return composite;
	}

	@Override
    protected void performDefaults() {
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        prefs.setValue(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		prefs.setValue(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		ArquillianUIUtil.initializeViewer(profilesViewer, containers);
		profilesViewer.refresh();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
    	IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, combo.getText());
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
        return super.performOk();
    }

}
