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
import org.eclipse.sapphire.modeling.annotations.CountConstraint;
import org.eclipse.sapphire.modeling.annotations.Label;
import org.eclipse.sapphire.modeling.annotations.Service;
import org.eclipse.sapphire.modeling.annotations.Type;
import org.eclipse.sapphire.modeling.xml.annotations.XmlBinding;
import org.eclipse.sapphire.modeling.xml.annotations.XmlListBinding;
import org.eclipse.sapphire.modeling.xml.annotations.XmlNamespace;
import org.eclipse.sapphire.modeling.xml.annotations.XmlSchema;
import org.jboss.tools.arquillian.editor.internal.services.UniqueQualifierBetweenContainerAndGroupService;

/**
 * 
 * @author snjeza
 *
 */
@XmlNamespace( prefix = "", uri = "http://jboss.org/schema/arquillian" )
@XmlSchema
(
    namespace = "http://jboss.org/schema/arquillian",
    location = "http://jboss.org/schema/arquillian/arquillian_1_0.xsd"
)
@XmlBinding( path = "arquillian" )
public interface Arquillian extends Element {
	
	ElementType TYPE = new ElementType( Arquillian.class );
    
	@Type( base = Protocol.class )
    @Label( standard = "Default Protocol" )
    @XmlBinding( path = "defaultProtocol" )
	@CountConstraint (max=1)
    ImpliedElementProperty PROP_DEFAULT_PROTOCOL = 
    	new ImpliedElementProperty( TYPE, "DefaultProtocol" ); //$NON-NLS-1$ 

	Protocol getDefaultProtocol();
	
	@Type( base = Engine.class )
    @Label( standard = "Engine" )
    @XmlBinding( path = "engine" )
	@CountConstraint (max=1)
    ImpliedElementProperty PROP_ENGINE = 
    	new ImpliedElementProperty( TYPE, "Engine" ); //$NON-NLS-1$ 

	Engine getEngine();
	
	@Type( base = Container.class )
    @Label( standard = "Container" )
	@Service( impl=UniqueQualifierBetweenContainerAndGroupService.class)
	@XmlListBinding( mappings = @XmlListBinding.Mapping( element = "container", type = Container.class ))
    ListProperty PROP_CONTAINER = new ListProperty( TYPE, "Container" ); //$NON-NLS-1$ 

	ElementList<Container> getContainers();
	
	@Type( base = Group.class )
    @Label( standard = "Group" )
	@Service( impl=UniqueQualifierBetweenContainerAndGroupService.class)
	@XmlListBinding( mappings = @XmlListBinding.Mapping( element = "group", type = Group.class ))
    ListProperty PROP_GROUP = new ListProperty( TYPE, "Group" ); //$NON-NLS-1$ 

	ElementList<Group> getGroups();
	
	@Type( base = Extension.class )
    @Label( standard = "Extension" )
    @XmlListBinding( mappings = @XmlListBinding.Mapping( element = "extension", type = Extension.class ))
    ListProperty PROP_EXTENSION = new ListProperty( TYPE, "Extension" ); //$NON-NLS-1$ 

	ElementList<Extension> getExtensions();
	
}
