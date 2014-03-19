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

import org.eclipse.sapphire.modeling.annotations.EnumSerialization;
import org.eclipse.sapphire.modeling.annotations.Label;

/**
 * 
 * @author snjeza
 *
 */
@Label(standard = "Type")
public enum ProtocolType {

	@Label( standard = "Local" )
	@EnumSerialization( primary = "Local" )

    LOCAL,
    
    @Label( standard = "Servlet 2.5" )
	@EnumSerialization( primary = "Servlet 2.5" )

    SERVLET_2_5,
    
    @Label( standard = "Servlet 3.0" )
	@EnumSerialization( primary = "Servlet 3.0" )

    SERVLET_3_0,

    @Label( standard = "jmx-as7" )
	@EnumSerialization( primary = "jmx-as7" )

    JMX_AS7,

}
