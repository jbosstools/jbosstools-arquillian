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
package org.jboss.tools.arquillian.ui.internal.editors.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The model class of protocol element.
 *
 */
public class Protocol {

	private String type;
	
	private List<Property> properties;
	
	public Protocol(Element element) {
		type = element.getAttribute(ArquillianXmlElement.ATTR_TYPE);
		properties = new ArrayList<Property>();
		NodeList props = element.getElementsByTagName(ArquillianXmlElement.TAG_PROPERTY);
		for(int i = 0; i < props.getLength(); i++) {
			properties.add(new Property((Element) props.item(i)));
		}
	}
	
	public Protocol(Protocol protocol) {
		this(protocol.type);
		properties.addAll(protocol.properties);
	}
	
	public Protocol(String type) {
		this.type = type;
		properties = new ArrayList<Property>();
	}
	
	public void addProperty(Property property) {
		properties.add(property);
	}
	
	public String getType() {
		return type;
	}
	
	public List<Property> getProperties() {
		return properties;
	}
}
