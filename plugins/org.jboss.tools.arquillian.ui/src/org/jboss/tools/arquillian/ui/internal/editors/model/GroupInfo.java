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
 * The information object for {@link Group}.
 * 
 */
public class GroupInfo implements ArquillianModelInfo {

	private String qualifier;
	private List<Container> preferenceContainers;

	public GroupInfo(String qualifier) {
		this.qualifier = qualifier;
	}

	public GroupInfo(String qualifier, List<Container> preferenceContainers) {
		this.qualifier = qualifier;
		this.preferenceContainers = preferenceContainers;
	}

	public String getQualifier() {
		return qualifier;
	}

	public List<Container> getPreferenceContainers() {
		return preferenceContainers;
	}

	@Override
	public ArquillianModel generate(Arquillian arquillian) {
		Element element = arquillian.getDocument().createElement(ArquillianXmlElement.TAG_GROUP);
		return new Group(arquillian, new GroupElement(element));
	}

}
