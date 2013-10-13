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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;

/**
 * @see IMarkerResolutionGenerator2
 * 
 * @author snjeza
 *
 */
public class FixArchiveNameResolutionGenerator implements
		IMarkerResolutionGenerator2 {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IResource resource = marker.getResource();
		if (resource == null || !resource.exists() || ! (resource instanceof IFile)) {
			return null;
		}
		IJavaElement element = JavaCore.create(resource);
		if (element != null ) {
			return new IMarkerResolution[] {
				new FixArchiveNameMarkerResolution(marker)	
			};
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			return ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID.equals(marker.getType());
		} catch (CoreException e) {
			return false;
		}
	}

}
