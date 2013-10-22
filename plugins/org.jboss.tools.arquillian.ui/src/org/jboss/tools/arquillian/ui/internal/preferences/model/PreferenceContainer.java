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
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.Protocol;

/**
 * The model class of container for preference GUI.
 *
 */
public class PreferenceContainer extends Container {

	private String qualifier;
	private String type;
	private String mode;
	private List<Protocol> protocols;
	private List<Property> configurations;

	public PreferenceContainer(){}

	public PreferenceContainer(String qualifier, String type, String mode) {
		this.qualifier = qualifier;
		this.type = type;
		this.mode = mode;
	}

	@Override
	public String getText() {
		return qualifier + " - " + type;
	}

	@Override
	protected void apply(ArquillianModelInfo info) {
		ContainerInfo containerInfo = (ContainerInfo) info;
		setGroup(containerInfo.getGroup());
		qualifier = containerInfo.getQualifier();
		type = containerInfo.getType();
		mode = containerInfo.getMode();
		protocols = containerInfo.getProtocols();
		configurations = containerInfo.getConfigurations();
	}

	@Override
	protected void appendTo(Arquillian arquillian) {
		if(getGroup() == null) {
			arquillian.getContainerGroups().add(this);
		} else {
			getGroup().addContainer(this);
		}
	}

	@Override
	protected void removeFrom(Arquillian arquillian) {
		if(getGroup() == null) {
			arquillian.getContainerGroups().remove(this);
		} else {
			getGroup().removeContainer(this);
		}
	}

	@Override
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public List<Protocol> getProtocols() {
		if(protocols == null) {
			protocols = new ArrayList<Protocol>();
		}
		return protocols;
	}

	@Override
	public List<Property> getConfigurations() {
		if(configurations == null) {
			configurations = new ArrayList<Property>();
		}
		return configurations;
	}

}
