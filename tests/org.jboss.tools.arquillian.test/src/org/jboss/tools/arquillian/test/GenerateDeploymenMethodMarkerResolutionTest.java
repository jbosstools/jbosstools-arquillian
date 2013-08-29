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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.internal.markers.GenerateDeploymentMethodMarkerResolution;
import org.jboss.tools.arquillian.ui.internal.markers.GenerateDeploymentResolutionGenerator;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
@SuppressWarnings("restriction")
public class GenerateDeploymenMethodMarkerResolutionTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "test";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/test.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.addArquillianNature(project);
		JobUtils.waitForIdle(1000);
		if (!ArquillianUtility.isValidatorEnabled(project)) {
			IEclipsePreferences prefs = new ProjectScope(project).getNode(ArquillianCoreActivator.PLUGIN_ID);
			prefs.putBoolean(ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR, true);
			prefs.flush();
		}
		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testMarkerResolution() throws CoreException {
		IProject proj = getProject(TEST_PROJECT_NAME);
		
		IResource resource = proj.findMember("/src/test/java/org/jboss/tools/arquillian/test/DeploymentTest.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian marker isn't created", projectMarkers.length == 1);
		IMarker marker = projectMarkers[0];
		IMarkerResolutionGenerator2 markerGenerator = new GenerateDeploymentResolutionGenerator();
		assertTrue(markerGenerator.hasResolutions(marker));
		IMarkerResolution[] resolutions = markerGenerator.getResolutions(marker);
		assertTrue("There isn't a quick fix", resolutions.length == 1);
		IMarkerResolution resolution = resolutions[0];
		assertTrue("Invalid quick fix", resolution instanceof GenerateDeploymentMethodMarkerResolution);
	}
	
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
