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
 * The content provider class for extension.
 *
 */
public class ExtensionContentProvider extends AbstractTreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof Arquillian) {
			Arquillian arquillian = (Arquillian) parentElement;
			return arquillian.getExtensions().toArray(new Extension[arquillian.getExtensions().size()]);
		}
		return new Object[0];
	}

}
