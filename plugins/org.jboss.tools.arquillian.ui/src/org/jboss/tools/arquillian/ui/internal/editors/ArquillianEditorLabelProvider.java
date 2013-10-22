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
package org.jboss.tools.arquillian.ui.internal.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.Extension;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;

/**
 * The label provider class for {@link ArquillianEditor}.
 *
 */
public class ArquillianEditorLabelProvider extends ColumnLabelProvider {
	
	private static final ImageDescriptor IMG_DESC_CONTAINER = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/container.gif");
	private static final ImageDescriptor IMG_DESC_GROUP = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/group.gif");
	private static final ImageDescriptor IMG_DESC_EXTENSION = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/extension.gif");

	@Override
	public String getText(Object element) {
		if(element instanceof ArquillianModel) {
			return ((ArquillianModel) element).getText();
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof Group) {
			return ArquillianUIActivator.getImage(IMG_DESC_GROUP);
		} else if (element instanceof Container) {
			return ArquillianUIActivator.getImage(IMG_DESC_CONTAINER);
		} else if (element instanceof Extension) {
			return ArquillianUIActivator.getImage(IMG_DESC_EXTENSION);
		}
		return super.getImage(element);
	}
}
