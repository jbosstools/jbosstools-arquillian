/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;

/** 
 * 
 * @author snjeza
 *
 */
public class ArquillianPreferencesInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences preferences = DefaultScope.INSTANCE.getNode(ArquillianCoreActivator.PLUGIN_ID);
		preferences.putBoolean(ArquillianConstants.ENABLE_ARQUILLIAN_VALIDATOR, true);
		preferences.putBoolean(ArquillianConstants.TEST_ARQUILLIAN_CONTAINER, true);
		preferences.put(ArquillianConstants.MISSING_DEPLOYMENT_METHOD, JavaCore.WARNING);
		preferences.put(ArquillianConstants.INVALID_ARCHIVE_NAME, JavaCore.WARNING);
		preferences.put(ArquillianConstants.MISSING_TEST_METHOD, JavaCore.WARNING);
		preferences.put(ArquillianConstants.TYPE_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, JavaCore.WARNING);
		preferences.put(ArquillianConstants.IMPORT_IS_NOT_INCLUDED_IN_ANY_DEPLOYMENT, JavaCore.WARNING);
		preferences.put(ArquillianConstants.DEPLOYMENT_ARCHIVE_CANNOT_BE_CREATED, JavaCore.WARNING);
		preferences.put(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		preferences.put(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		preferences.putBoolean(ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS, ArquillianConstants.ENABLE_DEFAULT_VM_ARGUMENTS_VALUE);
		preferences.put(ArquillianConstants.DEFAULT_VM_ARGUMENTS, ArquillianConstants.DEFAULT_VM_ARGUMENTS_VALUE);
		preferences.putBoolean(ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG, ArquillianConstants.ADD_DEFAULT_VM_ARGUMENTS_TO_JUNIT_TESTNG_VALUE);
		preferences.putBoolean(ArquillianConstants.ALLOW_OS_COMMAND, ArquillianConstants.ALLOW_OS_COMMAND_VALUE);
		preferences.putBoolean(ArquillianConstants.ALLOW_SP_COMMAND, ArquillianConstants.ALLOW_SP_COMMAND_VALUE);
	}

}
