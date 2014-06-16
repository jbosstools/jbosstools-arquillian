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
import org.eclipse.sapphire.ElementType;
import org.eclipse.sapphire.Unique;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.ValueProperty;
import org.eclipse.sapphire.modeling.annotations.Label;
import org.eclipse.sapphire.modeling.annotations.Required;
import org.eclipse.sapphire.modeling.annotations.Service;
import org.eclipse.sapphire.modeling.annotations.Services;
import org.eclipse.sapphire.modeling.xml.annotations.XmlBinding;
import org.jboss.tools.arquillian.editor.internal.services.PropertyNamePossibleValuesService;

/**
 * 
 * @author snjeza
 * 
 */
public interface Property extends Element {

	ElementType TYPE = new ElementType(Property.class);

	@Label(standard = "Name")
	@XmlBinding(path = "@name")
	@Required
	@Unique
	@Services ({
		@Service( impl = PropertyNamePossibleValuesService.class ),
	})
	ValueProperty PROP_NAME = new ValueProperty(TYPE, "Name"); //$NON-NLS-1$

	Value<String> getName();
	void setName(String name);

	@Label(standard = "Value")
	@XmlBinding(path = "")
	ValueProperty PROP_VALUE = new ValueProperty(TYPE, "Value"); //$NON-NLS-1$

	Value<String> getValue();
	void setValue(String value);

}
