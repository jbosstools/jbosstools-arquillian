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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jboss.tools.common.jdt.debug.RemoteDebugActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianSourcePathComputer implements
		ISourcePathComputerDelegate {

	@Override
	public ISourceContainer[] computeSourceContainers(
			ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		ILaunchConfiguration tempConfiguration = RemoteDebugActivator.createTemporaryLaunchConfiguration(projectName, RemoteDebugActivator.JDT_JUNIT_TEST);
		IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedSourceLookupPath(tempConfiguration);
		IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath(entries, tempConfiguration);
		tempConfiguration.delete();
		return JavaRuntime.getSourceContainers(resolved);
	}

}
