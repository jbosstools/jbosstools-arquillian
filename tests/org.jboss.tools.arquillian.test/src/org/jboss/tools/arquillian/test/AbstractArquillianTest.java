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
package org.jboss.tools.arquillian.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.project.examples.model.ProjectExample;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class AbstractArquillianTest {

	protected static void importMavenProject(String zipEntry, String projectName) throws Exception {
		File zipFile = createFile(zipEntry, projectName);
		zipFile.deleteOnExit();
		ProjectExample projectExample = new ProjectExample();
		projectExample.setImportType("maven");
		projectExample.setName(projectName);
		projectExample.setUrl(zipFile.toURI().toURL().toString());
		List<String> includedProjects = new ArrayList<String>();
		includedProjects.add(projectName);
		projectExample.setIncludedProjects(includedProjects);
		
		new ImportMavenProject().importProject(projectExample, zipFile, new HashMap<String, Object>(), Platform.getLocation());
	}

	public static File createFile(String entryName, String projectName) throws IOException,
			FileNotFoundException {
		Bundle bundle = Platform.getBundle(ArquillianTestActivator.PLUGIN_ID);
		URL url = bundle.getEntry(entryName);
		File outputFile = File.createTempFile(projectName, ".zip");
		InputStream in = null;
		OutputStream out = null;

		try {
			in = url.openStream();
			out = new FileOutputStream(outputFile);
			copy(in, out);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return outputFile;
	}

	protected static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[16 * 1024];
		int len;
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
	}

	protected static IProject getProject(String projectName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		return project;
	}

}
