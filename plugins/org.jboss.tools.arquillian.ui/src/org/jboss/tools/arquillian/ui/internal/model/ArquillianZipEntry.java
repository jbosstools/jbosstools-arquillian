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
package org.jboss.tools.arquillian.ui.internal.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianZipEntry extends PlatformObject {

	private String name;
	private boolean root;
	private boolean directory;
	private ArquillianZipEntry parent;
	private List<ArquillianZipEntry> entries = new ArrayList<ArquillianZipEntry>();
	private IJavaProject project;
	
	public ArquillianZipEntry(File file, IJavaProject project) {
		Assert.isNotNull(file);
		Assert.isLegal(file.exists());
		Assert.isNotNull(project);
		this.project = project;
		StringBuffer buf = new StringBuffer();
		buf.append(file.getParentFile().getName());
		buf.append("("); //$NON-NLS-1$
		int index = file.getName().indexOf("."); //$NON-NLS-1$
		if (index < 0) {
			buf.append(file.getName());
		} else {
			buf.append(file.getName().substring(index + 1));
		}
		buf.append(")"); //$NON-NLS-1$
		this.name = buf.toString();
		this.root = true;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			Map<String,ArquillianZipEntry> map = new HashMap<String, ArquillianZipEntry>();
			for (Enumeration<? extends ZipEntry> e= zipFile.entries(); e.hasMoreElements();) {
				ZipEntry member= (ZipEntry) e.nextElement();
				ArquillianZipEntry entry = new ArquillianZipEntry(member.getName(), false, member.isDirectory(), project);
				map.put(member.getName(), entry);
			}
			zipFile.close();
			Collection<ArquillianZipEntry> allEntries = map.values();
			for(ArquillianZipEntry entry:allEntries) {
				String[] names = entry.getName().split("/"); //$NON-NLS-1$
				if (names == null) {
					continue;
				}
				if (names.length <= 1) {
					entry.setParent(this);
					addEntry(entry);
				} else {
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < names.length - 1; i++) {
						sb.append(names[i]);
						sb.append("/"); //$NON-NLS-1$
					}
					ArquillianZipEntry p = map.get(sb.toString());
					if (p != null) {
						p.addEntry(entry);
						entry.setParent(p);
					}
				}
			}
		} catch (Exception e) {
			ArquillianUIActivator.log(e);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
					// ignore
				};
			}
		}
	}
	
	private ArquillianZipEntry(String name, boolean root, boolean directory, IJavaProject project) {
		this.name = name;
		this.root = root;
		this.directory = directory;
		this.project = project;
	}

	public String getName() {
		return name;
	}

	public Object[] getChildren() {
		return entries.toArray(new ArquillianZipEntry[0]);
	}

	public boolean isRoot() {
		return root;
	}

	private void addEntry(ArquillianZipEntry entry) {
		entries.add(entry);
	}

	public ArquillianZipEntry getParent() {
		return parent;
	}

	public void setParent(ArquillianZipEntry parent) {
		this.parent = parent;
	}

	public boolean isDirectory() {
		return directory;
	}

	public IJavaProject getProject() {
		return project;
	}
}
