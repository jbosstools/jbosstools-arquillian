/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Nippon Telegraph and Telephone Corporation - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.test;

import static org.jboss.tools.arquillian.test.AbstractArquillianTest.getProject;
import static org.jboss.tools.arquillian.test.AbstractArquillianTest.importMavenProject;
import static org.junit.Assert.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditor;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianGeneralEditor;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ContainerWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ContainerWizardPage;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ExtensionWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ExtensionWizardPage;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.GroupWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.GroupWizardPage;
import org.jboss.tools.common.util.FileUtil;
import org.jboss.tools.maven.core.MavenCoreActivator;
import org.jboss.tools.test.util.JobUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("restriction")
public class ArquillianEditorTest {

	private static final String PROJECT_NAME = "test-editor";
	
	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/test-editor.zip", PROJECT_NAME);
		JobUtils.waitForIdle();
	}
	
	@Before
	public void before() {
		JobUtils.waitForIdle();
	}
	
	@After
	public void after() {
		JobUtils.waitForIdle();
	}
	
	@Test
	public void testOpenEmptyFile() throws Exception {
		IProject project = getProject(PROJECT_NAME);
		IFile arquillianXml = project.getFile("src/test/resources/arquillian-empty.xml");
		
		// check initial content
		assertEquals("", FileUtil.readStream(arquillianXml));
		
		open(arquillianXml);

		// assert
		assertEquals("<arquillian xmlns=\"http://jboss.org/schema/arquillian\""
							  + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
							  + " xsi:schemaLocation=\"http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd\">"
				   + "</arquillian>", FileUtil.readStream(arquillianXml));
	}

	@Test
	public void testSave() throws Exception {
		IProject project = getProject(PROJECT_NAME);
		IFile arquillianXml = project.getFile("src/test/resources/arquillian-for-save.xml");
		
		ArquillianEditor editor = open(arquillianXml);
		editor.doSave(null);
		JobUtils.waitForIdle();
		
		// assert
		IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
		PomResourceImpl pomResource = null;
		try {
			pomResource = MavenCoreActivator.loadResource(pomFile);
			Model model = pomResource.getModel();
			assertEquals(1, model.getProfiles().size());
			Profile profile = model.getProfiles().get(0);
			assertEquals("JBOSS_AS_MANAGED_7.X", profile.getId());
			
			assertEquals(3, model.getDependencies().size());
			assertEquals("arquillian-extension-byteman", model.getDependencies().get(0).getArtifactId());
			assertEquals("byteman", model.getDependencies().get(1).getArtifactId());
			assertEquals("byteman-submit", model.getDependencies().get(2).getArtifactId());
		} finally {
			pomResource.unload();
		}
	}
	
	@Test
	public void testContainerWizard() throws Exception {
		IProject project = getProject(PROJECT_NAME);
		IFile arquillianXml = project.getFile("src/test/resources/arquillian-for-container-wizard.xml");
		
		ArquillianEditor editor = open(arquillianXml);
		JobUtils.waitForIdle();
		
		ArquillianGeneralEditor generalEditor = (ArquillianGeneralEditor) editor.getSelectedPage();
		ContainerWizard wizard = new ContainerWizard(generalEditor.getContainers());
		WizardDialog dialog = null;
		try {
			dialog = openWizard(wizard);
	
			ContainerWizardPage page = (ContainerWizardPage) dialog.getCurrentPage();
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			Composite composite = (Composite) page.getControl();
			Composite innerComposite = (Composite) composite.getChildren()[0];
			Combo containers = (Combo) innerComposite.getChildren()[1];
			Composite qualifierComposite = (Composite) innerComposite.getChildren()[4];
			Text qualifier = (Text) qualifierComposite.getChildren()[0];
			
			containers.setText("JBOSS_AS_MANAGED_7.X");
			qualifier.setText("container");
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			containers.setText("JBOSS_AS_MANAGED_7.X");
			qualifier.setText("container");
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			qualifier.setText("container2");
			
			assertTrue(wizard.canFinish());
			assertTrue(page.isPageComplete());
			
			qualifier.setText("container");
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			containers.setText("JBOSS_AS_REMOTE_7.X");
			
			assertTrue(wizard.canFinish());
			assertTrue(page.isPageComplete());
			
			wizard.performFinish();
			
			Arquillian arquillian = (Arquillian) generalEditor.getContainers().getInput();
			assertEquals(2, arquillian.getContainerGroups().size());
			assertEquals("container-JBOSS_AS_MANAGED_7.X", arquillian.getContainerGroups().get(0).getQualifier());
			assertEquals("container-JBOSS_AS_REMOTE_7.X", arquillian.getContainerGroups().get(1).getQualifier());
		} finally {
			dialog.close();
		}
	}
	
	@Test
	public void testGroupWizard() throws Exception {
		IProject project = getProject(PROJECT_NAME);
		IFile arquillianXml = project.getFile("src/test/resources/arquillian-for-group-wizard.xml");
		
		ArquillianEditor editor = open(arquillianXml);
		JobUtils.waitForIdle();
		
		ArquillianGeneralEditor generalEditor = (ArquillianGeneralEditor) editor.getSelectedPage();
		GroupWizard wizard = new GroupWizard(generalEditor.getContainers());
		WizardDialog dialog = null;
		try {
			dialog = openWizard(wizard);
	
			GroupWizardPage page = (GroupWizardPage) dialog.getCurrentPage();
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			Composite composite = (Composite) page.getControl();
			Text qualifier = (Text) composite.getChildren()[1];
			
			qualifier.setText("group");
			
			assertTrue(wizard.canFinish());
			assertTrue(page.isPageComplete());

			qualifier.setText("container-JBOSS_AS_MANAGED_7.X");
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());

			qualifier.setText("container");
			
			assertTrue(wizard.canFinish());
			assertTrue(page.isPageComplete());
			
			wizard.performFinish();
			
			Arquillian arquillian = (Arquillian) generalEditor.getContainers().getInput();
			assertEquals(2, arquillian.getContainerGroups().size());
			assertEquals("container-JBOSS_AS_MANAGED_7.X", arquillian.getContainerGroups().get(0).getQualifier());
			assertEquals("container", arquillian.getContainerGroups().get(1).getQualifier());
		} finally {
			dialog.close();
		}
	}
	
	@Test
	public void testExtensionWizard() throws Exception {
		IProject project = getProject(PROJECT_NAME);
		IFile arquillianXml = project.getFile("src/test/resources/arquillian-for-extension-wizard.xml");
		
		ArquillianEditor editor = open(arquillianXml);
		JobUtils.waitForIdle();
		
		ArquillianGeneralEditor generalEditor = (ArquillianGeneralEditor) editor.getSelectedPage();
		ExtensionWizard wizard = new ExtensionWizard(generalEditor.getExtensions());
		WizardDialog dialog = null;
		try {
			dialog = openWizard(wizard);
	
			ExtensionWizardPage page = (ExtensionWizardPage) dialog.getCurrentPage();
			
			assertFalse(wizard.canFinish());
			assertFalse(page.isPageComplete());
			
			Composite composite = (Composite) page.getControl();
			Combo type = (Combo) composite.getChildren()[1];
			
			// assert added extension is not included.
			for(String t : type.getItems()) {
				assertNotEquals("byteman", t);
			}
			
			type.setText("webdriver");

			assertTrue(wizard.canFinish());
			assertTrue(page.isPageComplete());
			
			wizard.performFinish();
			
			Arquillian arquillian = (Arquillian) generalEditor.getContainers().getInput();
			assertEquals(2, arquillian.getExtensions().size());
			assertEquals("byteman", arquillian.getExtensionQualifiers().get(0));
			assertEquals("webdriver", arquillian.getExtensionQualifiers().get(1));
		} finally {
			dialog.close();
		}
	}
	
	private ArquillianEditor open(IFile arquillianXml) throws PartInitException {
		IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry().findEditor("org.jboss.tools.arquillian.ui.editors.arquillianEditor");
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		
		return (ArquillianEditor) page.openEditor(new FileEditorInput(arquillianXml), editorDesc.getId());
	}
	
	private WizardDialog openWizard(IWizard wizard) {
		if(wizard instanceof IWorkbenchWizard) {
			((IWorkbenchWizard) wizard).init(ArquillianUIActivator.getDefault().getWorkbench(), null);
		}
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.setHelpAvailable(false);
		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();
		return dialog;
	}
}
