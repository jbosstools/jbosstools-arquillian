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
package org.jboss.tools.arquillian.ui.internal.wizards;

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
public class FixArchiveNameRefactoring extends Refactoring {

	private IMarker marker;
	private Change change;
	private String newArchiveName;
	private String oldArchiveName;
	private String extension;
	private int offset;
	private int len;
	
	public FixArchiveNameRefactoring(IMarker marker) {
		super();
		this.marker = marker;
	}

	@Override
	public String getName() {
		return "Fix Archive Name";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		boolean invalidMarker = false;
		if (marker == null || !(marker.getResource() instanceof IFile ) || !ArquillianConstants.MARKER_INVALID_ARCHIVE_NAME_ID.equals(marker.getType())) {
			invalidMarker = true;
		} else {
			offset = marker.getAttribute(IMarker.CHAR_START, 0);
		    int end = marker.getAttribute(IMarker.CHAR_END, 0);
		    len = end - offset;
		    oldArchiveName = marker.getAttribute(ArquillianConstants.OLD_ARCHIVE_NAME, null);
		    extension = marker.getAttribute(ArquillianConstants.ARCHIVE_EXTENSION, null);
		    if (offset <= 0 || end <= 0 || len <= 0 
		    		|| oldArchiveName == null || oldArchiveName.isEmpty() 
		    		|| extension == null || extension.isEmpty()) {
		    	invalidMarker = true;
		    } else {
		    	int index = oldArchiveName.lastIndexOf(".");
		    	if (index == -1) {
		    		newArchiveName = oldArchiveName + extension;
		    	} else {
		    		newArchiveName = oldArchiveName.substring(0, index) + extension;
		    	}
		    }
		    
		}
		if (invalidMarker) {
			IStatus status = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, "Invalid marker");
			return RefactoringStatus.create(status);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	public String getExtension() {
		return extension;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
//		if (change == null) {
//			return RefactoringStatus.createErrorStatus("There are no refactorings to apply");
//		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		IFile file = getFile();
	    TextFileChange result = new TextFileChange( file.getName(), file );
	    MultiTextEdit rootEdit = new MultiTextEdit();
	    result.setEdit( rootEdit );    
	    
	    ReplaceEdit edit = new ReplaceEdit( offset, len, newArchiveName);
	    rootEdit.addChild( edit );
	    return result;
	}

	private IFile getFile() {
		IFile file = (IFile) marker.getResource();
		return file;
	}

	public String getNewArchiveName() {
		return newArchiveName;
	}

	public void setNewArchiveName(String newArchiveName) {
		this.newArchiveName = newArchiveName;
	}

	public String getOldArchiveName() {
		return oldArchiveName;
	}

}
