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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
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
public class MavenResolverTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "mavenResolverTest";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/mavenResolverTest.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
		if (!ArquillianUtility.isValidatorEnabled(project)) {
			IEclipsePreferences prefs = new ProjectScope(project).getNode(ArquillianCoreActivator.PLUGIN_ID);
			prefs.putBoolean(ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR, true);
			prefs.flush();
		}
		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testMavenResolver() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/jboss/tools/arquillian/test/MavenResolverTest.java");
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_BASE_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("There is an Arquillian marker", projectMarkers.length == 0);
		IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
		boolean allowOSCommand = prefs.getBoolean(ArquillianConstants.ALLOW_OS_COMMAND);
		try {
			prefs.setValue(ArquillianConstants.ALLOW_OS_COMMAND, false);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
			resource = project.findMember("/src/test/java/org/jboss/tools/arquillian/test/MavenResolverTest.java");
			projectMarkers = resource.findMarkers(
					ArquillianConstants.MARKER_BASE_ID, true, IResource.DEPTH_INFINITE);
			assertTrue("There isn't an Arquillian marker", projectMarkers.length > 0);
		} finally {
			prefs.setValue(ArquillianConstants.ALLOW_OS_COMMAND, allowOSCommand);
		}
	}
	
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}
	
}
