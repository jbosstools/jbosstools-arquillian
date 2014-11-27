/*************************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 *  A refactoring to fix archive name
 *  
 * @author Snjeza
 *
 */
public class FixArchiveFileLocationRefactoring extends Refactoring {

	private IMarker marker;
	private String newMethodName;
	private String oldMethodName;
	private int offset;
	private int len;
	
	/**
	 * @param marker
	 */
	public FixArchiveFileLocationRefactoring(IMarker marker) {
		super();
		this.marker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
	public String getName() {
		return "Fix Archive File Location";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		boolean invalidMarker = false;
		if (marker == null || !(marker.getResource() instanceof IFile ) || !ArquillianConstants.MARKER_INVALID_ARCHIVE_FILE_LOCATION_ID.equals(marker.getType())) {
			invalidMarker = true;
		} else {
			offset = marker.getAttribute(IMarker.CHAR_START, 0);
		    int end = marker.getAttribute(IMarker.CHAR_END, 0);
		    len = end - offset;
		    oldMethodName = marker.getAttribute(ArquillianConstants.OLD_METHOD_NAME, null);
		    newMethodName = marker.getAttribute(ArquillianConstants.NEW_METHOD_NAME, null);
		}
		if (invalidMarker) {
			IStatus status = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, "Invalid marker");
			return RefactoringStatus.create(status);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		IFile file = getFile();
	    TextFileChange result = new TextFileChange( file.getName(), file );
	    MultiTextEdit rootEdit = new MultiTextEdit();
	    result.setEdit( rootEdit );    
	    
	    ReplaceEdit edit = new ReplaceEdit( offset, len, newMethodName);
	    rootEdit.addChild( edit );
	    return result;
	}

	private IFile getFile() {
		IFile file = (IFile) marker.getResource();
		return file;
	}

	public String getNewMethodName() {
		return newMethodName;
	}

	public void setNewMethodName(String newMethodName) {
		this.newMethodName = newMethodName;
	}

	public String getOldMethodName() {
		return oldMethodName;
	}

}
