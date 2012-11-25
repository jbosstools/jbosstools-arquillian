/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.views;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianView extends CommonNavigator {

	private IResourceChangeListener resourceChangeListener;

	public ArquillianView() {
		super();
	}

	@Override
	public void dispose() {
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(
					resourceChangeListener);
			resourceChangeListener = null;
		}
		super.dispose();
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		
		CommonViewer commonViewer = super.createCommonViewer(aParent);
		resourceChangeListener = new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if (!PlatformUI.isWorkbenchRunning()) {
					return;
				}
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						if (getCommonViewer() != null && getCommonViewer().getControl() != null 
								&& !getCommonViewer().getControl().isDisposed()) {
							getCommonViewer().refresh();
						}
					}
				});
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_BUILD);
		
		return commonViewer;
	}
	
}
