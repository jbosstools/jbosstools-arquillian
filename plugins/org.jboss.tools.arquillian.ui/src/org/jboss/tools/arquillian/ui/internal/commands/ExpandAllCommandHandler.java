/*************************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.commands;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.arquillian.ui.internal.views.ArquillianView;

/**
 * 
 * Expand Arquillia tree viewer.
 * 
 * @author snjeza
 *
 */
public class ExpandAllCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		
		if (activePart instanceof ArquillianView) {
			ArquillianView view = (ArquillianView) activePart;
			CommonViewer viewer = view.getCommonViewer();
			ITreeSelection sel = viewer.getStructuredSelection();
			if (sel.isEmpty()) {
				return null;
			}
			Iterator iterator = sel.iterator();
			while (iterator.hasNext()) {
				Object object = iterator.next();
				expand(viewer, object);
			}
		}
		return null;
	}

	private void expand(CommonViewer viewer, Object object) {
		int level = AbstractTreeViewer.ALL_LEVELS;
		if (object instanceof ICompilationUnit) {
			level = 1;
		} else if (object instanceof IPackageFragment) {
			level = 2;
		} else if (object instanceof IPackageFragmentRoot) {
			level = 3;
		} else if (object instanceof IProject) {
			level = 4;
		}
		viewer.expandToLevel(object, level);
	}

}
