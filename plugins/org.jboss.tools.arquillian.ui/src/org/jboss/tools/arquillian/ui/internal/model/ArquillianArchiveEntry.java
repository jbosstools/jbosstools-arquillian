/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.arquillian.core.internal.archives.Archive;
import org.jboss.tools.arquillian.core.internal.archives.Entry;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianArchiveEntry extends PlatformObject {

	private static final String PATH_SEPARATOR = "/";
	private String name;
	private boolean root;
	private boolean directory;
	private ArquillianArchiveEntry parent;
	private List<ArquillianArchiveEntry> entries = new ArrayList<ArquillianArchiveEntry>();
	private IJavaProject project;
	
	public ArquillianArchiveEntry(Archive archive, IJavaProject project) {
		Assert.isNotNull(archive);
		Assert.isNotNull(project);
		this.project = project;
//		StringBuffer buf = new StringBuffer();
//		buf.append(file.getParentFile().getName());
//		buf.append("("); //$NON-NLS-1$
//		int index = file.getName().indexOf("."); //$NON-NLS-1$
//		if (index < 0) {
//			buf.append(file.getName());
//		} else {
//			buf.append(file.getName().substring(index + 1));
//		}
//		buf.append(")"); //$NON-NLS-1$
		this.name = archive.getLocation().getMethodName();
		this.root = true;
		try {
			Map<String,ArquillianArchiveEntry> map = new HashMap<String, ArquillianArchiveEntry>();
			for (Entry e:archive.getEntries()) {
				ArquillianArchiveEntry entry = new ArquillianArchiveEntry(e.getFullName(), false, e.isDirectory(), project);
				map.put(e.getFullName(), entry);
			}
			Collection<ArquillianArchiveEntry> allEntries = map.values();
			for(ArquillianArchiveEntry entry:allEntries) {
				String[] names = entry.getName().split(PATH_SEPARATOR); //$NON-NLS-1$
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
						sb.append(PATH_SEPARATOR); //$NON-NLS-1$
					}
					ArquillianArchiveEntry p = map.get(sb.toString());
					if (p != null) {
						p.addEntry(entry);
						entry.setParent(p);
					}
				}
			}
		} catch (Exception e) {
			ArquillianUIActivator.log(e);
		} 
	}
	
	private ArquillianArchiveEntry(String name, boolean root, boolean directory, IJavaProject project) {
		this.name = name;
		this.root = root;
		this.directory = directory;
		this.project = project;
	}

	public String getName() {
		return name;
	}

	public Object[] getChildren() {
		return entries.toArray(new ArquillianArchiveEntry[0]);
	}

	public boolean isRoot() {
		return root;
	}

	private void addEntry(ArquillianArchiveEntry entry) {
		entries.add(entry);
	}

	public ArquillianArchiveEntry getParent() {
		return parent;
	}

	public void setParent(ArquillianArchiveEntry parent) {
		this.parent = parent;
	}

	public boolean isDirectory() {
		return directory;
	}

	public IJavaProject getProject() {
		return project;
	}
}
