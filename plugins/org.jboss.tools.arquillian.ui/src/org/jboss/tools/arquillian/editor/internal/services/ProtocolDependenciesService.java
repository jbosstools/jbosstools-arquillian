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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.sapphire.LocalizableText;
import org.eclipse.sapphire.Text;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.modeling.Status;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.editor.internal.model.Protocol;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ProtocolDependenciesService extends AbstractDependenciesService {

	 private static final String SERVLET_PROTOCOL_GROUP_ID = "org.jboss.arquillian.protocol";
	 private static final String SERVLET_PROTOCOL_ARTIFACT_ID = "arquillian-protocol-servlet";
	 
	@Text("The {0} protocol requires the {1}:{2} artifact.")
	private static LocalizableText message;

	static {
		LocalizableText
				.init(ProtocolDependenciesService.class);
	}
		
	@Override
	protected Status compute() {
		final Value<?> value = context( Value.class );
		if (value != null && value.element() instanceof Protocol) {
			if (Protocol.SERVLET_25.equals(value.text())
					|| Protocol.SERVLET_30.equals(value.text())) {
				IProject project = value.element().adapt(IProject.class);
				if (!testProtocolDependencies(project) ) {
					String msg = message.format(new Object[] {value.text(), SERVLET_PROTOCOL_GROUP_ID, SERVLET_PROTOCOL_ARTIFACT_ID});
					return Status.createWarningStatus(msg);
				}
			}

		}
		return Status.createOkStatus();
	}

	public static boolean testProtocolDependencies(IProject project) {
		if (project != null) {
			IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
			if (facade != null) {
				try {
					MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
					if (mavenProject != null) {
						String version = ArquillianUtility.getDependencyVersion(mavenProject, SERVLET_PROTOCOL_GROUP_ID, SERVLET_PROTOCOL_ARTIFACT_ID );
						if (version != null) {
							return true;
						}
					}
				} catch (CoreException e) {
					ArquillianUIActivator.logWarning(e.getLocalizedMessage());
				}
			}
		}
		return false;
	}

	public static List<Dependency> getDependencies(IProject project) {
		
		List<Dependency> dependencies = new ArrayList<Dependency>();
		Dependency dependency = new Dependency();
		dependency.setGroupId(SERVLET_PROTOCOL_GROUP_ID);
		dependency.setArtifactId(SERVLET_PROTOCOL_ARTIFACT_ID);
		boolean isManaged = false;
		try {
			IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
			if (facade != null) {
				MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
				if (mavenProject != null) {
					DependencyManagement depMgmt = mavenProject.getDependencyManagement();
					List<Dependency> mgmtDeps = depMgmt.getDependencies();
					for (Dependency mgmtDep:mgmtDeps) {
						if (SERVLET_PROTOCOL_GROUP_ID.equals(mgmtDep.getGroupId()) && SERVLET_PROTOCOL_ARTIFACT_ID.equals(mgmtDep.getArtifactId())) {
							isManaged = true;
							break;
						}
					}
				}
			}
		} catch (CoreException e) {
			ArquillianUIActivator.logWarning(e.getLocalizedMessage());
		}
		if (!isManaged) {
			String coords = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":[0,)";  //$NON-NLS-1$//$NON-NLS-2$
			String version = ArquillianUtility.getHighestVersion(coords);
			dependency.setVersion(version);
		}
		dependency.setScope(Artifact.SCOPE_TEST);
		dependencies.add(dependency);
		return dependencies;
	}

}
