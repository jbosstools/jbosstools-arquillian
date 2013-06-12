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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
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
public class ArquillianValidatorTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "arquillian-plugin-test";
	
	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/arquillian-plugin-test.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.addArquillianNature(project);
		JobUtils.waitForIdle();
		if (!ArquillianUtility.isValidatorEnabled(project)) {
			IEclipsePreferences prefs = new ProjectScope(project).getNode(ArquillianCoreActivator.PLUGIN_ID);
			prefs.putBoolean(ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR, true);
			prefs.flush();
		}
	}

	@Test
	public void testArquillianMarkers() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/arquillian/eclipse/Test2.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_CLASS_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian markers aren't created", projectMarkers.length > 0);
	}

	@Test
	public void testMaliciousCode() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/arquillian/eclipse/TestSystemExit.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_RESOURCE_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian markers aren't created", projectMarkers.length == 1);
	}
	
	@Test
	public void testMissingClass() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/arquillian/eclipse/ManagerTest.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_CLASS_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("There are arquillian markers", projectMarkers.length == 0);
		projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_RESOURCE_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("There are arquillian markers", projectMarkers.length == 0);
	}
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
