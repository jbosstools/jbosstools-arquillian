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
import org.eclipse.sapphire.ImpliedElementProperty;
import org.eclipse.sapphire.ListProperty;
import org.eclipse.sapphire.Unique;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.ValueProperty;
import org.eclipse.sapphire.modeling.annotations.CountConstraint;
import org.eclipse.sapphire.modeling.annotations.DefaultValue;
import org.eclipse.sapphire.modeling.annotations.Label;
import org.eclipse.sapphire.modeling.annotations.Required;
import org.eclipse.sapphire.modeling.annotations.Service;
import org.eclipse.sapphire.modeling.annotations.Services;
import org.eclipse.sapphire.modeling.annotations.Type;
import org.eclipse.sapphire.modeling.xml.annotations.XmlBinding;
import org.eclipse.sapphire.modeling.xml.annotations.XmlListBinding;
import org.jboss.tools.arquillian.editor.internal.services.ContainerProfilesService;
import org.jboss.tools.arquillian.editor.internal.services.ContainerQualifierPossibleValuesService;

/**
 * 
 * @author snjeza
 * 
 */
public interface Container extends Element {

	ElementType TYPE = new ElementType(Container.class);

	@Label(standard = "Qualifier")
	@XmlBinding(path = "@qualifier")
	@Required
	@Unique
	@Services ( {
		@Service( impl = ContainerQualifierPossibleValuesService.class ),
		@Service( impl = ContainerProfilesService.class )
		
	})
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
    
	@Label(standard = "Mode")
	@XmlBinding(path = "@mode")
	@Type(base = ModeType.class)
	ValueProperty PROP_MODE = new ValueProperty(TYPE, "Mode"); //$NON-NLS-1$

	Value<String> getMode();
	void setMode(String mode);

	@Type(base = Configuration.class)
	@Label(standard = "Configuration")
	@CountConstraint (max=1)
	@XmlBinding( path = "configuration" )
	ImpliedElementProperty PROP_CONFIGURATION = 
		new ImpliedElementProperty( TYPE, "Configuration" ); //$NON-NLS-1$ 

	Configuration getConfiguration();
		
	@Type( base = Protocol.class )
    @Label( standard = "Protocol" )
    @XmlListBinding( mappings = @XmlListBinding.Mapping( element = "protocol", type = Protocol.class ))
    ListProperty PROP_PROTOCOL = new ListProperty( TYPE, "Protocol" ); //$NON-NLS-1$ 

	ElementList<Protocol> getProtocols();
}
