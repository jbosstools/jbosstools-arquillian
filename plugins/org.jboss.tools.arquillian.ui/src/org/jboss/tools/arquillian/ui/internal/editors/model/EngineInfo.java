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


/**
 * The information object for {@link Engine}.
 *
 */
public class EngineInfo implements ArquillianModelInfo {

	private Property property;
	
	public EngineInfo(Property property) {
		this.property = property;
	}
	
	@Override
	public ArquillianModel generate(Arquillian arquillian) {
		return new Engine(
					arquillian,
					new EngineElement(arquillian.getDocument().createElement(ArquillianXmlElement.TAG_ENGINE)));
	}
	
	public Property getProperty() {
		return property;
	}
}
