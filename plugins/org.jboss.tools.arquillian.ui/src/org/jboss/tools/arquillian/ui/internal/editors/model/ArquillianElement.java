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
 * The model class of arquillian element.
 *
 */
public class ArquillianElement extends ArquillianXmlElement {

	public ArquillianElement(Element node) {
		super(node);
	}
	
	/**
	 * Returns the list of {@link ContainerGroupElement} by child element of root element(not descendent element).
	 * @return the list of {@link ContainerGroupElement}
	 */
	public List<ContainerGroupElement> getContainerGroups() {
		Map<String, ContainerGroupElement> elements = new LinkedHashMap<String, ContainerGroupElement>();
		NodeList nodeList = node.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++) {
			if(!(nodeList.item(i) instanceof Element)) {
				continue;
			}
			Element node = (Element) nodeList.item(i);
			if(TAG_CONTAINER.equals(node.getNodeName())) {
				// exclude container in group
				if(node.getParentNode().getNodeName().equals(TAG_GROUP)) {
					continue;
				}
				ContainerElement element = new ContainerElement(node);
				if(elements.containsKey(element.getQualifier())) {
					elements.remove(element.getQualifier());
				}
				elements.put(element.getQualifier(), element);
			} else if(TAG_GROUP.equals(node.getNodeName())) {
				GroupElement element = new GroupElement(node);
				if(elements.containsKey(element.getQualifier())) {
					elements.remove(element.getQualifier());
				}
				elements.put(element.getQualifier(), element);
			}
		}
		return new ArrayList<ContainerGroupElement>(elements.values());
	}
	
	public List<ExtensionElement> getExtensions() {
		Map<String, ExtensionElement> extensions = new LinkedHashMap<String, ExtensionElement>();
		NodeList nodeList = node.getElementsByTagName(TAG_EXTENSION);
		for(int i = 0; i < nodeList.getLength(); i++) {
			ExtensionElement extensionElement = new ExtensionElement((Element) nodeList.item(i));
			if(extensions.containsKey(extensionElement.getQualifier())) {
				extensions.remove(extensionElement.getQualifier());
			}
			extensions.put(extensionElement.getQualifier(), extensionElement);
		}
		return new ArrayList<ExtensionElement>(extensions.values());
	}
	
	public DefaultProtocolElement getDefaultProtocol() {
		NodeList nodeList = node.getElementsByTagName(TAG_DEFAULT_PROTOCOL);
		if(nodeList.getLength() > 0) {
			return new DefaultProtocolElement((Element) nodeList.item(0));
		}
		return null;
	}
	
	public EngineElement getEngine() {
		NodeList nodeList = node.getElementsByTagName(TAG_ENGINE);
		if(nodeList.getLength() > 0) {
			return new EngineElement((Element) nodeList.item(0));
		}
		return null;
	}
}
