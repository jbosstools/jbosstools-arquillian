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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.classpath.ArquillianClassLoader;
import org.jboss.tools.arquillian.core.internal.launcher.ArquillianLaunchConfigurationDelegate;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.common.jdt.debug.RemoteDebugActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ArquillianCoreActivator implements BundleActivator {

	private static final String ARQUILLIAN_CLASSLOADER = "arquillianClassLoader"; //$NON-NLS-1$

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.arquillian.core"; //$NON-NLS-1$
		
	private static Map<String, ArquillianClassLoader> loaders = new HashMap<String, ArquillianClassLoader>();
	
	// The shared instance
	private static ArquillianCoreActivator plugin;
		
	private static BundleContext context;
	
	private ScopedPreferenceStore preferenceStore;
	
	private IElementChangedListener elementChangedListener = new IElementChangedListener() {
		
		@Override
		public void elementChanged(ElementChangedEvent event) {
			IJavaElementDelta delta = event.getDelta();
//			int kind = delta.getKind();
//			if (kind == IJavaElementDelta.ADDED) {
//				return;
//			}
			if ((delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0 ) {
				IJavaElement element = delta.getElement();
				if (element.getJavaProject() == null) {
					return;
				}
				//if (element instanceof ICompilationUnit) {
					removeProjectLoader(element.getJavaProject().getProject());
				//}
					
			}
		}
	};
	
	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.PRE_DELETE ||
					event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				remove(event);
			} else if (event.getType() == IResourceChangeEvent.PRE_BUILD && event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD) {
				remove(event);
			}
		}

		private void remove(IResourceChangeEvent event) {
			IResource project = event.getResource();
			if (project != null && project instanceof IProject) {
				removeProjectLoader(project);
			}
		}
	};

	private ILaunchConfigurationListener launchConfigurationListener = new ILaunchConfigurationListener() {
		
		@Override
		public void launchConfigurationRemoved(ILaunchConfiguration configuration) {}
		
		@Override
		public void launchConfigurationChanged(ILaunchConfiguration configuration) {}
		
		@Override
		public void launchConfigurationAdded(ILaunchConfiguration configuration) {
			if (configuration.getName() != null && configuration.getName().startsWith(RemoteDebugActivator.JBOSS_TEMP_JAVA_APPLICATION)) {
				return;
			}
			IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
			boolean enabled = prefs.getBoolean(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS);
			if (!enabled) {
				return;
			}
			try {
				String typeId = configuration.getType().getIdentifier();
				boolean add = prefs.getBoolean(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG) && 
						(ArquillianConstants.JUNIT_LAUNCHCONFIG_TYPE_ID.equals(typeId) || 
						 ArquillianConstants.TESTNG_LAUNCHCONFIG_TYPE_ID.equals(typeId) );
				if (add || ArquillianLaunchConfigurationDelegate.ID.equals(typeId)) {
					String arguments = prefs.getString(ArquillianConstants.DEFAULT_VM_ARGUMENTS);
					if (arguments != null && !arguments.isEmpty()) {
						arguments = arguments.trim();
						ArquillianUtility.addArguments(configuration, arguments, true);
					}
				}
			} catch (CoreException e) {
				log(e);
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
				resourceChangeListener, IResourceChangeEvent.PRE_DELETE|IResourceChangeEvent.PRE_CLOSE|IResourceChangeEvent.PRE_BUILD);
		JavaCore.addElementChangedListener(elementChangedListener);
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);
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
		JavaCore.removeElementChangedListener(elementChangedListener);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(launchConfigurationListener);
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
	
	public static void logError(String message) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message);
		plugin.getLog().log(status);
	}
	
	public static void logWarning(String message) {
		IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, message);
		plugin.getLog().log(status);
	}
	
	public ILog getLog() {
		Bundle bundle = context.getBundle();
		return InternalPlatform.getDefault().getLog(bundle);
	}
	
	public ClassLoader getClassLoader(IJavaProject javaProject) {
		synchronized (this) {
			if (javaProject == null) {
				return null;
			}
			String projectName = javaProject.getProject().getName();
			ClassLoader loader = loaders.get(projectName);
			if (loader == null) {
				loader = new ArquillianClassLoader(this.getClass()
						.getClassLoader(), javaProject);
				loaders.put(projectName, (ArquillianClassLoader) loader);
			}
			return loader;
		}
	}

	public void removeProjectLoader(IResource project) {
		synchronized (this) {
			String projectName = project.getName();
			ArquillianClassLoader loader = loaders.get(projectName);
			if (loader != null) {
				loader.clear();
				loaders.remove(projectName);
			}
			File loaderFile = getLoaderDirectory((IProject) project);
			ArquillianUtility.deleteFile(loaderFile);
			Bundle bundle = Platform
					.getBundle(ArquillianCoreActivator.PLUGIN_ID);
			IPath stateLocation = InternalPlatform.getDefault()
					.getStateLocation(bundle, true);
			IPath location = stateLocation.append(projectName);
			File file = location.toFile();
			ArquillianUtility.deleteFile(file);
		}
	}

	public static File getLoaderDirectory(IProject project) {
		File base = getLoaderBase();
		String name = project.getName();
		return new File(base, name);
	}

	public static File getLoaderBase() {
		IPath stateLocation = getDefault().getStateLocation();
		File rootFile = stateLocation.toFile();
		File base = new File(rootFile, ARQUILLIAN_CLASSLOADER);
		return base;
	}
	
	public IPreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
					PLUGIN_ID);
		}
		return preferenceStore;
	}

	public IPath getStateLocation() {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		return InternalPlatform.getDefault().getStateLocation(bundle, true);
	}

}
