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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

/** 
 * 
 * @author snjeza
 *
 */
public class ArquillianPreferencesInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = ArquillianCoreActivator.getDefault().getPreferenceStore();
		preferences.setDefault(ArquillianConstants.ARQUILLIAN_SEVERITY_LEVEL, ArquillianConstants.ARQUILLIAN_SEVERITY_LEVEL_DEFAULT);
	}

}
