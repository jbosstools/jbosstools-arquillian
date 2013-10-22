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
import org.w3c.dom.NodeList;

/**
 * The model class of engine for ArquillianEditor.
 *
 */
public class Engine extends ArquillianModel {
	
	private EngineElement element;
	
	public Engine(Arquillian arquillian, EngineElement element) {
		super(arquillian);
		this.element = element;
	}
	
	@Override
	protected void apply(ArquillianModelInfo info) {
		EngineInfo engineInfo = (EngineInfo) info;
		element.setDocument(arquillian.getDocument());
		element.setProperty(engineInfo.getProperty());
		if(!element.hasChildren()) {
			removeFrom(arquillian);
		}
	}
	
	@Override
	protected void appendTo(Arquillian arquillian) {
		Element root = arquillian.getDocument().getDocumentElement();
		// engine must be appended to after defaultProtocol
		NodeList defaultProtocol = root.getElementsByTagName(ArquillianXmlElement.TAG_DEFAULT_PROTOCOL);
		if(defaultProtocol.getLength() > 0) {
			if(root.getChildNodes().getLength() == defaultProtocol.getLength()) {
				root.appendChild(element.getNode());
			} else {
				root.insertBefore(element.getNode(), defaultProtocol.item(defaultProtocol.getLength() - 1).getNextSibling());
			}
		} else {
			if(root.hasChildNodes()) {
				root.insertBefore(element.getNode(), root.getFirstChild());
			} else {
				root.appendChild(element.getNode());
			}
		}
		arquillian.setEngine(this);
	}
	
	@Override
	protected void removeFrom(Arquillian arquillian) {
		arquillian.getDocument().getDocumentElement().removeChild(element.getNode());
		arquillian.setEngine(null);
	}
	
	public String getDeploymentExportPath() {
		return element.getDeploymentExportPath();
	}
	
	public String getMaxTestClassesBeforeRestart() {
		return element.getMaxTestClassesBeforeRestart();
	}
}
