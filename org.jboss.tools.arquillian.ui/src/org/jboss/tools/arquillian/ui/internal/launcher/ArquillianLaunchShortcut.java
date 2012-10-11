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
package org.jboss.tools.arquillian.ui.internal.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.jboss.tools.arquillian.core.internal.classpath.ArquillianRuntimeClasspathProvider;
import org.jboss.tools.arquillian.core.internal.launcher.ArquillianLaunchConfigurationDelegate;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianLaunchShortcut extends JUnitLaunchShortcut {

	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
			IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(element);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, ArquillianRuntimeClasspathProvider.ID);
		return config;
	}

	@Override
	protected String getLaunchConfigurationTypeId() {
		return ArquillianLaunchConfigurationDelegate.ID;
	}

}
