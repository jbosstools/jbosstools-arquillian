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

import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * 
 * @author Fred Bricon
 * 
 */
public class CheckArquillianModificationsTest extends AbstractArquillianTest {

	private static final String PROJECT_NAME = "JBIDE-15030";

	@Test
	public void testAddArquillianSupportKeepsCompilerSettings() throws Exception {
		String projectName = PROJECT_NAME;
		IProject project = getProject(projectName);
		if (project !=null){
			project.delete(true, new NullProgressMonitor());
		}
	
		importMavenProject("projects/JBIDE-15030.zip", projectName);
		JobUtils.waitForIdle(1000);
		project = getProject(projectName);
		assertNotNull(project);
		ArquillianUtility.addArquillianNature(project);
		JobUtils.waitForIdle();
		
		//If test run w/ Java 6, project will have errors, 
		//so just check pom.xml contents directly
		InputStream is = null;
		try {
			is = project.getFile("pom.xml").getContents();
			String pomContent = IOUtil.toString(is);
			assertTrue("pom.xml doesn't contain expected compiler source (1.7)\n"+pomContent, pomContent.contains("<source>1.7</source>"));
			assertTrue("pom.xml doesn't contain expected compiler target (1.7)\n"+pomContent, pomContent.contains("<target>1.7</target>"));
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
		getProject(PROJECT_NAME).delete(true, true, null);
	}

}
