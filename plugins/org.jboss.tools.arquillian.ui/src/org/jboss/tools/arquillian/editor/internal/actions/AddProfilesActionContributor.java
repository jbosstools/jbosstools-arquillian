/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.editor.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ui.assist.PropertyEditorAssistContext;
import org.eclipse.sapphire.ui.assist.PropertyEditorAssistContribution;
import org.eclipse.sapphire.ui.assist.PropertyEditorAssistContributor;
import org.eclipse.sapphire.ui.assist.PropertyEditorAssistSection;
import org.jboss.tools.arquillian.editor.internal.model.Container;
import org.jboss.tools.arquillian.editor.internal.refactoring.AddProfilesRefactoring;
import org.jboss.tools.arquillian.editor.internal.refactoring.AddProfilesWizard;
import org.jboss.tools.arquillian.editor.internal.services.ContainerProfilesService;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.markers.RefactoringUtil;

/**
 * 
 * @author snjeza
 * 
 */
public final class AddProfilesActionContributor extends
		PropertyEditorAssistContributor {
	@Override
	public void contribute(final PropertyEditorAssistContext context) {
		Element element = context.getPart().getLocalModelElement();

		if (element instanceof Container) {
			final IProject project = element.adapt(IProject.class);
			final String id = ((Container)element).getQualifier().text();
			if (!ContainerProfilesService.testProfile(project, id)) {
				PropertyEditorAssistContribution.Factory contribution = PropertyEditorAssistContribution.factory();
				contribution.text("<p><a href=\"action\" nowrap=\"true\">Add required profiles...</a></p>");
				contribution.link("action", new Runnable() {
					public void run() {
						List<String> profiles = new ArrayList<String>();
						profiles.add(id);
						AddProfilesRefactoring refactoring = new AddProfilesRefactoring(project, profiles);
						RefactoringWizard wizard = new AddProfilesWizard(refactoring);
						RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
						try {
							operation.run(RefactoringUtil.getShell(), ""); //$NON-NLS-1$
						} catch (InterruptedException e) {
							ArquillianUIActivator.log(e);
						}
					}
				});
				PropertyEditorAssistSection section = context.getSection(SECTION_ID_ACTIONS);
				section.addContribution(contribution.create());
			}
		}
	}

}
