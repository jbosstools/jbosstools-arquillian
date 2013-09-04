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
package org.jboss.tools.arquillian.ui.internal.preferences.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The model class of container element for preference store.
 *
 */
public class ContainerXml {

	private String qualifier;
	private String type;
	private String mode;
	private List<ProtocolXml> protocol;
	private List<PropertyXml> configuration;
	
	public ContainerXml(){}
	
	public String getQualifier() {
		return qualifier;
	}
	
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getMode() {
		return mode;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public List<ProtocolXml> getProtocol() {
		if(protocol == null) {
			protocol = new ArrayList<ProtocolXml>();
		}
		return protocol;
	}
	
	public void setProtocol(List<ProtocolXml> protocol) {
		this.protocol = protocol;
	}
	
	public List<PropertyXml> getConfiguration() {
		if(configuration == null) {
			configuration = new ArrayList<PropertyXml>();
		}
		return configuration;
	}
	
	public void setConfiguration(List<PropertyXml> configuration) {
		this.configuration = configuration;
	}
}
