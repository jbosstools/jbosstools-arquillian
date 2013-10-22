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

import org.w3c.dom.Element;

/**
 * The base class of container and group.
 *
 */
public abstract class ContainerGroupElement extends ArquillianXmlElement {

	public ContainerGroupElement(Element element) {
		super(element);
	}
	
	public String getQualifier() {
		return node.getAttribute(ATTR_QUALIFIER);
	}
	
	public void setQualifier(String qualifier) {
		node.setAttribute(ATTR_QUALIFIER, qualifier);
	}
	
	public boolean isDefault() {
		String attrDefault = node.getAttribute(ATTR_DEFAULT);
		return attrDefault != null && Boolean.valueOf(attrDefault);
	}
	
	public void setDefault(boolean isDefault) {
		node.setAttribute(ATTR_DEFAULT, Boolean.toString(isDefault));
	}
	
	public boolean isGroup() {
		return false;
	}
	
	public void copy(ContainerGroupElement target) {
		target.setQualifier(getQualifier());
		target.setDefault(isDefault());
	}
}
