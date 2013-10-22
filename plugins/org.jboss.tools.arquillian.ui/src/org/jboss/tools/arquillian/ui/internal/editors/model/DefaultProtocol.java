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
 * The model class of defaultProtocol for ArquillianEditor.
 *
 */
public class DefaultProtocol extends ArquillianModel {
	
	private DefaultProtocolElement element;
	
	public DefaultProtocol(Arquillian arquillian, DefaultProtocolElement element) {
		super(arquillian);
		this.element = element;
	}

	@Override
	protected void appendTo(Arquillian arquillian) {
		Element root = arquillian.getDocument().getDocumentElement();
		// defaultProtocol must be appended to the top.
		if(root.hasChildNodes()) {
			root.insertBefore(element.getNode(), root.getFirstChild());
		} else {
			root.appendChild(element.getNode());
		}
		arquillian.setDefaultProtocol(this);
	}
	
	@Override
	protected void removeFrom(Arquillian arquillian) {
		arquillian.getDocument().getDocumentElement().removeChild(element.getNode());
		arquillian.setDefaultProtocol(null);
	}
	
	@Override
	protected void apply(ArquillianModelInfo info) {
		DefaultProtocolInfo protocolInfo = (DefaultProtocolInfo) info;
		element.setDocument(arquillian.getDocument());
		element.setType(protocolInfo.getType());
		element.setProperties(protocolInfo.getProperties());
	}
	
	public String getType() {
		return element.getType();
	}
	
	public List<Property> getProperties() {
		return element.getProperties();
	}
}
