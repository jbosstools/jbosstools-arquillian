/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.jboss.tools.arquillian.core.internal.archives.Entry;
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
	public void testEntries() {
		Entry entry = new Entry("02d7c314-bd82-467b-b309-7a574ca461fa.jar:");
		assertFalse("The entry is valid", entry.isValid());
		entry = new Entry("/META-INF/");
		assertTrue("The entry isn't valid", entry.isValid());
		assertTrue("The entry isn't a directory", entry.isDirectory());
		entry = new Entry("/org/arquillian/example/PhraseBuilder.class");
		assertTrue("The entry isn't valid", entry.isValid());
		assertFalse("The entry is a directory", entry.isDirectory());
		entry = new Entry("/PhraseBuilder.class");
		assertTrue("The entry is valid", entry.isValid());
		assertFalse("The entry is a directory", entry.isDirectory());
	}
	
	@Test
	public void testCreateArchive() throws JavaModelException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IJavaProject javaProject = JavaCore.create(project);
		IType type = javaProject.findType("org.arquillian.example.GreeterTest");
		List<Archive> archives = ArquillianSearchEngine.getDeploymentArchivesNew(type, true);
		assertTrue("Archives haven't been configured", archives.size() > 0);
		
	}
	
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
