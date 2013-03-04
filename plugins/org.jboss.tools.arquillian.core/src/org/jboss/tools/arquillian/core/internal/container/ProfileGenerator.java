/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.core.internal.container;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

/** 
 * 
 * @author snjeza
 *
 */
public class ProfileGenerator {
	
	private static Map<String, Profile> profiles = new HashMap<String, Profile>();
	
	public static Profile getProfile(Container container) {
		if (container == null) {
			return null;
		}
		String id = container.getId();
		Profile profile = profiles.get(id);
		if (profile == null) {
			profile = PomFactory.eINSTANCE.createProfile();
			profile.setId(id);
			Dependency dependency = PomFactory.eINSTANCE.createDependency();
			dependency.setArtifactId(container.getArtifact_id());
			dependency.setGroupId(container.getGroup_id());
			resolveVersion(dependency);
			profile.getDependencies().add(dependency);
			List<org.jboss.forge.arquillian.container.Dependency> dependencies = container.getDependencies();
			if (dependencies != null) {
				for (org.jboss.forge.arquillian.container.Dependency fd:dependencies) {
					Dependency dep = PomFactory.eINSTANCE.createDependency();
					dep.setArtifactId(fd.getArtifact_id());
					dep.setGroupId(fd.getGroup_id());
					resolveVersion(dep);
					profile.getDependencies().add(dep);
				}
			}
			profiles.put(id, profile);
		}
		return profile;
	}

	private static Map<String, Profile> getProfiles() {
		return profiles;
	}
	
	private static void resolveVersion(Dependency dep) {
		String coords = dep.getGroupId() + ":" + dep.getArtifactId() + ":[0,)";  //$NON-NLS-1$//$NON-NLS-2$
		String version = ArquillianUtility.getHighestVersion(coords);
		dep.setVersion(version);
	}
	
	public static void clearProfiles() {
		profiles.clear();
	}
}
