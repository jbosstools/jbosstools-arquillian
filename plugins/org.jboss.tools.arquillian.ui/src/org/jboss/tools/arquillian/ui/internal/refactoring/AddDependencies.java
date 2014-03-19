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
package org.jboss.tools.arquillian.ui.internal.refactoring;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomHelper.addOrUpdateDependency;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author snjeza
 *
 */
public class AddDependencies implements Operation {

	private List<Dependency> dependencies;
	private MavenProject mavenProject;

	public AddDependencies(List<Dependency> dependencies, MavenProject mavenProject) {
		this.dependencies = dependencies;
		this.mavenProject = mavenProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process
	 * (org.w3c.dom.Document)
	 */
	public void process(Document document) {
		if (dependencies == null || dependencies.size() <= 0) {
			return;
		}
		Element root = document.getDocumentElement();
		Element dependenciesEl = getChild(root, PomEdits.DEPENDENCIES);
		for (Dependency dependency:dependencies) {
			String version = ArquillianUtility.getDependencyVersion(mavenProject, dependency.getGroupId(), dependency.getArtifactId());
			if (version == null) {
				addOrUpdateDependency(dependenciesEl,
						dependency.getGroupId(), dependency.getArtifactId(),
						dependency.getVersion(), dependency.getType(),
						dependency.getScope(), dependency.getClassifier());
			}
		}
	}
}