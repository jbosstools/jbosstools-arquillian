/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.project.examples.model.ProjectExampleUtil;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class ContainerParser {
	
	private static final String CONTAINERS_JSON = "https://raw.github.com/forge/plugin-arquillian/master/src/main/resources/containers.json"; //$NON-NLS-1$

	private static List<Container> containers;
	
	public static void refresh() {
		containers = null;
	}
	public static List<Container> getContainers() {
		if (containers != null) {
			return containers;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		URL url = getUrl();
		if (url != null) {
			try {
				containers = objectMapper.readValue(url,
						new TypeReference<List<Container>>() {
						});
				return containers;
			} catch (Exception e) {
				ArquillianCoreActivator.log(e);
			}
		}
		return Collections.emptyList();
	}

	private static URL getUrl() {
		try {
			File f = ProjectExampleUtil.getProjectExamplesFile(new URL(CONTAINERS_JSON),
					"containers", "json", new NullProgressMonitor()); //$NON-NLS-1$ //$NON-NLS-2$
			if (f == null || !f.exists()) {
				return getUrlFromBundle();
			} else {
				return f.toURI().toURL();
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

}
