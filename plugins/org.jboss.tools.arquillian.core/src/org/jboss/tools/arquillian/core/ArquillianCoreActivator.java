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

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.archives.ArchiveContainer;
import org.jboss.tools.arquillian.core.internal.classpath.ArquillianClassLoader;
import org.jboss.tools.arquillian.core.internal.dependencies.DependencyCache;
import org.jboss.tools.arquillian.core.internal.launcher.ArquillianLaunchConfigurationDelegate;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.common.jdt.debug.RemoteDebugActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class ArquillianCoreActivator implements BundleActivator {

	private static final String ARQUILLIAN_CLASSLOADER = "arquillianClassLoader"; //$NON-NLS-1$

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.arquillian.core"; //$NON-NLS-1$
		
	private static Map<String, ArquillianClassLoader> loaders = new HashMap<String, ArquillianClassLoader>();
	
	// The shared instance
	private static ArquillianCoreActivator plugin;
		
	private static BundleContext context;
	
	private ServiceTracker parserTracker = null;
	
	private ScopedPreferenceStore preferenceStore;
	
	private IElementChangedListener elementChangedListener = new IElementChangedListener() {
		
		@Override
		public void elementChanged(ElementChangedEvent event) {
			IJavaElementDelta delta = event.getDelta();
			processDelta(delta);
		}

		private boolean processDelta(IJavaElementDelta delta) {
			int kind= delta.getKind();
			int flags= delta.getFlags();
			IJavaElement element= delta.getElement();
			int elementType= element.getElementType();
			if (isClassPathChange(delta)) {
				cleanup(element.getJavaProject());
				return true;
			}
			if (elementType == IJavaElement.JAVA_PROJECT) {
				if ((flags & (IJavaElementDelta.F_CLOSED | IJavaElementDelta.F_OPENED)) != 0) {
					cleanup(element.getJavaProject());
					return true;
				}
				if ((flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
					cleanup(element.getJavaProject());
					return true;
				}
				if (kind == IJavaElementDelta.ADDED) {
					cleanup(element.getJavaProject());
					return true;
				}
			}
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			for (IJavaElementDelta d:affectedChildren) {
				if (processDelta(d)) {
					return true;
				}
			}
			return false;
		}

		public void cleanup(IJavaProject javaProject) {
			removeProjectLoader(javaProject.getProject());
			ArchiveContainer.remove(javaProject.getProject());
			DependencyCache.removeDependencies(javaProject.getProject());
		}
		
		private boolean isClassPathChange(IJavaElementDelta delta) {

			if (delta.getElement().getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT)
				return false;

			int flags= delta.getFlags();
			return (delta.getKind() == IJavaElementDelta.CHANGED &&
				((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
				 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
				 ((flags & IJavaElementDelta.F_REORDER) != 0));
		}
	};

	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
				return;
			}
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			try {
				delta.accept(new IResourceDeltaVisitor() {
					
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource resource = delta.getResource();
						if (resource instanceof IWorkspaceRoot) {
				    		return true;
				    	}
						if (resource instanceof IProject) {
							if ( (delta.getFlags() & IResourceDelta.OPEN) != 0) {
								IProject project = (IProject) resource;
								if (project.isOpen() && project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
									fixProject(project);
								}
							}
							return false;
						}
				        return false;
					}
				});
			} catch (CoreException e) {
				ArquillianCoreActivator.log(e);
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
		JavaCore.addElementChangedListener(elementChangedListener);
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ArquillianCoreActivator.PLUGIN_ID);
		boolean firstStart = prefs.getBoolean(ArquillianConstants.FIRST_START, true);
		if (firstStart) {
			fixProjects();
		}
	}
	
	private void fixProjects() {
		WorkspaceJob job = new WorkspaceJob("Fixing projects") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				if (!PlatformUI.isWorkbenchRunning()) {
					return Status.OK_STATUS;
				}
				try {
					IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
							.getProjects();
					for (IProject project : projects) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						if (project.isAccessible() && project.isOpen() && project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
							ArquillianUtility.addBuilder(project);
						}
					}
					IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(ArquillianCoreActivator.PLUGIN_ID);
					prefs.putBoolean(ArquillianConstants.FIRST_START, false);
					prefs.flush();
				} catch (Exception e) {
					ArquillianCoreActivator.log(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void fixProject(final IProject project) {
		if (project == null || !project.isAccessible()) {
			return;
		}
		WorkspaceJob job = new WorkspaceJob("Fixing the '" + project.getName() + "' project") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				if (!PlatformUI.isWorkbenchRunning()) {
					return Status.OK_STATUS;
				}
				if (project.isAccessible() && project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
					ArquillianUtility.addBuilder(project);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		ArquillianCoreActivator.context = null;
		JavaCore.removeElementChangedListener(elementChangedListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(launchConfigurationListener);
		if (preferenceStore != null) {
			preferenceStore.save();
		}
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

	public void removeProjectLoader(IProject project) {
		synchronized (this) {
			String projectName = project.getName();
			ArquillianClassLoader loader = loaders.get(projectName);
			if (loader != null) {
				loader.clear();
				loaders.remove(projectName);
			}
			File loaderFile = getLoaderDirectory((IProject) project);
			ArquillianUtility.deleteFile(loaderFile);
			Bundle bundle = Platform.getBundle(ArquillianCoreActivator.PLUGIN_ID);
			IPath stateLocation = Platform.getStateLocation(bundle);
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
	
	/*
	 * Return the registered SAX parser factory or null if one
	 * does not exist.
	 */
	public SAXParserFactory getFactory() {
		if (parserTracker == null) {
			parserTracker = new ServiceTracker(getContext(), SAXParserFactory.class.getName(), null);
			parserTracker.open();
		}
		SAXParserFactory theFactory = (SAXParserFactory) parserTracker.getService();
		if (theFactory != null)
			theFactory.setNamespaceAware(true);
		return theFactory;
	}

}
