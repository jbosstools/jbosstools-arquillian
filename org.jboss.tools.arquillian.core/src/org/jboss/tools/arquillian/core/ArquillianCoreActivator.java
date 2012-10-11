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
package org.jboss.tools.arquillian.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.arquillian.core.internal.classpath.xpl.BuildPathClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ArquillianCoreActivator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.arquillian.core"; //$NON-NLS-1$
		
	private static Map<String, ClassLoader> loaders = new HashMap<String, ClassLoader>();
	
	// The shared instance
	private static ArquillianCoreActivator plugin;
		
	private static BundleContext context;
	
	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.PRE_DELETE ||
					event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				IResource project = event.getResource();
				if (project != null && project instanceof IProject) {
					String projectName = project.getName();
					loaders.remove(projectName);
				}
			}
		}
	};

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		plugin = this;
		ArquillianCoreActivator.context = bundleContext;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.PRE_DELETE|IResourceChangeEvent.PRE_CLOSE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		ArquillianCoreActivator.context = null;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ArquillianCoreActivator getDefault() {
		return plugin;
	}
	
	public static void log(Exception e, String message) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
		plugin.getLog().log(status);
	}

	public static void log(Throwable e) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e
				.getLocalizedMessage(), e);
		plugin.getLog().log(status);
	}
	
	public ILog getLog() {
		Bundle bundle = context.getBundle();
		return InternalPlatform.getDefault().getLog(bundle);
	}
	
	public ClassLoader getClassLoader(IJavaProject javaProject) {
		if (javaProject == null) {
			return null;
		}
		String projectName = javaProject.getProject().getName();
		ClassLoader loader = loaders.get(projectName);
		if (loader == null) {
			loader = new BuildPathClassLoader(this.getClass().getClassLoader(), javaProject);
			loaders.put(projectName, loader);
		}
		return loader;
	}

}
