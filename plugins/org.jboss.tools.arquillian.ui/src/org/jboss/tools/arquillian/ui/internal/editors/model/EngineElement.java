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

import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The model class of engine element.
 *
 */
public class EngineElement extends ArquillianXmlElement {

	private Element deploymentExportPath;
	private Element maxTestClassesBeforeRestart;
	
	public EngineElement(Element node) {
		super(node);
		NodeList nodeList = node.getElementsByTagName(TAG_PROPERTY);
		for(int i = 0; i < nodeList.getLength(); i++) {
			Element element = (Element) nodeList.item(i);
			Property property = new Property(element);
			if(PROP_DEPLOYMENT_EXPORT_PATH.equals(property.getName())) {
				deploymentExportPath = element;
				continue;
			}
			if(PROP_MAX_TEST_CLASSES_BEFORE_RESTART.equals(property.getName())) {
				maxTestClassesBeforeRestart = element;
				continue;
			}
		}
	}
	
	public void setProperty(Property property) {
		if(PROP_DEPLOYMENT_EXPORT_PATH.equals(property.getName())) {
			setDeploymentExportPath(property);
		} else if(PROP_MAX_TEST_CLASSES_BEFORE_RESTART.equals(property.getName())) {
			setMaxTestClassesBeforeRestart(property);
		}
	}
	
	private void setDeploymentExportPath(Property property) {
		if(StringUtils.isEmpty(property.getValue())) {
			if(deploymentExportPath != null) {
				node.removeChild(deploymentExportPath);
				deploymentExportPath = null;
			}
			return;
		}
		if(deploymentExportPath == null) {
			deploymentExportPath = createProperty(document, property);
			node.appendChild(deploymentExportPath);
		} else {
			deploymentExportPath.getFirstChild().setNodeValue(property.getValue());
		}
	}
	
	private void setMaxTestClassesBeforeRestart(Property property) {
		if(StringUtils.isEmpty(property.getValue())) {
			if(maxTestClassesBeforeRestart != null) {
				node.removeChild(maxTestClassesBeforeRestart);
				maxTestClassesBeforeRestart = null;
			}
			return;
		}
		if(maxTestClassesBeforeRestart == null) {
			maxTestClassesBeforeRestart = createProperty(document, property);
			node.appendChild(maxTestClassesBeforeRestart);
		} else {
			maxTestClassesBeforeRestart.getFirstChild().setNodeValue(property.getValue());
		}
	}
	
	public String getDeploymentExportPath() {
		if(deploymentExportPath != null && deploymentExportPath.hasChildNodes()) {
			return deploymentExportPath.getFirstChild().getNodeValue();
		} else {
			return "";
		}
	}
	
	public String getMaxTestClassesBeforeRestart() {
		if(maxTestClassesBeforeRestart != null && maxTestClassesBeforeRestart.hasChildNodes()) {
			return maxTestClassesBeforeRestart.getFirstChild().getNodeValue();
		} else {
			return "";
		}
	}
	
	public boolean hasChildren() {
		return deploymentExportPath != null || maxTestClassesBeforeRestart != null;
	}
}
