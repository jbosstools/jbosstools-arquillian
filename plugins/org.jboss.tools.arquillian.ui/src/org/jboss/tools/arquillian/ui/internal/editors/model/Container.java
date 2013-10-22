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

/**
 * The model class of container for ArquillianEditor.
 *
 */
public class Container extends ContainerGroupModel {

	private Group group;
	private ContainerElement element;
	
	private List<Protocol> protocols;
	private List<Property> configurations;
	
	public Container() {}
	
	public Container(Arquillian arquillian, ContainerElement element) {
		this(arquillian, element, null);
	}
	
	public Container(Arquillian arquillian, ContainerElement element, Group group) {
		super(arquillian);
		this.element = element;
		this.group = group;
		protocols = element.getProtocols();
		configurations = element.getConfigurations();
	}
	
	@Override
	public String getText() {
		String text = getQualifierPrefix() + " - " + getType();
		if(isDefault()) {
			text = text + " (default)";
		}
		return text;
	}
	
	@Override
	protected void apply(ArquillianModelInfo info) {
		ContainerInfo containerInfo = (ContainerInfo) info;
		
		if(element != null) {
			element.setDocument(arquillian.getDocument());
			element.setQualifier(containerInfo.getQualifier() + "-" + containerInfo.getType());
			element.setMode(containerInfo.getMode());
			element.setProtocols(containerInfo.getProtocols());
			element.setConfigurations(containerInfo.getConfigurations());
		}
		
		protocols = containerInfo.getProtocols();
		configurations = containerInfo.getConfigurations();
	}
	
	@Override
	protected void appendTo(Arquillian arquillian) {
		if(isGroupContainer()) {
			group.addContainer(this);
		} else {
			arquillian.getDocument().getDocumentElement().appendChild(element.getNode());
			arquillian.getContainerGroups().add(this);
			// checks whether a default container exists.
			arquillian.checkDefaultContainerGroup();
		}
	}
	
	@Override
	protected void removeFrom(Arquillian arquillian) {
		if(isGroupContainer()) {
			group.removeContainer(this);
		} else {
			if(element != null) {
				arquillian.getDocument().getDocumentElement().removeChild(element.getNode());
			}
			arquillian.getContainerGroups().remove(this);
			// checks whether a default container exists.
			arquillian.checkDefaultContainerGroup();
		}
	}
	
	@Override
	public void insertBefore(ContainerGroupModel model) {
		if(isGroupContainer()) {
			if(model instanceof Container) {
				group.insertBefore((Container) model, this);
			}
		} else {
			super.insertBefore(model);
		}
	}
	
	@Override
	public void insertAfter(ContainerGroupModel model) {
		if(isGroupContainer()) {
			if(model instanceof Container) {
				group.insertAfter((Container) model, this);
			}
		} else {
			super.insertAfter(model);
		}
	}
	
	@Override
	public ContainerElement getElement() {
		return element;
	}
	
	@Override
	public String getQualifier() {
		return element.getQualifier();
	}
	
	public String getQualifierPrefix() {
		String qualifier = getQualifier();
		int index = qualifier.lastIndexOf("-");
		if(index > 0) {
			return qualifier.substring(0, qualifier.lastIndexOf("-"));
		} else {
			return qualifier;
		}
	}
	
	@Override
	public boolean isDefault() {
		return element.isDefault();
	}
	
	public String getMode() {
		return element.getMode();
	}
	
	public List<Protocol> getProtocols() {
		return protocols;
	}
	
	public List<Property> getConfigurations() {
		return configurations;
	}
	
	/**
	 * Sets a default attribute to specified flag.
	 * If flag is true, then it sets a default attribute of other element to false.
	 * 
	 * @param isDefault default flag
	 */
	@Override
	public void setDefault(boolean isDefault) {
		if(isDefault) {
			// first, sets all containers to false.
			if(isGroupContainer()) {
				for(Container container : group.getContainers()) {
					container.setDefault(false);
				}
			} else {
				for(ContainerGroupModel model : arquillian.getContainerGroups()) {
					model.setDefault(false);
				}
			}
		}
		element.setDefault(isDefault);
	}
	
	public boolean isGroupContainer() {
		return group != null;
	}
	
	public String getType() {
		String qualifier = getQualifier();
		int index = qualifier.lastIndexOf("-");
		if( index > 0) {
			return qualifier.substring(qualifier.lastIndexOf("-") + 1);
		} else {
			return "";
		}
	}
	
	public Group getGroup() {
		return group;
	}
	
	public void setGroup(Group group) {
		this.group = group;
	}
}
