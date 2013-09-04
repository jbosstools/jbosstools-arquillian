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

import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The model class of container element.
 *
 */
public class ContainerElement extends ContainerGroupElement {

	public ContainerElement(Element node) {
		super(node);
	}
	
	@Override
	public void copy(ContainerGroupElement target) {
		super.copy(target);
		ContainerElement element = (ContainerElement) target;
		element.setMode(getMode());
		element.setProtocols(getProtocols());
		element.setConfigurations(getConfigurations());
	}
	
	public String getMode() {
		return node.getAttribute(ATTR_MODE);
	}
	
	public void setMode(String mode) {
		if(StringUtils.isEmpty(mode)) {
			node.removeAttribute(ATTR_MODE);
		} else {
			node.setAttribute(ATTR_MODE, mode);
		}
	}
	
	public List<Protocol> getProtocols() {
		List<Protocol> protocols = new ArrayList<Protocol>();
		NodeList nodeList = node.getElementsByTagName(TAG_PROTOCOL);
		for(int i = 0; i < nodeList.getLength(); i++) {
			protocols.add(new Protocol((Element) nodeList.item(i)));
		}
		return protocols;
	}
	
	public void setProtocols(List<Protocol> protocols) {
		removeNodes(node, TAG_PROTOCOL);
		if(protocols == null) {
			return;
		}
		for(Protocol protocol : protocols) {
			Element protocolElem = document.createElement(TAG_PROTOCOL);
			protocolElem.setAttribute(ATTR_TYPE, protocol.getType());
			for(Property prop : protocol.getProperties()) {
				if(StringUtils.isNotEmpty(prop.getValue())) {
					protocolElem.appendChild(createProperty(document, prop));
				}
			}
			node.appendChild(protocolElem);
		}
	}
	
	public List<Property> getConfigurations() {
		List<Property> configurations = new ArrayList<Property>();
		NodeList nodeList = node.getElementsByTagName(TAG_CONFIGURATION);
		if(nodeList.getLength() > 0) {
			Element configuration = (Element) nodeList.item(0);
			NodeList props = configuration.getElementsByTagName(TAG_PROPERTY);
			for(int i = 0; i < props.getLength(); i++) {
				configurations.add(new Property((Element) props.item(i)));
			}
		}
		return configurations;
	}
	
	public void setConfigurations(List<Property> properties) {
		removeNodes(node, TAG_CONFIGURATION);
		if(properties == null) {
			return;
		}
		Element configuration = document.createElement(TAG_CONFIGURATION);
		for(Property prop : properties) {
			if(StringUtils.isNotEmpty(prop.getValue())) {
				configuration.appendChild(createProperty(document, prop));
			}
		}
		if(configuration.hasChildNodes()) {
			node.appendChild(configuration);
		}
	}

	public String getType() {
		return node.getAttribute(ATTR_TYPE);
	}
	
	public void setType(String type) {
		if(StringUtils.isEmpty(type)) {
			node.removeAttribute(ATTR_TYPE);
		} else {
			node.setAttribute(ATTR_TYPE, type);
		}
	}
}
