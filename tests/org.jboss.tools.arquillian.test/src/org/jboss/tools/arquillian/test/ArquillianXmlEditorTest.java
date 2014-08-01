/*************************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc. and others.
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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.sapphire.ui.swt.xml.editor.SapphireEditorForXml;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class ArquillianXmlEditorTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "testProject";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/testProject.zip", TEST_PROJECT_NAME);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		addArquillianSupport(project);
		JobUtils.waitForIdle();
	}

	@Test
	public void openArquillianXmlEditor() throws CoreException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IProject project = getProject(TEST_PROJECT_NAME);
			IFile file = project.getFile("arquillian.xml");
			InputStream source = new ByteArrayInputStream("".getBytes());
			file.create(source, true, null);
			IEditorPart editor = IDE.openEditor(page, file);
			assertTrue("Arquillian XML editor is not opened.", editor instanceof SapphireEditorForXml);
			
			file = project.getFile("test.xml");
			String s = "<arquillian xmlns=\"http://jboss.org/schema/arquillian\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" //$NON-NLS-1$
                    + "            xsi:schemaLocation=\"http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd\">\n" //$NON-NLS-1$
                    + "</arquillian>"; //$NON-NLS-1$
			file.create(new ByteArrayInputStream(s.getBytes()), true, null);
			editor = IDE.openEditor(page, file);
			assertTrue("Arquillian XML editor is not opened.", editor instanceof SapphireEditorForXml);
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		} finally {
			page.closeAllEditors(false);
		}
	}
	
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle();
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}

}
