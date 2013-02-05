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
package org.jboss.tools.arquillian.core.internal.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianLaunchConfigurationDelegate extends
		JUnitLaunchConfigurationDelegate {

	public static final String ID = ArquillianCoreActivator.PLUGIN_ID + ".launchconfig";

	@Override
	protected IMember[] evaluateTests(ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		IMember[] tests = super.evaluateTests(configuration, monitor);
		String testMethodName= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, ""); //$NON-NLS-1$
		if (testMethodName.length() > 0) {
			return tests;
		}
		List<IMember> filteredTests = new ArrayList<IMember>();
		for (IMember member:tests) {
			if (member instanceof IType && ArquillianSearchEngine.isArquillianJUnitTest(member, true, true)) {
				filteredTests.add(member);
			}
		}
		
		return filteredTests.toArray(new IMember[0]);
	}

	@Override
	protected void preLaunchCheck(ILaunchConfiguration configuration,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.preLaunchCheck(configuration, launch, monitor);
		IStatus status = ArquillianSearchEngine.validateDeployableContainer(getJavaProject(configuration));
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

}
