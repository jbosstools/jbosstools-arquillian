/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.classpath;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianClassLoader extends URLClassLoader {

	/**
	 * @param project
	 */
	public ArquillianClassLoader(IJavaProject project) {
		super(getURLs(project), ClassLoader.getSystemClassLoader());
	}
	
	private static URL[] getURLs(IJavaProject project) {
		Set<URL> urls = getURLSet(project);
		return urls.toArray(new URL[0]);
	}

	private static File getRawLocationFile(IPath simplePath) {
		IResource resource = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(simplePath);
		File file = null;
		if (resource != null) {
			file = ResourcesPlugin.getWorkspace().getRoot().findMember(
					simplePath).getLocation().toFile();
		} else {
			file = simplePath.toFile();
		}
		return file;
	}
	
	private static Set<URL> getURLSet(IJavaProject jProject) {
		Set<URL> urls = new HashSet<URL>();
		Set<IJavaProject> dependentProjects = new HashSet<IJavaProject>();
		if (jProject == null) {
			return urls;
		}
		try {
			IClasspathEntry[] entries = jProject.getRawClasspath();

			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					addSource(jProject, urls, entry);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IClasspathEntry resLib = JavaCore
							.getResolvedClasspathEntry(entry);
					addLibrary(urls, resLib);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					addProject(urls, entry, dependentProjects);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
					IClasspathEntry resLib = JavaCore
							.getResolvedClasspathEntry(entry);
					addLibrary(urls, resLib);
				} else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					if (!entry.getPath().segment(0).toString().endsWith("JRE_CONTAINER")) { //$NON-NLS-1$
						addContainer(jProject, urls, entry, dependentProjects);
					}
				}
			}

		} catch (Exception e) {
			ArquillianCoreActivator.log(e);
		} 
		
		return urls;
	}
	
	private static void addContainer(IJavaProject jProject,
			Set<URL> urls, IClasspathEntry entry, Set<IJavaProject> dependentProjects)
			throws JavaModelException, MalformedURLException {
		IClasspathEntry[] resLibs = JavaCore.getClasspathContainer(entry.getPath(),
						jProject).getClasspathEntries();
		for (int i = 0; i < resLibs.length; i++) {
			if (resLibs[i] == null) {
				continue;
			}
			if (resLibs[i].getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				addProject(urls, resLibs[i], dependentProjects);
			} else if (resLibs[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				addContainer(jProject, urls, resLibs[i], dependentProjects);
			} else if (resLibs[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				addLibrary(urls, resLibs[i]);
			} else if (resLibs[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				addSource(jProject, urls, resLibs[i]);
			}
		}
	}

	private static void addSource(IJavaProject jProject, Set<URL> urls,
			IClasspathEntry entry) throws JavaModelException, MalformedURLException {
		IPath path = entry.getOutputLocation();
		if (path == null) {
			path = jProject.getOutputLocation();
		}
		addPath(urls, path);
		IPath sourcePath = entry.getPath();
		if (sourcePath != null) {
			addPath(urls, sourcePath);
		}
	}

	private static void addPath(Set<URL> urls, IPath path)
			throws MalformedURLException {
		File file = getRawLocationFile(path);
		if (file.exists()) {
			urls.add(file.toURI().toURL());
		}
	}

	private static void addProject(Set<URL> urls, IClasspathEntry entry, Set<IJavaProject> dependentProjects) {
		IClasspathEntry projectEntry = JavaCore
				.getResolvedClasspathEntry(entry);
		IPath path = projectEntry.getPath();
		String name = path.segment(0);
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(name);
		if (project.exists()) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject.exists() && !dependentProjects.contains(javaProject)) {
				dependentProjects.add(javaProject);
				urls.addAll(getURLSet(javaProject));
			}
		}
	}
	private static void addLibrary(Set<URL> urls, IClasspathEntry resLib)
			throws MalformedURLException {
		IPath path = resLib.getPath();
		String ls = path.lastSegment();
		if (ls != null && ls.length() > 0) {
			File file;
			addPath(urls, path);
		}
	}

}
