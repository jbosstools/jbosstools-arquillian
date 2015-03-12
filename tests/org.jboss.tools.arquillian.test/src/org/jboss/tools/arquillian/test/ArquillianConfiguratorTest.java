/*************************************************************************************
 * Copyright (c) 2013-2015 Red Hat, Inc. and others.
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

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.maven.ui.Activator;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class ArquillianConfiguratorTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "dependentClasses";
	private static boolean oldValue;
	
	@BeforeClass
	public static void init() throws Exception {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		oldValue = prefs.getBoolean(Activator.CONFIGURE_ARQUILLIAN);
		prefs.setValue(Activator.CONFIGURE_ARQUILLIAN, false);
		importMavenProject("projects/dependentClasses.zip", TEST_PROJECT_NAME);
		updateProject();
	}

	private static void updateProject() throws CoreException {
		JobUtils.delay(1000);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.updateProject(project);
		JobUtils.delay(1000);
		JobUtils.waitForIdle(1000);
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testConfigurator() throws CoreException, IOException {
		IProject project = getProject(TEST_PROJECT_NAME);
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		assertFalse(project.getName() +" should not have the Arquillian nature", project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID));
		prefs.setValue(Activator.CONFIGURE_ARQUILLIAN, true);
		updateProject();
		assertTrue(project.getName() +" should have the Arquillian nature", project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID));
	}

	@AfterClass
	public static void dispose() throws Exception {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		prefs.setValue(Activator.CONFIGURE_ARQUILLIAN, oldValue);
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
