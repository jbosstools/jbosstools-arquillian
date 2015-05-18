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
import org.eclipse.sapphire.Length;
import org.eclipse.sapphire.ListProperty;
import org.eclipse.sapphire.Type;
import org.eclipse.sapphire.Unique;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.ValueProperty;
import org.eclipse.sapphire.modeling.annotations.DefaultValue;
import org.eclipse.sapphire.modeling.annotations.Fact;
import org.eclipse.sapphire.modeling.annotations.Facts;
import org.eclipse.sapphire.modeling.annotations.Label;
import org.eclipse.sapphire.modeling.annotations.Required;
import org.eclipse.sapphire.modeling.xml.annotations.XmlBinding;
import org.eclipse.sapphire.modeling.xml.annotations.XmlListBinding;

/**
 * 
 * @author snjeza
 * 
 */
public interface Group extends Element {

	ElementType TYPE = new ElementType(Group.class);

	@Label(standard = "Qualifier")
	@XmlBinding(path = "@qualifier")
	@Facts ( {
		@Fact (statement = "Must be unique between containers and groups."),
		@Fact (statement = "Used to select which container to run.")
	})
	@Required
	@Unique
	ValueProperty PROP_QUALIFIER = new ValueProperty(TYPE, "Qualifier"); //$NON-NLS-1$

	Value<String> getQualifier();
	void setQualifier(String qualifier);
	
	@Type( base = Boolean.class )
	@Label(standard = "Default")
	@XmlBinding(path = "@default")
	@DefaultValue(text ="false")
	ValueProperty PROP_DEFAULT = new ValueProperty(TYPE, "Default"); //$NON-NLS-1$

	Value<Boolean> getDefault();
	void setDefault( String value );
    void setDefault( Boolean value );
    
	@Type( base = Container.class )
    @Label( standard = "Container" )
	@Length(min=1)
    @XmlListBinding( mappings = @XmlListBinding.Mapping( element = "container", type = Container.class ))
    ListProperty PROP_CONTAINER = new ListProperty( TYPE, "Container" ); //$NON-NLS-1$ 

	ElementList<Container> getContainers();
}
