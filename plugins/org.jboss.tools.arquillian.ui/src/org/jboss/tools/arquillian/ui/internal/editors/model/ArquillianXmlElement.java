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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The base class for element of arquillian.xml
 *
 */
public abstract class ArquillianXmlElement {
	
	public static final String TAG_ARQUILLIAN = "arquillian";
	public static final String TAG_CONTAINER = "container";
	public static final String TAG_GROUP = "group";
	public static final String TAG_EXTENSION = "extension";
	public static final String TAG_DEFAULT_PROTOCOL = "defaultProtocol";
	public static final String TAG_ENGINE = "engine";
	public static final String TAG_PROTOCOL = "protocol";
	public static final String TAG_PROPERTY = "property";
	public static final String TAG_CONFIGURATION = "configuration";
	
	public static final String ATTR_QUALIFIER = "qualifier";
	public static final String ATTR_MODE = "mode";
	public static final String ATTR_DEFAULT = "default";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_DEACTIVE = "deactive";
	public static final String ATTR_CONFIGURED = "configured";
	
	public static final String PROP_DEPLOYMENT_EXPORT_PATH = "deploymentExportPath";
	public static final String PROP_MAX_TEST_CLASSES_BEFORE_RESTART = "maxTestClassesBeforeRestart";

	protected Document document;
	protected Element node;
	
	public ArquillianXmlElement(Element node) {
		this.node = node;
	}
	
	public Element getNode() {
		return node;
	}
	
	public void setDocument(Document document) {
		this.document = document;
	}
	
	/**
	 * Creates the property element with the given {@link Property}.
	 * @param document the document
	 * @param property the {@link Property}
	 * @return the created property element
	 */
	protected Element createProperty(Document document, Property property) {
		return createProperty(document, property.getName(), property.getValue());
	}
	
	/**
	 * Creates the property element with the given name and value.
	 * @param document the document
	 * @param name the name
	 * @param value the value
	 * @return the created property element
	 */
	protected Element createProperty(Document document, String name, String value) {
		Element property = document.createElement(ArquillianXmlElement.TAG_PROPERTY);
		property.setAttribute(ArquillianXmlElement.ATTR_NAME, name);
		property.appendChild(document.createTextNode(value));
		return property;
	}
	
	/**
	 * Removes nodes from the given node by the given tag name.
	 * @param node the node
	 * @param tagName the tag name
	 */
	protected void removeNodes(Element node, String tagName) {
		NodeList nodeList = node.getElementsByTagName(tagName);
		for(int i = nodeList.getLength(); i > 0; i--) {
			Node target = nodeList.item(i - 1);
			if(node == target.getParentNode()) {
				node.removeChild(target);
			}
		}
	}
}
