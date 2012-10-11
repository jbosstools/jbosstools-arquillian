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
package org.jboss.tools.arquillian.ui.internal.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 * 
 */
public class RemoveArquillianCommandHandler extends ArquillianAbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = getProject(event);

		try {
			if (project == null || !project.hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID)) {
				// FIXME
				return null;
			}
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length - 1];
			int i = 0;
			for (String prevNature : prevNatures) {
				if (!ArquillianNature.ARQUILLIAN_NATURE_ID.equals(prevNature)) {
					newNatures[i] = prevNature;
					i++;
				}
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, new NullProgressMonitor());
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return null;
	}

}
