/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal;

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
	
	public static final String ENABLE_DEFAULT_VM_ARGUMENTS = "enableDefaultVMArguments"; //$NON-NLS-1$
	
	public static final boolean ENABLE_DEFAULT_VM_ARGUMENTS_VALUE = false;
	
	public static final String ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG = "addDefaultVMArgumentstoJUnitTestNG"; //$NON-NLS-1$
	
	public static final boolean ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG_VALUE = false;
	
	public static final String DEFAULT_VM_ARGUMENTS = "defaultVMArguments"; //$NON-NLS-1$
	
	public static final String DEFAULT_VM_ARGUMENTS_VALUE = "-Darquillian.debug=true";  //$NON-NLS-1$
	
	public static final String ALLOW_OS_COMMAND = "allowOSCommand";   //$NON-NLS-1$
	
	public static final String ALLOW_SP_COMMAND = "allowSPCommand";   //$NON-NLS-1$
	
	public static final boolean ALLOW_OS_COMMAND_VALUE = true;
	
	public static final boolean ALLOW_SP_COMMAND_VALUE = true;
	
	public static final String SEVERITY_ERROR = "Error";
	public static final String SEVERITY_WARNING = "Warning";
	public static final String SEVERITY_IGNORE = "Ignore";
	
	public static final String[] SEVERITY_LEVELS = new String[] { SEVERITY_ERROR, SEVERITY_WARNING, SEVERITY_IGNORE} ;

	public static final String ARQUILLIAN_VERSION = "arquillianVersion"; //$NON-NLS-1$

	public static final String ARQUILLIAN_VERSION_DEFAULT = "1.1.2.Final"; //$NON-NLS-1$
	
	public static final String MARKER_CLASS_NAME = "markerClassName"; //$NON-NLS-1$

	public static final String MARKER_BASE_ID = "org.jboss.tools.arquillian.core.problem";  //$NON-NLS-1$
	
	public static final String MARKER_CLASS_ID = "org.jboss.tools.arquillian.core.problem.class";  //$NON-NLS-1$
	
	public static final String MARKER_RESOURCE_ID = "org.jboss.tools.arquillian.core.problem.resource";  //$NON-NLS-1$

	public static final String MARKER_MISSING_DEPLOYMENT_METHOD_ID = "org.jboss.tools.arquillian.core.problem.missingDeploymentMethod";  //$NON-NLS-1$

	public static final String MARKER_INVALID_ARCHIVE_NAME_ID = "org.jboss.tools.arquillian.core.problem.invalidArchiveName";  //$NON-NLS-1$

	public static final String MARKER_INVALID_ARCHIVE_FILE_LOCATION_ID = "org.jboss.tools.arquillian.core.problem.invalidArchiveFileLocation";  //$NON-NLS-1$

	public static final String MARKER_INVALID_DEPLOYMENT_METHOD_ID = "org.jboss.tools.arquillian.core.problem.invalidDeploymentMethod";  //$NON-NLS-1$

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
	
	public static final String INVALID_ARCHIVE_NAME = ArquillianCoreActivator.PLUGIN_ID + ".invalidArchiveName"; //$NON-NLS-1$
	
	public static final String MISSING_TEST_METHOD = ArquillianCoreActivator.PLUGIN_ID + ".missingTestMethod"; //$NON-NLS-1$
	
	public static final String TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = ArquillianCoreActivator.PLUGIN_ID + ".typeIsNotIncludedInAnyDeployment"; //$NON-NLS-1$
	
	public static final String IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT = ArquillianCoreActivator.PLUGIN_ID + ".importIsNotIncludedInAnyDeployment"; //$NON-NLS-1$
	
	public static final String DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED = ArquillianCoreActivator.PLUGIN_ID + ".deploymentArchiveCannotBeCreated"; //$NON-NLS-1$
	
	public static final String DEPLOYMENT_METHOD_HAS_TO_BE_STATIC_AND_PUBLIC = ".deploymentMethodHasToBeStaticAndPublic"; //$NON-NLS-1$

	public static final String INVALID_ARCHIVE_FILE_LOCATION = ".invalidArchiveFileLocation"; //$NON-NLS-1$

	public static final String SELECTED_ARQUILLIAN_PROFILES = "selectedArquillianProfiles"; //$NON-NLS-1$
	
	public static final String ACTIVATED_ARQUILLIAN_PROFILES = "activatedArquillianProfiles"; //$NON-NLS-1$
	
	public static final String JBOSS_AS_REMOTE_7_X = "JBOSS_AS_REMOTE_7.X"; //$NON-NLS-1$

	public static final String COMMA = ","; //$NON-NLS-1$
	
	public static final String MAVEN_PROFILES_UI_PLUGIN_ID = "org.eclipse.m2e.profiles.ui"; //$NON-NLS-1$
	
	public static final String SELECT_MAVEN_PROFILES_COMMAND = "org.eclipse.m2e.profiles.ui.commands.selectMavenProfileCommand"; //$NON-NLS-1$
	
	public static final String ADD_ARQUILLIAN_PROFILES_COMMAND = "org.jboss.tools.arquillian.ui.action.addArquillianProfiles"; //$NON-NLS-1$
	
	public static final String ADD_ARQUILLIAN_SUPPORT_COMMAND = "org.jboss.tools.arquillian.ui.action.addArquillianSupport"; //$NON-NLS-1$
	
	public static final String ADD_ARQUILLIAN_SUPPORT = "Add Arquillian Support";
	
	public static final String ADD_ARQUILLIAN_PROFILES = "Add Arquillian Profiles";
	
	public static final String SELECT_MAVEN_PROFILES = "Select Maven Profiles";
	
	public static final String JUNIT_LAUNCHCONFIG_TYPE_ID = "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$

	public static final String TESTNG_LAUNCHCONFIG_TYPE_ID = "org.testng.eclipse.launchconfig"; //$NON-NLS-1$

	public static final String OLD_ARCHIVE_NAME = "oldArchiveName"; //$NON-NLS-1$
	
	public static final String OLD_METHOD_NAME = "oldMethodName"; //$NON-NLS-1$
	
	public static final String NEW_METHOD_NAME = "newMethodName"; //$NON-NLS-1$

	public static final String ARCHIVE_EXTENSION = "archiveExtension"; //$NON-NLS-1$
	
	public static final String JAR = "jar"; //$NON-NLS-1$
	public static final String WAR = "war"; //$NON-NLS-1$
	public static final String EAR = "ear"; //$NON-NLS-1$
	public static final String RAR = "rar"; //$NON-NLS-1$

	public static final String FIRST_START = "firstStart"; //$NON-NLS-1$
	
	public static final int MODIFIER_TYPE_UNKNOWN = 3;
	public static final String MODIFIER_TYPE = "modifierType"; //$NON-NLS-1$

	public static final String METHOD_NAME = "methodName"; //$NON-NLS-1$
	
	public static final String APPLICATION_XML = "application.xml"; //$NON-NLS-1$
	public static final String EJB_JAR_XML = "ejb-jar.xml"; //$NON-NLS-1$
	public static final String PERSISTENCE_XML = "persistence.xml"; //$NON-NLS-1$
	public static final String BEANS_XML = "beans.xml"; //$NON-NLS-1$
	public static final String WEB_FRAGMENT_XML = "web-fragment.xml"; //$NON-NLS-1$
	public static final String WEB_XML = "web.xml"; //$NON-NLS-1$
	
	public static final String ADD_AS_WEB_INF_RESOURCE = "addAsWebInfResource"; //$NON-NLS-1$
	public static final String ADD_AS_MANIFEST_RESOURCE = "addAsManifestResource";  //$NON-NLS-1$

}
