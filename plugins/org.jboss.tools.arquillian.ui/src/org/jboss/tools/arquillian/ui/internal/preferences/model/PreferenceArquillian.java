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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModelInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.model.GroupInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.Protocol;

/**
 * The model class of {@link Arquillian} for preference.
 *
 */
public class PreferenceArquillian extends Arquillian {
	
	public static final int TYPE_ALL = 0;
	public static final int TYPE_CONTAINER = 1;
	public static final int TYPE_GROUP = 2;

	private IPreferenceStore store;

	private int type;
	
	public PreferenceArquillian(IPreferenceStore store) {
		this(store, TYPE_ALL);
	}
	
	public PreferenceArquillian(IPreferenceStore store, int type) {
		super(null);
		this.store = store;
		this.type = type;
		containerGroups = new ArrayList<ContainerGroupModel>();
	}

	@Override
	public void init() {
		String xml = store.getString(ArquillianConstants.PREF_CONTAINER);
		if(StringUtils.isNotEmpty(xml)) {
			ArquillianXml arquillian = getArquillianXml(xml);
			if(arquillian != null) {
				for(ContainerXml container : arquillian.getContainer()) {
					containerGroups.add(convert(container));
				}
				for(GroupXml group : arquillian.getGroup()) {
					PreferenceGroup preferenceGroup = new PreferenceGroup(group.getQualifier());
					for(ContainerXml containerXml : group.getContainer()) {
						PreferenceContainer container = convert(containerXml);
						container.setGroup(preferenceGroup);
						preferenceGroup.addContainer(container);
					}
					containerGroups.add(preferenceGroup);
				}
			}
		}
		sort();
	}

	@Override
	public void add(ArquillianModelInfo info) {
		if(info instanceof ContainerInfo) {
			PreferenceContainer container = new PreferenceContainer();
			container.apply(info);
			container.appendTo(this);
		} else if(info instanceof GroupInfo) {
			PreferenceGroup group = new PreferenceGroup();
			group.apply(info);
			group.appendTo(this);
		}
		sort();
		save();
	}

	@Override
	public void edit(ArquillianModel model, ArquillianModelInfo info) {
		super.edit(model, info);
		sort();
		save();
	}

	@Override
	public void remove(ArquillianModel model) {
		super.remove(model);
		save();
	}
	
	protected ArquillianXml getArquillianXml(String xml) {
		try {
			return deserialize(xml);
		} catch (UnsupportedEncodingException e) {
			ArquillianUIActivator.log(e);
			return null;
		}
	}
	
	protected void sort() {
		Collections.sort(containerGroups, new Comparator<ContainerGroupModel>() {
			@Override
			public int compare(ContainerGroupModel o1,
					ContainerGroupModel o2) {
				return o1.getQualifier().compareTo(o2.getQualifier());
			}
		});
		for(ContainerGroupModel model : containerGroups) {
			if(model instanceof Group) {
				Collections.sort(((Group) model).getContainers(), new Comparator<Container>() {
					@Override
					public int compare(Container o1, Container o2) {
						return o1.getQualifier().compareTo(o2.getQualifier());
					}
				});
			}
		}
	}

	protected void save() {
		ArquillianXml arquillian = new ArquillianXml();
		for(ContainerGroupModel model : containerGroups) {
			if(model instanceof Container) {
				arquillian.getContainer().add(convert((Container) model));
			} else if(model instanceof Group) {
				Group group = (Group) model;
				GroupXml groupXml = new GroupXml();
				groupXml.setQualifier(group.getQualifier());
				for(Container container : group.getContainers()) {
					groupXml.getContainer().add(convert(container));
				}
				arquillian.getGroup().add(groupXml);
			}
		}
		store.setValue(ArquillianConstants.PREF_CONTAINER, serialize(arquillian));
	}
	
	private String serialize(ArquillianXml arquillian) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(out));
			try {
				encoder.writeObject(arquillian);
			} finally {
				encoder.close();
			}
			return new String(out.toByteArray(), "UTF-8");  //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			ArquillianUIActivator.log(e);
			return "";
		}
	}
	
	private ArquillianXml deserialize(String xml) throws UnsupportedEncodingException {
		// it sets argument's class loader for finding a class when called from plugin
		Thread thread = Thread.currentThread();
		ClassLoader loader = thread.getContextClassLoader();
		thread.setContextClassLoader(this.getClass().getClassLoader());
		Object object = null;
		try {
			XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes("UTF-8")));  //$NON-NLS-1$
			object = decoder.readObject();
			decoder.close();
		} finally {
			thread.setContextClassLoader(loader);
		}
		return ArquillianXml.class.cast(object);
	}

	public int getType() {
		return type;
	}
	
	protected PreferenceContainer convert(ContainerXml container) {
		PreferenceContainer preferenceContainer = 
				new PreferenceContainer(container.getQualifier(), container.getType(), container.getMode());
		for(ProtocolXml protocolXml : container.getProtocol()) {
			Protocol protocol = new Protocol(protocolXml.getType());
			for(PropertyXml property : protocolXml.getProperty()) {
				protocol.addProperty(new Property(property.getName(), property.getValue()));
			}
			preferenceContainer.getProtocols().add(protocol);
		}
		for(PropertyXml property : container.getConfiguration()) {
			preferenceContainer.getConfigurations().add(new Property(property.getName(), property.getValue()));
		}
		return preferenceContainer;
	}

	protected ContainerXml convert(Container container) {
		ContainerXml containerXml = new ContainerXml();
		containerXml.setQualifier(container.getQualifier());
		containerXml.setMode(container.getMode());
		containerXml.setType(container.getType());
		for(Protocol protocol : container.getProtocols()) {
			ProtocolXml protocolXml = new ProtocolXml();
			protocolXml.setType(protocol.getType());
			for(Property property : protocol.getProperties()) {
				PropertyXml propertyXml = new PropertyXml();
				propertyXml.setName(property.getName());
				propertyXml.setValue(property.getValue());
				protocolXml.getProperty().add(propertyXml);
			}
			containerXml.getProtocol().add(protocolXml);
		}
		for(Property property : container.getConfigurations()) {
			PropertyXml propertyXml = new PropertyXml();
			propertyXml.setName(property.getName());
			propertyXml.setValue(property.getValue());
			containerXml.getConfiguration().add(propertyXml);
		}
		return containerXml;
	}
	
}
