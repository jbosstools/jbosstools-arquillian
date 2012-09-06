package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.core.runtime.IPath;

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
