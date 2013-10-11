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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
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
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class ValidateArchiveNameTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "validateArchiveName";
	
	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/validateArchiveName.zip", TEST_PROJECT_NAME);
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
	public void testArchiveName() throws CoreException, IOException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IResource resource = project.findMember("/src/test/java/org/jboss/tools/arquillian/test/ValidateArchiveNameTest.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian markers aren't created", projectMarkers.length > 0);
		InputStream is = null;
		IFile file = (IFile) resource;
		try {
			is = file.getContents();
			String content = IOUtil.toString(is);
			content = content.replace("test.jar", "test.war");
			is.close();
			is = new ByteArrayInputStream(content.getBytes());
			file.setContents(is, IResource.FORCE, null);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
			JobUtils.waitForIdle(1000);
			projectMarkers = resource.findMarkers(
					ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID, true, IResource.DEPTH_INFINITE);
			assertTrue("An invalid marker is created.", projectMarkers.length == 0);
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

	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
