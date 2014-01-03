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
package org.jboss.tools.arquillian.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.archives.Archive;
import org.jboss.tools.arquillian.core.internal.archives.ArchiveLocation;
import org.jboss.tools.arquillian.core.internal.archives.IEntry;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 * 
 */
public class ArchivesTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "archives";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/archives.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.addArquillianNature(project, true);
		JobUtils.waitForIdle();
		activateProfile();
		JobUtils.waitForIdle();
	}

	private static void activateProfile() throws CoreException {
		final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

		IProject project = getProject(TEST_PROJECT_NAME);
		
		final ResolverConfiguration configuration =configurationManager.getResolverConfiguration(project);

		final String profilesAsString = ArquillianConstants.JBOSS_AS_REMOTE_7_X;
		if (profilesAsString.equals(configuration.getActiveProfiles())) {
			return;
		}
		
		configuration.setActiveProfiles(profilesAsString);
		boolean isSet = configurationManager.setResolverConfiguration(project, configuration);
		if (isSet) {
			MavenUpdateRequest request = new MavenUpdateRequest(project, true, false);
			configurationManager.updateProjectConfiguration(request, new NullProgressMonitor());
		}
	}

	@Test
	public void testEntries() throws IOException {
		IProject project = getProject(TEST_PROJECT_NAME);
		InputStream is = null;
		try {
			Bundle bundle = Platform.getBundle(ArquillianTestActivator.PLUGIN_ID);
			URL url = bundle.getEntry("/projects/archive.xml");
			is = url.openStream();
			String description = IOUtil.toString(is);
			ArchiveLocation location = new ArchiveLocation(project.getName(), "org.arquillian.eclipse.DependentClassTest", "createDeployment");
			IJavaProject javaProject = JavaCore.create(project);
			Archive archive = new Archive(description, location, javaProject );
			Set<IEntry> entries = archive.getChildren();
			assertEquals(entries.size(), 2);
			assertTrue(archive.getFullyQuallifiedNames().contains("org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1"));
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		
	}
	
	@Test
	public void testCreateArchive() throws JavaModelException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IJavaProject javaProject = JavaCore.create(project);
		IType type = javaProject.findType("org.arquillian.example.GreeterTest");
		List<Archive> archives = ArquillianSearchEngine.getDeploymentArchives(type, true);
		assertTrue("Archives haven't been configured", archives.size() > 0);
		
	}
	
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
