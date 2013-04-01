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
package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.core.runtime.IPath;

/**
 * 
 * @author snjeza
 *
 */
public class ProjectResource {

	private IPath path;
	private boolean deployAsWebInfResource;

	public ProjectResource(IPath path, boolean deployAsWebInfResource) {
		this.path = path;
		this.deployAsWebInfResource = deployAsWebInfResource;
	}

	public IPath getPath() {
		return path;
	}

	public void setPath(IPath path) {
		this.path = path;
	}

	public boolean isDeployAsWebInfResource() {
		return deployAsWebInfResource;
	}

	public void setDeployAsWebInfResource(boolean deployAsWebInfResource) {
		this.deployAsWebInfResource = deployAsWebInfResource;
	}

}
