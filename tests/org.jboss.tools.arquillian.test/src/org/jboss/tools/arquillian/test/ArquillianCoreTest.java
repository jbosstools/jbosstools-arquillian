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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
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
public class ArquillianCoreTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "testProject";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/testProject.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.addArquillianNature(project, true);
		JobUtils.waitForIdle();
	}

	@Test
	public void testAddArquillianSupport() throws CoreException {
		IProject project = getProject(TEST_PROJECT_NAME);
		assertTrue("The '" + TEST_PROJECT_NAME + "' project isn't open.", project.isOpen());
		assertTrue("The '" + TEST_PROJECT_NAME + "' project hasn't the Arquillian nature.", project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID));
	}
	
	@Test
	public void testArquillianValidatorSettings() {
		assertTrue("The Arquillian validator isn't enabled.", ArquillianUtility.isValidatorEnabled(getProject(TEST_PROJECT_NAME)));
	}
	
	@Test
	public void testArquillianClass() throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(getProject(TEST_PROJECT_NAME));
		assertTrue("The '" + ArquillianSearchEngine.ARQUILLIAN_JUNIT_ARQUILLIAN + "' type doesn't exist.", ArquillianSearchEngine.hasArquillianType(javaProject));
	}

	@Test
	public void testInitialProfiles() {
		List<String> profiles = ArquillianUtility.getProfiles(getProject(TEST_PROJECT_NAME));
		assertTrue("The '" + ArquillianConstants.JBOSS_AS_REMOTE_7_X + "' profile doesn't exist.", profiles.contains(ArquillianConstants.JBOSS_AS_REMOTE_7_X));
	}
	
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
