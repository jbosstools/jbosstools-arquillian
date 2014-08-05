/*******************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.dependencies;

/**
 * 
 * @author snjeza
 *
 */
public class TypeLocation {

	private int charStart;
	private int charEnd;
	private int lineNumber;

	public TypeLocation(int charStart, int charEnd, int lineNumber) {
		super();
		this.charStart = charStart;
		this.charEnd = charEnd;
		this.lineNumber = lineNumber;
	}

	public int getCharStart() {
		return charStart;
	}

	public void setCharStart(int charStart) {
		this.charStart = charStart;
	}

	public int getCharEnd() {
		return charEnd;
	}

	public void setCharEnd(int charEnd) {
		this.charEnd = charEnd;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lineNumber;
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
		TypeLocation other = (TypeLocation) obj;
		if (lineNumber != other.lineNumber)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TypeLocation [charStart=" + charStart + ", charEnd=" + charEnd
				+ ", lineNumber=" + lineNumber + "]";
	}

}
