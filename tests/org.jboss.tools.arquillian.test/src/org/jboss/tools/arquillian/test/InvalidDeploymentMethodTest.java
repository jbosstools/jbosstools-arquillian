/*************************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.internal.markers.FixArchiveFileLocationMarkerResolution;
import org.jboss.tools.arquillian.ui.internal.markers.FixInvalidDeploymentMethodMarkerResolution;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class InvalidDeploymentMethodTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "invalidDeploymentMethod";
	
	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/invalidDeploymentMethod.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		addArquillianSupport(project);
		JobUtils.waitForIdle();
		if (!ArquillianUtility.isValidatorEnabled(project)) {
			IEclipsePreferences prefs = new ProjectScope(project).getNode(ArquillianCoreActivator.PLUGIN_ID);
			prefs.putBoolean(ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR, true);
			prefs.flush();
		}
	}

	@Test
	public void testDeploymentMethod() throws CoreException, IOException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/jboss/tools/arquillian/test/InvalidDeploymentMethodTest.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_INVALID_DEPLOYMENT_METHOD_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian markers aren't created", projectMarkers.length > 0);
		
		FixInvalidDeploymentMethodMarkerResolution resolution = new FixInvalidDeploymentMethodMarkerResolution(projectMarkers[0], true);
		resolution.run(projectMarkers[0]);
		
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		JobUtils.waitForIdle(1000);
		projectMarkers = resource.findMarkers(ArquillianConstants.MARKER_INVALID_DEPLOYMENT_METHOD_ID, true,
				IResource.DEPTH_INFINITE);
		assertTrue("An invalid marker is created.", projectMarkers.length == 0);
	}
	
	@Test
	public void testEar() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/EarArchiveFileLocationTest.java");
	}
	
	@Test
	public void testBeansXml() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/BeansXmlFileLocationTest.java");
	}

	@Test
	public void testBeansXml1() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/BeansXml1FileLocationTest.java");
	}

	@Test
	public void testBeansXml2() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/BeansXml2FileLocationTest.java");
	}
	
	@Test
	public void testWebXml() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/WebXmlFileLocationTest.java");
	}
	
	@Test
	public void testWebXml1() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/WebXml1FileLocationTest.java");
	}
	
	@Test
	public void testWebXml2() throws CoreException, IOException {
		internalTest("/src/test/java/org/jboss/tools/arquillian/test/WebXml2FileLocationTest.java");
	}
	
	private void internalTest(String resourceName) throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember(resourceName);
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		JobUtils.waitForIdle(1000);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_INVALID_ARCHIVE_FILE_LOCATION_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian markers aren't created", projectMarkers.length > 0);
		
		FixArchiveFileLocationMarkerResolution resolution = new FixArchiveFileLocationMarkerResolution(projectMarkers[0], true);
		resolution.run(projectMarkers[0]);
		
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		JobUtils.waitForIdle(1000);
		projectMarkers = resource.findMarkers(ArquillianConstants.MARKER_INVALID_ARCHIVE_FILE_LOCATION_ID, true,
				IResource.DEPTH_INFINITE);
		assertTrue("An invalid marker is created.", projectMarkers.length == 0);
	}

	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
