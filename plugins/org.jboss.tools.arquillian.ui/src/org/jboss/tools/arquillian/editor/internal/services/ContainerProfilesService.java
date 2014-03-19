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
package org.jboss.tools.arquillian.editor.internal.services;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.sapphire.LocalizableText;
import org.eclipse.sapphire.Text;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.modeling.Status;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.editor.internal.utils.ArquillianEditorUtils;

/**
 * 
 * @author snjeza
 *
 */
public class ContainerProfilesService extends AbstractDependenciesService {

	@Text("The {0} profile is not included into the project.")
	private static LocalizableText message;

	static {
		LocalizableText.init(ContainerProfilesService.class);
	}

	private static Container getContainer(Value<?> value) {
		if (value != null && value.element() instanceof org.jboss.tools.arquillian.editor.internal.model.Container) {
			String id = value.text();
			return ArquillianEditorUtils.getContainer(id);
		}
		return null;
	}
	
	@Override
	protected Status compute() {
		Value<?> value = context(Value.class);
		IProject project = value.element().adapt(IProject.class);
		Container c = getContainer(value);
		if (c != null && c.getId() != null && !c.getId().isEmpty()) {
			String id = c.getId();
			if (!testProfile(project, id)) {
				String msg = message.format(new Object[] { value.text(), id });
				return Status.createWarningStatus(msg);
			}
		}
		return Status.createOkStatus();
	}

	public static boolean testProfile(IProject project, String id) {
		if (project != null && id != null && !id.isEmpty() && ArquillianEditorUtils.getContainer(id) != null) {
			List<String> profiles = ArquillianUtility.getProfiles(project);
			return profiles != null && profiles.contains(id);
		}
		return true;
	}

}
