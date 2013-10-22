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
 * The model class of arquillian element for preference store.
 *
 */
public class ArquillianXml {
	
	private List<ContainerXml> container;
	private List<GroupXml> group;
	
	public ArquillianXml(){
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
	
	public List<GroupXml> getGroup() {
		if(group == null) {
			group = new ArrayList<GroupXml>();
		}
		return group;
	}
	
	public void setGroup(List<GroupXml> group) {
		this.group = group;
	}
	
}
