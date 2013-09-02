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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.internal.filters.ActiveProjectFilter;
import org.jboss.tools.arquillian.ui.internal.views.ArquillianView;
import org.jboss.tools.test.util.JobUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class ArquillianFilterTest extends AbstractArquillianTest {

	private static final String TEST_PROJECT_NAME = "testFilter1";
	private static final String TEST_PROJECT_NAME2 = "testFilter2";

	@BeforeClass
	public static void init() throws Exception {
		importMavenProject("projects/testFilter1.zip", TEST_PROJECT_NAME);
		IPath path = Platform.getLocation();
		path = path.append(TEST_PROJECT_NAME2);
		File location = new File(path.toOSString());
		location.mkdirs();
		importMavenProject("projects/testFilter2.zip", TEST_PROJECT_NAME2, path);
		JobUtils.waitForIdle(1000);
		IProject project = getProject(TEST_PROJECT_NAME);
		ArquillianUtility.addArquillianNature(project);
		JobUtils.waitForIdle(1000);
		project = getProject(TEST_PROJECT_NAME2);
		ArquillianUtility.addArquillianNature(project);
		JobUtils.waitForIdle(1000);
	}

	@Test
	public void testFilter() throws CoreException {
		IPerspectiveDescriptor persDescription = PlatformUI.getWorkbench()
				.getPerspectiveRegistry()
				.findPerspectiveWithId(JavaUI.ID_PERSPECTIVE);
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		page.setPerspective(persDescription);
		IViewPart arquilliaView = page.showView(ArquillianView.ID);
		assertTrue(arquilliaView instanceof CommonNavigator);
		PackageExplorerPart packageView = (PackageExplorerPart) page.showView(JavaUI.ID_PACKAGES);
		IProject project = getProject(TEST_PROJECT_NAME);
		packageView.selectAndReveal(project);
		
		CommonNavigator navigator = (CommonNavigator) arquilliaView;
		CommonViewer viewer = navigator.getCommonViewer();
		TreeItem[] items = viewer.getTree().getItems();
		assertTrue(items.length == 1);
		ViewerFilter[] filters = viewer.getFilters();
		assertTrue(filters.length > 0);
		ActiveProjectFilter activeProjectFilter = null;
		ViewerFilter[] newFilters = new ViewerFilter[filters.length - 1];
		int i = 0;
		int j = 0;
		for (ViewerFilter filter: filters) {
			if (filter instanceof ActiveProjectFilter) {
				activeProjectFilter = (ActiveProjectFilter) filter;
				i++;
			} else {
				newFilters[j++] = filters[i++];
			}
		}
		assertNotNull(activeProjectFilter);
		viewer.setFilters(newFilters);
		
		items = viewer.getTree().getItems();
		assertTrue("Invalid filter", items.length == 2);
	}
	
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
		getProject(TEST_PROJECT_NAME2).delete(true, true, null);
	}
	
}
