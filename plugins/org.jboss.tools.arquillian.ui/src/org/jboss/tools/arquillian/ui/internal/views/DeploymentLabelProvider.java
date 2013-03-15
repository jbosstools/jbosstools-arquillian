/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.model.ArquillianZipEntry;
/**
 * 
 * @author snjeza
 *
 */
public class DeploymentLabelProvider implements IStyledLabelProvider, ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ArquillianZipEntry) {
			String fullName = ((ArquillianZipEntry) element).getName();
			if (fullName == null) {
				return null;
			}
			String[] names = fullName.split("/"); //$NON-NLS-1$
			if (names.length > 0) {
				return names[names.length - 1];
			}
		}
		return null;
	}

	@Override
	public StyledString getStyledText(Object element) {
		return new StyledString(getText(element));
	}

	@Override
	public Image getImage(Object element) {
		Image image = null;
		if (element instanceof ArquillianZipEntry) {
			ArquillianZipEntry entry = (ArquillianZipEntry) element;
			if (entry.isRoot()) {
				ImageDescriptor descriptor = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/jar_obj.gif"); //$NON-NLS-1$
				image = ArquillianUIActivator.getImage(descriptor);
			} else {
				if (entry.isDirectory()) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
				}
				ImageDescriptor descriptor = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(entry.getName());
				if (descriptor != null) {
					image = ArquillianUIActivator.getImage(descriptor);
				}
				if (image == null) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
				}
			}
		}
		return image;
	}

}
