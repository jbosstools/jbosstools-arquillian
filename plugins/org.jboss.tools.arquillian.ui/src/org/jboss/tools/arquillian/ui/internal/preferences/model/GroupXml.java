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
package org.jboss.tools.arquillian.ui.internal.preferences.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The model class of group element for preference store.
 *
 */
public class GroupXml {

	private String qualifier;
	private List<ContainerXml> container;
	
	public GroupXml(){}
	
	public String getQualifier() {
		return qualifier;
	}
	
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	
	public List<ContainerXml> getContainer() {
		if(container == null) {
			container = new ArrayList<ContainerXml>();
		}
		return container;
	}
	
	public void setContainer(List<ContainerXml> container) {
		this.container = container;
	}
}
