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
 * The content provider class for container and group.
 *
 */
public class ContainerGroupContentProvider extends AbstractTreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof Arquillian) {
			Arquillian arquillian = (Arquillian) parentElement;
			List<ContainerGroupModel> containerGroups = arquillian.getContainerGroups();
			return containerGroups.toArray(new ContainerGroupModel[containerGroups.size()]);
		} else if(parentElement instanceof Group) {
			List<Container> containers = ((Group) parentElement).getContainers();
			return containers.toArray(new Container[containers.size()]);
		}
		return new Object[0];
	}
	
}
