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

package org.jboss.tools.arquillian.core.internal.dependencies;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author snjeza
 *
 */
public class DependencyType {

	private String name;
	private Set<TypeLocation> locations = new HashSet<TypeLocation>();

	public DependencyType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<TypeLocation> getLocations() {
		return locations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DependencyType other = (DependencyType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DependencyType [name=" + name + ", locations=" + locations
				+ "]";
	}
}
