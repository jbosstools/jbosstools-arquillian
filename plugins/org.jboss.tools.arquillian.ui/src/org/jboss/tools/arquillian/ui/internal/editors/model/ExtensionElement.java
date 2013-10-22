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
 * The model class of extension element.
 *
 */
public class ExtensionElement extends ArquillianXmlElement {

	public ExtensionElement(Element node) {
		super(node);
	}
	
	public String getQualifier() {
		return node.getAttribute(ATTR_QUALIFIER);
	}
	
	public void setQualifier(String qualifier) {
		node.setAttribute(ATTR_QUALIFIER, qualifier);
	}
	
	public List<Property> getProperties() {
		List<Property> properties = new ArrayList<Property>();
		NodeList nodeList = node.getElementsByTagName(TAG_PROPERTY);
		for(int i = 0; i < nodeList.getLength(); i++) {
			properties.add(new Property((Element) nodeList.item(i)));
		}
		return properties;
	}
	
	public void setProperties(List<Property> properties) {
		removeNodes(node, TAG_PROPERTY);
		if(properties == null) {
			return;
		}
		for(Property prop : properties) {
			if(StringUtils.isNotEmpty(prop.getValue())) {
				node.appendChild(createProperty(document, prop));
			}
		}
	}
	
}
