/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Nippon Telegraph and Telephone Corporation - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.extension.Extension;
import org.jboss.tools.arquillian.core.internal.extension.ExtensionParser;
import org.jboss.tools.arquillian.core.internal.protocol.Protocol;
import org.jboss.tools.arquillian.core.internal.protocol.ProtocolParser;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependencies;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependency;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.DependencyManagement;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Profile;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Profiles;

public class PomHelper {

	private Arquillian arquillian;

	public PomHelper(Arquillian arquillian) {
		this.arquillian = arquillian;
	}

	public Profiles getProfiles() {
		List<String> types = new ArrayList<String>();
		for(org.jboss.tools.arquillian.ui.internal.editors.model.Container container : arquillian.getAllContainers()) {
			if(!types.contains(container.getType())) {
				types.add(container.getType());
			}
		}
		return getProfiles(types);
	}
	
	public Profiles getProfiles(List<String> containers) {
		Profiles profiles = new Profiles();

		Map<String, Container> allContainers = new HashMap<String, Container>();
		for (Container container : ContainerParser.getContainers()) {
			allContainers.put(container.getId(), container);
		}
		
		for(String type : containers) {
			Container container = allContainers.get(type);
			if(container == null) continue;
			Profile profile = new Profile().id(container.getId());
			Dependencies dependencies = new Dependencies();
			dependencies.addChild(createDependency(container.getGroup_id(), container.getArtifact_id(), null, null));
			if (container.getDependencies() != null) {
				for (org.jboss.forge.arquillian.container.Dependency dependency : container.getDependencies()) {
					dependencies.addChild(createDependency(dependency));
				}
			}
			profile.addChild(dependencies);
			profiles.addChild(profile);
		}

		return profiles;
	}
	
	public Dependencies getDependencies() {
		List<String> protocols = new ArrayList<String>();
		if(arquillian.getDefaultProtocol() != null) {
			protocols.add(arquillian.getDefaultProtocol().getType());
		}
		for(org.jboss.tools.arquillian.ui.internal.editors.model.Container container : arquillian.getAllContainers()) {
			for(org.jboss.tools.arquillian.ui.internal.editors.model.Protocol proto : container.getProtocols()) {
				if(!protocols.contains(proto.getType())) {
					protocols.add(proto.getType());
				}
			}
		}
		return getDependencies(arquillian.getExtensionQualifiers(), protocols);
	}
	
	public Dependencies getDependencies(List<String> extensions, List<String> protocols) {
		Dependencies dependencies = new Dependencies();
		List<String> added = new ArrayList<String>();
		// extension
		for(String qualifier : extensions) {
			Extension extension = ExtensionParser.getExtension(qualifier);
			if(extension == null || extension.getDependencies() == null) continue;
			for(org.jboss.forge.arquillian.container.Dependency dependency : extension.getDependencies()) {
				if(added.contains(dependency.getGroup_id() + dependency.getArtifact_id())) continue;
				added.add(dependency.getGroup_id() + dependency.getArtifact_id());
				dependencies.addChild(createDependency(dependency));
			}
		}
		// protocol
		for(String type : protocols) {
			Protocol protocol = ProtocolParser.getProtocol(type);
			if(protocol == null
					|| StringUtils.isEmpty(protocol.getGroup_id())
					|| added.contains(protocol.getGroup_id() + protocol.getArtifact_id())) continue;
			added.add(protocol.getGroup_id() + protocol.getArtifact_id());
			dependencies.addChild(createDependency(protocol.getGroup_id(), protocol.getArtifact_id(), null, "test"));
		}
		return dependencies;
	}
	
	public DependencyManagement getDependencyManagement() {
		return getDependencyManagement(arquillian.getExtensionQualifiers());
	}
	
	public DependencyManagement getDependencyManagement(List<String> extensions) {
		DependencyManagement dependencyManagement = new DependencyManagement();
		Dependencies dependencies = dependencyManagement.getDependencies();
		List<String> added = new ArrayList<String>();
		for(String qualifier : extensions) {
			Extension extension = ExtensionParser.getExtension(qualifier);
			if(extension == null || extension.getManagementDependencies() == null) continue;
			for(org.jboss.forge.arquillian.container.Dependency dependency : extension.getManagementDependencies()) {
				if(added.contains(dependency.getGroup_id() + dependency.getArtifact_id())) continue;
				added.add(dependency.getGroup_id() + dependency.getArtifact_id());
				dependencies.addChild(createDependency(dependency));
			}
		}
		return dependencyManagement;
	}
	
	private Dependency createDependency(org.jboss.forge.arquillian.container.Dependency dependency) {
		return createDependency(
					dependency.getGroup_id(),
					dependency.getArtifact_id(),
					dependency.getType(),
					dependency.getScope());
	}

	private Dependency createDependency(String groupId, String artifactId, String type, String scope) {
		Dependency dependency = new Dependency().groupId(groupId).artifactId(artifactId).version(resolveVersion(groupId, artifactId));
		if (StringUtils.isNotEmpty(type)) {
			dependency.type(type);
		}
		if (StringUtils.isNotEmpty(scope)) {
			dependency.scope(scope);
		}
		return dependency;
	}

	private String resolveVersion(String groupId, String artifactId) {
		String coords = groupId + ":" + artifactId + ":[0,)"; //$NON-NLS-1$//$NON-NLS-2$
		return ArquillianUtility.getHighestVersion(coords);
	}
}
