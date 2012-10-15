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
package org.jboss.tools.arquillian.core.internal.preferences;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianConstants {
	
	public static final String ARQUILLIAN_SEVERITY_LEVEL = "arquillianSeverityLevel";

	public static final String SEVERITY_ERROR = "Error";
	public static final String SEVERITY_WARNING = "Warning";
	public static final String SEVERITY_IGNORE = "Ignore";
	
	public static final String ARQUILLIAN_SEVERITY_LEVEL_DEFAULT = SEVERITY_WARNING;

	public static final String[] SEVERITY_LEVELS = new String[] { SEVERITY_ERROR, SEVERITY_WARNING, SEVERITY_IGNORE} ;

	public static final String MARKER_CLASS_NAME = "markerClassName";

	public static final String MARKER_CLASS_ID = "org.jboss.tools.arquillian.core.problem.class";  //$NON-NLS-1$
	
	public static final String MARKER_RESOURCE_ID = "org.jboss.tools.arquillian.core.problem.resource";  //$NON-NLS-1$

    public final static String[] ARQUILLIAN_PROBLEM_MARKER_ATTRIBUTE_NAMES = {
    	IMarker.MESSAGE,
    	IMarker.SEVERITY,
    	IJavaModelMarker.ID,
    	IMarker.CHAR_START,
    	IMarker.CHAR_END,
    	IJavaModelMarker.CATEGORY_ID,
    	IMarker.SOURCE_ID,
    	ArquillianConstants.MARKER_CLASS_NAME,
    };

	public static final int ARQUILLIAN_PROBLEM_ID = 1;

	public static final Object SOURCE_ID ="Arquillian";
    
}
