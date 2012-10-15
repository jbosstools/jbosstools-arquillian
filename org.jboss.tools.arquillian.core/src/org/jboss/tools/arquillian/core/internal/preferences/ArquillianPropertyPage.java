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
package org.jboss.tools.arquillian.core.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
/**
 * 
 * @author snjeza
 *
 */
public class ArquillianPropertyPage extends PropertyPage {

    private Combo combo;

    private IJavaProject javaProject;

    @Override
    protected Control createContents(Composite parent) {
        javaProject = getJavaProject();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(2, false));

        Label label = new Label(composite, SWT.NONE);
        if (javaProject != null) {
        	label.setText("Select the severity level for the Arquillian validator");
        	combo = new Combo(composite, SWT.READ_ONLY);
        	combo.setItems(ArquillianConstants.SEVERITY_LEVELS);
        	String apiSeverity = ArquillianCoreActivator.getDefault().getArquillianSeverityLevel();
        	combo.setText(apiSeverity);
        } else {
        	label.setText("Cannot adapt the project to a Java project");
        	noDefaultAndApplyButton();
        }
        return composite;
    }

	private IJavaProject getJavaProject() {
		IAdaptable element = getElement();
		if (element instanceof IJavaProject) {
			return (IJavaProject)getElement();
		}
		if (element instanceof IProject) {
			IJavaProject project = JavaCore.create((IProject)element);
			if (project != null && project.exists()) {
				return project;
			}
		}
		IJavaProject project = (IJavaProject) element.getAdapter(IJavaProject.class);
		return project;
	}

    @Override
    protected void performApply() {
        saveValue();
        super.performApply();
    }

    private void saveValue() {
    	if (javaProject == null) {
    		return;
    	}
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_SEVERITY_LEVEL, combo.getText());
        rebuild();
    }

	private void rebuild() {
		try {
			ArquillianCoreActivator.getDefault().removeProjectLoader(javaProject.getProject());
            javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        } catch (CoreException e) {
            // ignore
        }
	}

    @Override
    protected void performDefaults() {
    	if (javaProject == null) {
    		return;
    	}
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_SEVERITY_LEVEL, ArquillianConstants.SEVERITY_WARNING);
        combo.setText(ArquillianConstants.SEVERITY_WARNING);
        rebuild();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        saveValue();
        return super.performOk();
    }

}
