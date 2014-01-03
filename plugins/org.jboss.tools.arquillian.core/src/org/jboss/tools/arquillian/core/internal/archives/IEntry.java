/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;

/**
 * 
 * @author snjeza
 *
 */
public interface IEntry {

	IEntry getParent();
	Set<IEntry> getChildren();
	void add(IEntry entry);
	String getType();
	boolean isDirectory();
	String getName();
	IJavaProject getJavaProject();
}
