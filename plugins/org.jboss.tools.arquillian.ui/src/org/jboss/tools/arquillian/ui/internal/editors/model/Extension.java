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

import org.w3c.dom.DOMException;

/**
 * The model class of extension for ArquillianEditor.
 *
 */
public class Extension extends ArquillianModel {

	private ExtensionElement element;
	
	private List<Property> properties;
	
	public Extension(Arquillian arquillian, ExtensionElement element) {
		super(arquillian);
		this.element = element;
		properties = element.getProperties();
	}
	
	@Override
	public String getText() {
		return getQualifier();
	}
	
	@Override
	protected void appendTo(Arquillian arquillian) {
		arquillian.getDocument().getDocumentElement().appendChild(element.getNode());
		arquillian.getExtensions().add(this);
	}
	
	@Override
	protected void removeFrom(Arquillian arquillian) {
		try {
			arquillian.getDocument().getDocumentElement().removeChild(element.getNode());
		} catch(DOMException e) {
			if(e.code != DOMException.NOT_FOUND_ERR) {
				throw e;
			}
		}
		arquillian.getExtensions().remove(this);
	}
	
	public void apply(ArquillianModelInfo info) {
		ExtensionInfo extensionInfo = (ExtensionInfo) info;
		element.setDocument(arquillian.getDocument());
		element.setQualifier(extensionInfo.getQualifier());
		element.setProperties(extensionInfo.getProperties());
		properties = extensionInfo.getProperties();
	}
	
	public String getQualifier() {
		return element.getQualifier();
	}
	
	public List<Property> getProperties() {
		return properties;
	}
}
