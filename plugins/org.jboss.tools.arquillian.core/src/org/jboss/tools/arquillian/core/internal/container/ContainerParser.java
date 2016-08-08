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
package org.jboss.tools.arquillian.core.internal.container;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.foundation.core.ecf.URLTransportUtility;
import org.osgi.framework.Bundle;
/**
 * 
 * @author snjeza
 *
 */
public class ContainerParser {
	
	private static final String CONTAINERS_JSON = "https://raw.github.com/forge/plugin-arquillian/master/src/main/resources/containers.json"; //$NON-NLS-1$
	private static final String PROTOCOL_FILE = "file"; //$NON-NLS-1$

	private static final String PROTOCOL_PLATFORM = "platform"; //$NON-NLS-1$

	private static List<Container> containers;
	
	/**
	 * Refresh parser
	 * 
	 */
	public static void refresh() {
		containers = null;
	}
	
	/**
	 * Returns list of containers
	 * 
	 */
	public static List<Container> getContainers() {
		if (containers != null) {
			return containers;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		// a workaround for JBIDE-19324 - Arquillian core freezes Eclipse startup
		boolean suspended = Job.getJobManager().isSuspended();
		if (!suspended) {
			URL url = getUrl();
			if (url != null) {
				try {
					containers = objectMapper.readValue(url, new TypeReference<List<Container>>() {
					});
				} catch (Exception e) {
					ArquillianCoreActivator.log(e);
				}
			}
		} else {
			// we can't use Eclipse job framework because JobManager is suspended in this moment
			Thread refreshingThread = new Thread("Refreshing containers ...") {

				@Override
				public void run() {
					while (Job.getJobManager().isSuspended()) {
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							// ignore
						}
					}
					refresh();
					getContainers();
				}

			};
			refreshingThread.start();
		}
		if (containers == null || containers.size() == 0) {
			try {
				File f =  getFile(new URL(CONTAINERS_JSON));
				URL url;
				if (f != null && f.isFile() && f.length() > 0) {
					url = f.toURL();
				} else {
					url = getUrlFromBundle();
				}
				containers = objectMapper.readValue(url, new TypeReference<List<Container>>() {
						});
				return containers;
			} catch (Exception e) {
				ArquillianCoreActivator.log(e);
			}
		}
		if (containers == null) {
			return Collections.emptyList();
		}
		return containers;
	}

	private static URL getUrl() {
		try {
			File f = getFile(new URL(CONTAINERS_JSON), "containers", "json", new NullProgressMonitor()); //$NON-NLS-1$ //$NON-NLS-2$
			if (f == null || !f.exists()) {
				return getUrlFromBundle();
			} else {
				return f.toURL();
			}
		} catch (Exception e) {
			ArquillianCoreActivator.log(e);
		}
		return null;
	}

	private static URL getUrlFromBundle() {
		Bundle bundle = Platform.getBundle(ArquillianCoreActivator.PLUGIN_ID);
		if (bundle != null) {
			URL[] urls = FileLocator.findEntries(bundle, new Path(
					"/containers.json")); //$NON-NLS-1$
			if (urls != null && urls.length > 0) {
				try {
					return FileLocator.resolve(urls[0]);
				} catch (IOException e) {
					ArquillianCoreActivator.log(e);
				}
			}
		}
		return null;
	}
	
	private static File getFile(URL url, String prefix,
			String suffix, IProgressMonitor monitor) {
		File file = null;
		if (PROTOCOL_FILE.equals(url.getProtocol())
				|| PROTOCOL_PLATFORM.equalsIgnoreCase(url.getProtocol())) {
			try {
				file = new File(new URI(url.toExternalForm()));
			} catch (Exception e) {
				file = new File(url.getFile());
			}
			if (!file.exists())
				return null;
		} else {
			try {
				if (monitor.isCanceled()) {
					return null;
				}
				long urlModified = -1;
				file = getFile(url);
				try {
					urlModified = new URLTransportUtility()
							.getLastModified(url);
				} catch (CoreException e) {
					if (file.exists()) {
						return file;
					}
				}
				if (file.exists()) {
					long modified = file.lastModified();
					if (modified > 0 && urlModified == modified) {
						return file;
					}
				}
				boolean fileAlreadyExists = file.exists();
				file.getParentFile().mkdirs();
				if (monitor.isCanceled()) {
					return null;
				}
				BufferedOutputStream destination = new BufferedOutputStream(
						new FileOutputStream(file));
				IStatus result = new URLTransportUtility().download(prefix,
						url.toExternalForm(), destination, monitor);
				if (!result.isOK()) {
					ArquillianCoreActivator.getDefault().getLog().log(result);
					if (!fileAlreadyExists && file.exists()) {
						file.delete();
					}
					return null;
				} else {
					if (file.exists() && urlModified > 0) {
						file.setLastModified(urlModified);
					}
				}
			} catch (FileNotFoundException e) {
				ArquillianCoreActivator.log(e);
				return null;
			}
		}
		return file;
	}

	private static File getFile(URL url) {
		IPath location = ArquillianCoreActivator.getDefault().getStateLocation();
		File root = location.toFile();
		String urlFile = url.getFile();
		File file = new File(root, urlFile);
		return file;
	}
}
