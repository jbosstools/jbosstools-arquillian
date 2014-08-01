/*******************************************************************************
 * Copyright (c) 2013-2014 JBoss by Red Hat and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.archives;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

/**
 * 
 * @author snjeza
 * 
 */
public class Entry implements IEntry {
	private String type;
	private String path;
	private String source;
	private IEntry parent;
	private Set<IEntry> children = new LinkedHashSet<IEntry>();
	private IJavaProject javaProject;
	private String fullyQualifiedName;
	private String name;

	public Entry(IEntry parent, String type, String path, String source, IJavaProject javaProject, String fullyQualifiedName) {
		this.parent = parent;
		this.type = type;
		this.path = path;
		this.source = source;
		this.javaProject = javaProject;
		this.fullyQualifiedName = fullyQualifiedName;
		if (path != null) {
			int index = path.lastIndexOf(Archive.PATH_SEPARATOR);
			if (index >= 0) {
				this.name = path.substring(index + 1);
			}
		}
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public IEntry getParent() {
		return parent;
	}

	public void setParent(IEntry parent) {
		this.parent = parent;
	}
	
	@Override
	public Set<IEntry> getChildren() {
		return children;
	}

	@Override
	public void add(IEntry entry) {
		children.add(entry);
	}

	@Override
	public boolean isDirectory() {
		return Archive.DIRECTORY.equals(type);
	}

	@Override
	public String getName() {
		return name == null ? "?" : name; //$NON-NLS-1$
	}

	@Override
	public IJavaProject getJavaProject() {
		return javaProject;
	}
	
	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entry other = (Entry) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Entry [path=" + path + ", type=" + type + ", parent=" + parent
				+ ", children=" + children + ", source=" + source + "]";
	}

}
