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

import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModelInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.model.GroupInfo;

/**
 * The model class of group for preference GUI.
 *
 */
public class PreferenceGroup extends Group {

	private String qualifier;

	public PreferenceGroup() {
		this(null);
	}

	public PreferenceGroup(String qualifier) {
		this.qualifier = qualifier;
		containers = new ArrayList<Container>();
	}

	@Override
	public String getText() {
		return qualifier;
	}

	@Override
	protected void apply(ArquillianModelInfo info) {
		GroupInfo groupInfo = (GroupInfo) info;
		qualifier = groupInfo.getQualifier();
	}

	@Override
	public void appendTo(Arquillian arquillian) {
		arquillian.getContainerGroups().add(this);
	}

	@Override
	protected void removeFrom(Arquillian arquillian) {
		arquillian.getContainerGroups().remove(this);
	}

	@Override
	public void addContainer(Container container) {
		containers.add(container);
	}

	@Override
	public void removeContainer(Container container) {
		containers.remove(container);
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public List<Container> getContainers() {
		return containers;
	}

}
