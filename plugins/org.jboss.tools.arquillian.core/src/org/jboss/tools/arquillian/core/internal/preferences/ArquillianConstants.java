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
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianConstants {
	
	public static final String ENABLE_ARQUILLIAN_VALIDATOR = "enableArquillianValidator"; //$NON-NLS-1$
	
	public static final String TEST_ARQUILLIAN_CONTAINER = "testArquillianContainer"; //$NON-NLS-1$
	
	public static final boolean ENABLE_ARQUILLIAN_VALIDATOR_DEFAULT = true;
	
	public static final String SEVERITY_ERROR = "Error";
	public static final String SEVERITY_WARNING = "Warning";
	public static final String SEVERITY_IGNORE = "Ignore";
	
	public static final String[] SEVERITY_LEVELS = new String[] { SEVERITY_ERROR, SEVERITY_WARNING, SEVERITY_IGNORE} ;

	public static final String ARQUILLIAN_VERSION = "arquillianVersion"; //$NON-NLS-1$

	public static final String ARQUILLIAN_VERSION_DEFAULT = "1.0.3.Final"; //$NON-NLS-1$
	
	public static final String MARKER_CLASS_NAME = "markerClassName"; //$NON-NLS-1$

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

	public static final Object SOURCE_ID ="Arquillian"; //$NON-NLS-1$
	
	public static final String MISSING_DEPLOYMENT_METHOD = ArquillianCoreActivator.PLUGIN_ID + ".missingDeploymentMethod"; //$NON-NLS-1$
	
	public static final String MISSING_TEST_METHOD = ArquillianCoreActivator.PLUGIN_ID + ".missingTestMethod"; //$NON-NLS-1$
	
	public static final String TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = ArquillianCoreActivator.PLUGIN_ID + ".typeIsNotIncludedInAnyDeployment"; //$NON-NLS-1$
	
	public static final String IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = ArquillianCoreActivator.PLUGIN_ID + ".importIsNotIncludedInAnyDeployment"; //$NON-NLS-1$
	
	public static final String DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED = ArquillianCoreActivator.PLUGIN_ID + ".deploymentArchiveCannotBeCreated"; //$NON-NLS-1$
	
}
