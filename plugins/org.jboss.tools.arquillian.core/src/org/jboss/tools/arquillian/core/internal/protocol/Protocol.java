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
package org.jboss.tools.arquillian.core.internal.protocol;

import java.util.List;

import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.forge.arquillian.container.Dependency;

public class Protocol {

	private String type;
	private String group_id;
    private String artifact_id;
    private List<Configuration> configurations;
	
    public String getType() {
		return type;
	}
    
    public void setType(String type) {
		this.type = type;
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
    
    public List<Configuration> getConfigurations() {
		return configurations;
	}
    
    public void setConfigurations(List<Configuration> configurations) {
		this.configurations = configurations;
	}
    
}
