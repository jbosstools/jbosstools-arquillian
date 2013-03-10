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
package org.jboss.tools.arquillian.core.internal.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathProvider;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;
import org.jboss.tools.common.jdt.debug.RemoteDebugActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianRuntimeClasspathProvider extends StandardClasspathProvider {

	public static final String ID = "org.jboss.tools.arquillian.core.launchconfig.classpathProvider"; //$NON-NLS-1$
	
	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
			ILaunchConfiguration configuration) throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		ILaunchConfiguration tempConfiguration = RemoteDebugActivator.createTemporaryLaunchConfiguration(projectName, RemoteDebugActivator.JDT_JUNIT_TEST);
		IRuntimeClasspathProvider provider = JavaRuntime.getClasspathProvider(tempConfiguration);
		IRuntimeClasspathEntry[] unresolved = provider.computeUnresolvedClasspath(tempConfiguration);
		tempConfiguration.delete();
		if (unresolved.length > 0) {
			return unresolved;
		}
		return super.computeUnresolvedClasspath(configuration);
	}

	@Override
	public IRuntimeClasspathEntry[] resolveClasspath(
			IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
			throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		ILaunchConfiguration tempConfiguration = RemoteDebugActivator.createTemporaryLaunchConfiguration(projectName, RemoteDebugActivator.JDT_JUNIT_TEST);
		IRuntimeClasspathProvider provider = JavaRuntime.getClasspathProvider(tempConfiguration);
		IRuntimeClasspathEntry[] resolved = provider.resolveClasspath(entries, tempConfiguration);
		tempConfiguration.delete();
		if (resolved.length > 0) {
			return resolved;
		}
		return super.resolveClasspath(entries, configuration);
	}

	

}
