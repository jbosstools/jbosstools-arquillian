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
public enum ModeType {

	@Label( standard = "Suite" )
	@EnumSerialization( primary = "suite" )

    SUITE,
    
    @Label( standard = "Class" )
	@EnumSerialization( primary = "class" )

    CLASS,
    
    @Label( standard = "Manual" )
	@EnumSerialization( primary = "manual" )

    MANUAL,

    @Label( standard = "Custom" )
	@EnumSerialization( primary = "custom" )

    CUSTOM,

}
