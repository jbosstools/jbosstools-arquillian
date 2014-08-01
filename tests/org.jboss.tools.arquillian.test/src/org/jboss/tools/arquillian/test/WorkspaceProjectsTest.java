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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class WorkspaceProjectsTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "workspaceProject1";
	private static final String TEST_PROJECT_NAME2 = "workspaceProject2";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/workspaceProject1.zip", TEST_PROJECT_NAME);
		IPath path = Platform.getLocation();
		path = path.append(TEST_PROJECT_NAME2);
		File location = new File(path.toOSString());
		location.mkdirs();
		importMavenProject("projects/workspaceProject2.zip", TEST_PROJECT_NAME2, path);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
		project = getProject(TEST_PROJECT_NAME2);
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testWorkspaceProject() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME2);
		IResource resource = project.findMember("/src/test/java/org/jboss/tools/arquillian/ArquillianTest.java");
		assertNotNull(resource);
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_RESOURCE_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("There are Arquillian resource markers", projectMarkers.length == 0);
		projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_CLASS_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("There are Arquillian class markers", projectMarkers.length == 0);
		
	}
	
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
		getProject(TEST_PROJECT_NAME2).delete(true, true, null);
	}
	
}
