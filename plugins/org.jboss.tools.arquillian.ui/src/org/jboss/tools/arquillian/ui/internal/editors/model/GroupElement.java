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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The model class of group element.
 *
 */
public class GroupElement extends ContainerGroupElement {

	public GroupElement(Element node) {
		super(node);
	}
	
	@Override
	public boolean isGroup() {
		return true;
	}
	
	public List<ContainerElement> getContainers() {
		Map<String, ContainerElement> elements = new LinkedHashMap<String, ContainerElement>();
		NodeList nodeList = node.getElementsByTagName(TAG_CONTAINER);
		for(int i = 0; i < nodeList.getLength(); i++) {
			ContainerElement container = new ContainerElement((Element) nodeList.item(i));
			container.setDocument(document);
			if(elements.containsKey(container.getQualifier())) {
				elements.remove(container.getQualifier());
			}
			elements.put(container.getQualifier(), container);
		}
		return new ArrayList<ContainerElement>(elements.values());
	}
	
	public void addContainer(ContainerElement element) {
		node.appendChild(element.getNode());
	}
	
	public void removeContainer(ContainerElement element) {
		node.removeChild(element.getNode());
	}
}
