/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.detectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianResourceHyperlink implements IHyperlink {

	private String resource;
	private IRegion region;
	private IFile file;
	
	public ArquillianResourceHyperlink(String resource, IRegion region, IFile file) {
		this.resource = resource;
		this.region = region;
		this.file = file;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return resource;
	}

	@Override
	public void open() {
		if (file != null) {
			IWorkbenchPage page = ArquillianUIUtil.getActivePage();
			try {
				IDE.openEditor(page, file);
			} catch (PartInitException e) {
				IFileEditorInput input = new FileEditorInput(file);
				try {
					IDE.openEditor(page, input, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
				} catch (PartInitException e1) {
					ArquillianUIActivator.log(e);
				}
			}
		}
	}

}
