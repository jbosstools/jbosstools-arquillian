/*************************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.maven.configurator;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianProjectConfigurator extends AbstractProjectConfigurator {

	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.getMavenProject();
		IProject project = request.getProject();
		configureInternal(mavenProject,project, monitor);
	}
	
	private void configureInternal(MavenProject mavenProject,IProject project,
			IProgressMonitor monitor) throws CoreException {
		if (!isArquilianConfigurable(mavenProject) || project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
			return;
		}
		
		if (project.hasNature(JavaCore.NATURE_ID) && isArquillianProject(mavenProject)) {
			ArquillianUtility.addArquillianNature(project);
		}
	}

	private boolean isArquilianConfigurable(MavenProject mavenProject) {
		String arquillianActivation = mavenProject.getProperties().getProperty("m2e.arquillian.activation");
		
		boolean configureArquillian; 
		if (arquillianActivation == null) {
			IPreferenceStore prefs = org.jboss.tools.maven.ui.Activator.getDefault().getPreferenceStore();
			configureArquillian = prefs.getBoolean(org.jboss.tools.maven.ui.Activator.CONFIGURE_ARQUILLIAN);
		} else {
		  configureArquillian = Boolean.valueOf(arquillianActivation);
		}
		return configureArquillian;
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event,
			IProgressMonitor monitor) throws CoreException {
		IMavenProjectFacade facade = event.getMavenProject();
	    if(facade != null) {
	      IProject project = facade.getProject();
	      MavenProject mavenProject = facade.getMavenProject(monitor);
	      configureInternal(mavenProject, project, monitor);
	    }
	}

	private boolean isArquillianProject(MavenProject mavenProject) {
		String version = ArquillianUtility.getArquillianVersion(mavenProject);
	    return version != null;
	}
}
