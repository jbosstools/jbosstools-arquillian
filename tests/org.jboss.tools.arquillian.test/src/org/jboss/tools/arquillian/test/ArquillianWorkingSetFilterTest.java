/*************************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.arquillian.ui.internal.filters.ActiveProjectFilter;
import org.jboss.tools.arquillian.ui.internal.filters.WorkingSetFilter;
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
public class ArquillianWorkingSetFilterTest extends AbstractArquillianTest {

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
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
		project = getProject(TEST_PROJECT_NAME2);
		addArquillianSupport(project);
		JobUtils.waitForIdle(1000);
		
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkingSetManager manager = workbenchWindow.getWorkbench()
			            .getWorkingSetManager();
				IWorkingSet workingSet = manager.createWorkingSet("test", 
						new IAdaptable[] {getProject(TEST_PROJECT_NAME), 
								getProject(TEST_PROJECT_NAME2)});
				manager.addWorkingSet(workingSet);
				IWorkingSet[] ws = manager.getAllWorkingSets();
				System.out.println(ws);
			}
		});
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
		try {
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
			WorkingSetFilter activeFilter = null;
			List<ViewerFilter> newFilters = new ArrayList<ViewerFilter>();
			int i = 0;
			for (ViewerFilter filter: filters) {
				if ( !(filter instanceof ActiveProjectFilter) ) {
					newFilters.add(filter);
				}
			}
			newFilters.add(new WorkingSetFilter());
			viewer.setFilters(newFilters.toArray(new ViewerFilter[0]));
			
			items = viewer.getTree().getItems();
			assertTrue("Invalid filter", items.length == 2);
		} finally {
			if (arquilliaView != null) {
				page.hideView(arquilliaView);
			}
		}
		
	}
	
		
	@AfterClass
	public static void dispose() throws Exception {
		JobUtils.waitForIdle(1000);
		getProject(TEST_PROJECT_NAME).delete(true, true, null);
		getProject(TEST_PROJECT_NAME2).delete(true, true, null);
	}
	
}
