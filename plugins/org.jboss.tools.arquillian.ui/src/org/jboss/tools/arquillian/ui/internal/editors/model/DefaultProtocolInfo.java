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

import java.util.List;

import org.w3c.dom.Element;

/**
 * The information object for {@link DefaultProtocol}.
 * 
 */
public class DefaultProtocolInfo implements ArquillianModelInfo {

	private String type;
	private List<Property> properties;

	public DefaultProtocolInfo(String type, List<Property> properties) {
		this.type = type;
		this.properties = properties;
	}

	@Override
	public ArquillianModel generate(Arquillian arquillian) {
		Element element = arquillian.getDocument().createElement(ArquillianXmlElement.TAG_DEFAULT_PROTOCOL);
		return new DefaultProtocol(arquillian, new DefaultProtocolElement(element));
	}

	public String getType() {
		return type;
	}

	public List<Property> getProperties() {
		return properties;
	}

}
