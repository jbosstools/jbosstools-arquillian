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
 * The base model class of container and group.
 *
 */
public abstract class ContainerGroupModel extends ArquillianModel {

	public ContainerGroupModel(){}
	
	public ContainerGroupModel(Arquillian arquillian) {
		super(arquillian);
	}
	
	public abstract String getQualifier();
	
	public abstract boolean isDefault();
	
	public abstract ArquillianXmlElement getElement();
	
	public abstract void setDefault(boolean isDefault);
	
	public void insertBefore(ContainerGroupModel model) {
		clear(model);
		arquillian.insertBefore(model, this);
	}
	
	public void insertAfter(ContainerGroupModel model) {
		clear(model);
		arquillian.insertAfter(model, this);
	}
	
	private void clear(ContainerGroupModel model) {
		if(model instanceof Container) {
			Container container = (Container) model;
			if(container.getGroup() != null) {
				container.setDefault(false);
				container.getGroup().removeContainer(container);
			}
		}
	}
}
