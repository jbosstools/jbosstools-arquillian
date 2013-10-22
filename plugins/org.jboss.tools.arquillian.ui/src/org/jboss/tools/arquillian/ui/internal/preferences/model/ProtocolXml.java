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
 * The model class of protocol element for preference store.
 *
 */
public class ProtocolXml {

	private String type;
	private List<PropertyXml> property;
	
	public ProtocolXml(){}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public List<PropertyXml> getProperty() {
		if(property == null) {
			property = new ArrayList<PropertyXml>();
		}
		return property;
	}
	
	public void setProperty(List<PropertyXml> property) {
		this.property = property;
	}
}
