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

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.utils.IDeploymentDescriptor;
import org.jboss.tools.arquillian.ui.internal.wizards.ProjectResource;
import org.jboss.tools.test.util.JobUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
@SuppressWarnings("restriction")
public class CreateDeploymentMethodTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "test";

	@Before
	public void init() throws Exception {
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
	public void testCreateDeploymentMethod() throws CoreException {
		IResource resource = createDeploymentMethod(ArquillianUIActivator.RAR);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_CLASS_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Deployment method isn't created", projectMarkers.length == 0);		
	}

	private IResource createDeploymentMethod(String archiveType)
			throws CoreException, JavaModelException {
		return createDeploymentMethod(archiveType, false);
	}
	
	private IResource createDeploymentMethod(String archiveType, boolean beansXml)
			throws CoreException, JavaModelException {
		IProject proj = getProject(TEST_PROJECT_NAME);
		
		IResource resource = proj.findMember("/src/test/java/org/jboss/tools/arquillian/test/DeploymentTest.java");
		assertTrue(resource instanceof IFile);
		IMarker[] projectMarkers = resource.findMarkers(
				ArquillianConstants.MARKER_MISSING_DEPLOYMENT_METHOD_ID, true, IResource.DEPTH_INFINITE);
		assertTrue("Arquillian marker isn't created", projectMarkers.length == 1);
		
		IJavaProject project = JavaCore.create(proj);
		assertNotNull(project);
		assertTrue(project.isOpen());
		
		ICompilationUnit icu = JavaCore.createCompilationUnitFrom((IFile) resource).getWorkingCopy(null);
		assertNotNull(icu);
		
		IType type = icu.getTypes()[0];
		DeploymentDescriptor descriptor = new DeploymentDescriptor(archiveType, beansXml);
		String delimiter = type.getPackageFragment().findRecommendedLineSeparator();
		IJavaElement position = type.getChildren()[0];
		ArquillianUIUtil.createDeploymentMethod(icu, type, null, 
				false, delimiter,
				descriptor, 
				position, false);
		JobUtils.waitForIdle(1000);
		return resource;
	}
	
	@Test
	public void testAddEmptyBeansXmlEAR() throws CoreException, IOException {
		IResource resource = createDeploymentMethod(ArquillianUIActivator.EAR, true);
		JobUtils.waitForIdle(1000);
		assertTrue(resource instanceof IFile);
		InputStream is = null;
		try {
			is = ((IFile)resource).getContents();
			String content = IOUtil.toString(is);
			assertFalse("Invalid beans.xml", content.contains("beans.xml"));
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	@Test
	public void testAddEmptyBeansXmlWAR() throws CoreException, IOException {
		IResource resource = createDeploymentMethod(ArquillianUIActivator.WAR, true);
		JobUtils.waitForIdle(1000);
		assertTrue(resource instanceof IFile);
		InputStream is = null;
		try {
			is = ((IFile)resource).getContents();
			String content = IOUtil.toString(is);
			assertTrue("Invalid beans.xml", content.contains("addAsWebInfResource"));
			assertFalse("Invalid beans.xml", content.contains("addAsManifestResource"));
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	@Test
	public void testAddEmptyBeansXmlJAR() throws CoreException, IOException {
		IResource resource = createDeploymentMethod(ArquillianUIActivator.JAR, true);
		JobUtils.waitForIdle(1000);
		assertTrue(resource instanceof IFile);
		InputStream is = null;
		try {
			is = ((IFile)resource).getContents();
			String content = IOUtil.toString(is);
			assertFalse("Invalid beans.xml", content.contains("addAsWebInfResource"));
			assertTrue("Invalid beans.xml", content.contains("addAsManifestResource"));
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	@After
	public void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
	}
	
	public static class DeploymentDescriptor implements IDeploymentDescriptor {

		private String type;
		private boolean beansXml;

		public DeploymentDescriptor(String type) {
			this(type, false);
		}
		
		public DeploymentDescriptor(String type, boolean beansXml) {
			this.type = type;
			this.beansXml = beansXml;
		}
		
		@Override
		public boolean addBeansXml() {
			return beansXml;
		}

		@Override
		public String getArchiveName() {
			return null;
		}

		@Override
		public String getArchiveType() {
			return type;
		}

		@Override
		public String getDeploymentName() {
			return null;
		}

		@Override
		public String getDeploymentOrder() {
			return null;
		}

		@Override
		public String getMethodName() {
			return "createDeployment";
		}

		@Override
		public ProjectResource[] getResources() {
			return new ProjectResource[0];
		}

		@Override
		public IType[] getTypes() {
			return new IType[0];
		}
		
	}

}
