/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.editor.internal.utils;

import java.util.List;

import org.jboss.tools.arquillian.core.internal.container.ContainerParser;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianEditorUtils {

	public static org.jboss.forge.arquillian.container.Container getContainer(String id) {
		if (id != null) {
			List<org.jboss.forge.arquillian.container.Container> containers = ContainerParser.getContainers();
			for (org.jboss.forge.arquillian.container.Container container : containers) {
				if (id.equals(container.getId())) {
					return container;
				}
			}
		}
		return null;
	}
}
