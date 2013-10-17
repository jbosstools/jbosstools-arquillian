/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class RefactoringUtil {

	public static Shell getShell() {
		if (Display.getCurrent() != null) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		return null;
	}
	
	public static String getMissingClassName(IMarker marker) throws CoreException {
		boolean validMarker = ArquillianConstants.MARKER_CLASS_ID.equals(marker.getType());
		if (validMarker) {
			return marker.getAttribute(ArquillianConstants.MARKER_CLASS_NAME, null);
		}
		return null;
	}
	
	public static String getQuickFixName(IMarker marker) {
		String name;
		try {
			name = "Add " + RefactoringUtil.getMissingClassName(marker) + " to deployment";
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
			return null;
		}
		return name;
	}
}
