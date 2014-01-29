/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.test;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class DeployableContainerTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "testDeployableContainer";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/testDeployableContainer.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
		if (!ArquillianUtility.isValidatorEnabled(project)) {
			IEclipsePreferences prefs = new ProjectScope(project).getNode(ArquillianCoreActivator.PLUGIN_ID);
			prefs.putBoolean(ArquillianConstants.TEST_ARQUILLIAN_CONTAINER, true);
			prefs.flush();
		}
		IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
		ResolverConfiguration configuration =configurationManager.getResolverConfiguration(project);
		
		configuration.setSelectedProfiles("JBOSS_AS_REMOTE_7.X, JBOSS_AS_MANAGED_7.X");
		boolean isSet = configurationManager.setResolverConfiguration(project, configuration);
		if (isSet) {
			MavenUpdateRequest request = new MavenUpdateRequest(project, true, true);
			configurationManager.updateProjectConfiguration(request, new NullProgressMonitor());
		}

		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testDeployableContainer() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IJavaProject javaProject = JavaCore.create(project);
		IStatus status = ArquillianSearchEngine.validateDeployableContainer(javaProject);
		assertTrue(status.getMessage(), !status.isOK());		
		IResource resource = project.findMember("/src/main/resources/META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension2");
		IPath destination = new Path("/testDeployableContainer/src/main/resources/META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension");
		resource.move(destination, true, null);
		status = ArquillianSearchEngine.validateDeployableContainer(javaProject);
		assertTrue(status.getMessage(), status.isOK());		
	}
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}
	
}
