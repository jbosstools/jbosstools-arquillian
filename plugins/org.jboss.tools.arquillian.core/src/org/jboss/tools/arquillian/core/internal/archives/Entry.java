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

import org.eclipse.core.runtime.Assert;

/**
 * 
 * @author snjeza
 * 
 */
public class Entry {
	private static final String PERIOD = ".";
	private static final String CLASS_SUFFIX = ".class";
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String PATH_SEPARATOR = "/"; //$NON-NLS-1$
	private boolean directory;
	private String fullName;
	private boolean valid;
	private String fqn;

	public Entry(String fullName) {
		Assert.isNotNull(fullName);
		this.fullName = fullName.trim();
		this.valid = this.fullName.startsWith(PATH_SEPARATOR);
		if (valid) {
			this.directory = this.fullName.endsWith(PATH_SEPARATOR);
			this.fullName = this.fullName.substring(PATH_SEPARATOR.length());
		}
	}

	public boolean isDirectory() {
		return directory;
	}
	
	public String getFullName() {
		return fullName;
	}

	public boolean isValid() {
		return valid;
	}
	
	public String getFullyQualifiedName() {
		if (isDirectory() || !isValid() || !fullName.endsWith(CLASS_SUFFIX)) {
			return null;
		}
		if (fqn == null) {
			fqn = fullName.substring(0, fullName.length() - CLASS_SUFFIX.length());
			fqn = fqn.replace(PATH_SEPARATOR, PERIOD);
		}
		return fqn;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fullName == null) ? 0 : fullName.hashCode());
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
		if (fullName == null) {
			if (other.fullName != null)
				return false;
		} else if (!fullName.equals(other.fullName))
			return false;
		return true;
	}

}
