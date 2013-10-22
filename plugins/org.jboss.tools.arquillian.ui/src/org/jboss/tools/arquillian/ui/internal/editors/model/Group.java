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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

/**
 * The model class of group for ArquillianEditor.
 *
 */
public class Group extends ContainerGroupModel {
	
	protected List<Container> containers;
	
	private GroupElement element;
	
	public Group(){}
	
	public Group(Arquillian arquillian, GroupElement element) {
		super(arquillian);
		this.element = element;
		initContainer();
	}
	
	protected void initContainer() {
		containers = new ArrayList<Container>();
		for(ContainerElement container : element.getContainers()) {
			containers.add(new Container(arquillian, container, this));
		}
	}

	@Override
	public String getText() {
		String text = getQualifier();
		if(isDefault()) {
			text = text + " (default)";
		}
		return text;
	}
	
	@Override
	public void appendTo(Arquillian arquillian) {
		arquillian.getDocument().getDocumentElement().appendChild(element.getNode());
		arquillian.getContainerGroups().add(this);
		// checks whether a default container exists.
		arquillian.checkDefaultContainerGroup();
	}
	
	@Override
	protected void removeFrom(Arquillian arquillian) {
		if(element != null) {
			arquillian.getDocument().getDocumentElement().removeChild(element.getNode());
		}
		arquillian.getContainerGroups().remove(this);
		// checks whether a default container exists.
		arquillian.checkDefaultContainerGroup();
	}
	
	public void insertBefore(Container container, Container refContainer) {
		insert(container, refContainer, true);
		element.getNode().insertBefore(container.getElement().getNode(), refContainer.getElement().getNode());
	}
	
	public void insertAfter(Container container, Container refContainer) {
		insert(container, refContainer, false);
		Node nextNode = refContainer.getElement().getNode().getNextSibling();
		if(nextNode != null) {
			element.getNode().insertBefore(container.getElement().getNode(), nextNode);
		} else {
			element.getNode().appendChild(container.getElement().getNode());
		}
	}
	
	protected void insert(Container container, Container refContainer, boolean before) {
		boolean isDefault = container.getGroup() == this && container.isDefault();
		arquillian.remove(container);
		if(container.getGroup() != null && container.getGroup() != this) {
			container.getGroup().removeContainer(container);
		}
		container.setDefault(false);
		container.setGroup(this);
		if(isDefault) {
			container.setDefault(true);
		}
			
		// rebuild for ordering
		List<Container> containers = new ArrayList<Container>();
		for(Container c : this.containers) {
			if(!before) {
				containers.add(c);
			}
			if(c == refContainer) {
				containers.add(container);
			}
			if(before) {
				containers.add(c);
			}
		}
		this.containers = containers;
		checkDefaultContainer();
	}
	
	@Override
	protected void apply(ArquillianModelInfo info) {
		GroupInfo groupInfo = (GroupInfo) info;
		if(element != null) {
			element.setDocument(arquillian.getDocument());
			element.setQualifier(groupInfo.getQualifier());
		}
		if(groupInfo.getPreferenceContainers() != null) {
			for(int i = containers.size(); i > 0; i--) {
				arquillian.remove(containers.get(i - 1));
			}
			for(Container preferenceContainer : groupInfo.getPreferenceContainers()) {
				arquillian.add(new ContainerInfo(
										this,
										preferenceContainer.getQualifier(),
										preferenceContainer.getType(),
										preferenceContainer.getMode(),
										preferenceContainer.getConfigurations(),
										preferenceContainer.getProtocols()
									));
			}
		}
	}
	
	@Override
	public GroupElement getElement() {
		return element;
	}
	
	@Override
	public String getQualifier() {
		return element.getQualifier();
	}
	
	@Override
	public boolean isDefault() {
		return element.isDefault();
	}
	
	@Override
	public void setDefault(boolean isDefault) {
		if(isDefault) {
			for(ContainerGroupModel model : arquillian.getContainerGroups()) {
				model.setDefault(false);
			}
		}
		element.setDefault(isDefault);
	}
	
	public void addContainer(Container container) {
		if(element != null && container.getElement() != null) {
			element.addContainer(container.getElement());
		}
		containers.add(container);
		checkDefaultContainer();
	}
	
	public void removeContainer(Container container) {
		if(element != null && container.getElement() != null) {
			element.removeContainer(container.getElement());
		}
		containers.remove(container);
		container.setGroup(null);
		checkDefaultContainer();
	}
	
	public List<Container> getContainers() {
		return containers;
	}
	
	public Container getContainer(String qualifier) {
		for(Container container : containers) {
			if(qualifier.equals(container.getQualifier())) {
				return container;
			}
		}
		return null;
	}
	
	protected void checkDefaultContainer() {
		for(Container container : containers) {
			if(container.isDefault()) {
				return;
			}
		}
		if(containers.size() > 0) {
			containers.get(0).setDefault(true);
		}
	}
	
}
