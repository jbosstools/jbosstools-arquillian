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
package org.jboss.tools.arquillian.editor.internal.model;

import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ElementList;
import org.eclipse.sapphire.ElementType;
import org.eclipse.sapphire.ListProperty;
import org.eclipse.sapphire.modeling.annotations.Label;
import org.eclipse.sapphire.modeling.annotations.Type;
import org.eclipse.sapphire.modeling.xml.annotations.XmlListBinding;

/**
 * 
 * @author snjeza
 * 
 */
public interface Engine extends Element {

	ElementType TYPE = new ElementType(Engine.class);

	@Type(base = Property.class)
	@Label(standard = "Property")
	@XmlListBinding(mappings = @XmlListBinding.Mapping(element = "property", type = Property.class))
	ListProperty PROP_PROPERTY = new ListProperty(TYPE, "Property"); //$NON-NLS-1$

	ElementList<Property> getProperties();
}
