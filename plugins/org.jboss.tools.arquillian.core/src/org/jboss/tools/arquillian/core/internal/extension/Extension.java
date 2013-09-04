/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Nippon Telegraph and Telephone Corporation - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.extension;

import java.util.List;

import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.forge.arquillian.container.Dependency;

public class Extension {

	private String qualifier;
	private String group_id;
    private String artifact_id;
    private List<Dependency> dependencies;
    private List<Dependency> managementDependencies;
    private List<Configuration> configurations;
    
	public String getQualifier() {
		return qualifier;
	}
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	
	public String getGroup_id() {
		return group_id;
	}
	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}
	public String getArtifact_id() {
		return artifact_id;
	}
	public void setArtifact_id(String artifact_id) {
		this.artifact_id = artifact_id;
	}
	public List<Dependency> getDependencies() {
		return dependencies;
	}
	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}
	public List<Dependency> getManagementDependencies() {
		return managementDependencies;
	}
	public void setManagementDependencies(List<Dependency> managementDependencies) {
		this.managementDependencies = managementDependencies;
	}
	public List<Configuration> getConfigurations() {
		return configurations;
	}
	public void setConfigurations(List<Configuration> configurations) {
		this.configurations = configurations;
	}
	
    
}
