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
 * The information object for {@link Container}.
 * 
 */
public class ContainerInfo implements ArquillianModelInfo {

	private Group group;
	private String qualifier;
	private String type;
	private String mode;
	private List<Property> configurations;
	private List<Protocol> protocols;

	public ContainerInfo(Group group, String qualifier, String type, String mode, List<Property> configurations,
			List<Protocol> protocols) {
		this.group = group;
		this.qualifier = qualifier;
		this.type = type;
		this.mode = mode;
		this.configurations = configurations;
		this.protocols = protocols;
	}

	@Override
	public ArquillianModel generate(Arquillian arquillian) {
		Element element = arquillian.getDocument().createElement(ArquillianXmlElement.TAG_CONTAINER);
		return new Container(arquillian, new ContainerElement(element), group);
	}

	public Group getGroup() {
		return group;
	}

	public String getQualifier() {
		return qualifier;
	}

	public String getMode() {
		return mode;
	}

	public String getType() {
		return type;
	}

	public List<Property> getConfigurations() {
		return configurations;
	}

	public List<Protocol> getProtocols() {
		return protocols;
	}

}
