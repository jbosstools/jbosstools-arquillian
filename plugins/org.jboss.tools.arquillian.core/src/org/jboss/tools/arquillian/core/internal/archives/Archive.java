/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.archives;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 
 * @author snjeza
 * 
 */
public class Archive {

	private String description;
	private ArchiveLocation location;
	private String type;
	private Set<Entry> entries;
	public static final Archive EMPTY_ARCHIVE = new Archive(null, null, null);
	

	public Archive(String description, ArchiveLocation location, String type) {
		this.description = description;
		this.location = location;
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArchiveLocation getLocation() {
		return location;
	}

	public void setLocation(ArchiveLocation location) {
		this.location = location;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Set<Entry> getEntries() {
		if (entries == null) {
			entries = new LinkedHashSet<Entry>();
			if (description != null) {
				String[] elements = description.split("\n"); //$NON-NLS-1$
				for(String element:elements) {
					Entry entry = new Entry(element);
					if (entry.isValid()) {
						entries.add(entry);
					}
				}
			}
		}
		return entries;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((location == null) ? 0 : location.hashCode());
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
		Archive other = (Archive) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Archive [description=" + description + ", location=" + location
				+ ", type=" + type + "]";
	}

}
