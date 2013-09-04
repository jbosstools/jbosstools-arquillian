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
 * The information object for {@link Extension}.
 * 
 */
public class ExtensionInfo implements ArquillianModelInfo {

	private String qualifier;
	private List<Property> properties;

	public ExtensionInfo(String qualifier, List<Property> properties) {
		this.qualifier = qualifier;
		this.properties = properties;
	}

	@Override
	public ArquillianModel generate(Arquillian arquillian) {
		Element element = arquillian.getDocument().createElement(ArquillianXmlElement.TAG_EXTENSION);
		return new Extension(arquillian, new ExtensionElement(element));
	}

	public String getQualifier() {
		return qualifier;
	}

	public List<Property> getProperties() {
		return properties;
	}

}
