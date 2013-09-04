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
package org.jboss.tools.arquillian.ui.internal.editors.model;

import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.arquillian.core.internal.util.StringUtils;

public class PomElement {

	private String name;

	private String value;

	private List<PomElement> children = new ArrayList<PomElement>();

	public PomElement(){}
	
	public PomElement(String name) {
		this(name, null);
	}

	public PomElement(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public PomElement addChild(PomElement node) {
		children.add(node);
		return this;
	}
	
	protected String getChildValue(String prop) {
		for(PomElement element : children) {
			if(element.getName().equals(prop)) {
				return element.getValue();
			}
		}
		return null;
	}

	public List<PomElement> getChildren() {
		return children;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getXml();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PomElement && toString().equals(((PomElement) obj).toString());
	}

	private String getXml() {
		return getXml(""); //$NON-NLS-1$
	}

	protected String getXml(String tab) {
		StringBuilder sb = new StringBuilder();
		sb.append(tab).append(String.format("<%s>", name)); //$NON-NLS-1$
		if(StringUtils.isNotEmpty(value)) {
			sb.append(value);
		}
		if(!children.isEmpty()) {
			sb.append("\n"); //$NON-NLS-1$
			for(PomElement child : children) {
				sb.append(child.getXml(tab + "\t")).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if(StringUtils.isEmpty(value)) {
			sb.append(tab);
		}
		sb.append(String.format("</%s>", name)); //$NON-NLS-1$
		return sb.toString();
	}
	
	public static class Profiles extends PomElement {
		
		public Profiles() {
			super("profiles");
		}
		
		public List<Profile> getProfiles() {
			List<Profile> profiles = new ArrayList<PomElement.Profile>();
			for(PomElement element : getChildren()) {
				if(element instanceof Profile) {
					profiles.add((Profile) element);
				}
			}
			return profiles;
		}
	}
	
	public static class Profile extends PomElement {
		
		public Profile() {
			super("profile");
		}
		
		public Profile id(String id) {
			addChild(new PomElement("id", id));
			return this;
		}
		
		public String getId() {
			return getChildValue("id");
		}
		
		public Dependencies getDependencies() {
			for(PomElement element : getChildren()) {
				if(element instanceof Dependencies) {
					return (Dependencies) element;
				}
			}
			return null;
		}
	}
	
	public static class Dependencies extends PomElement {

		public Dependencies() {
			super("dependencies");
		}
		
		public List<Dependency> getDependencies() {
			List<Dependency> dependencies = new ArrayList<PomElement.Dependency>();
			for(PomElement element : getChildren()) {
				if(element instanceof Dependency) {
					dependencies.add((Dependency) element);
				}
			}
			return dependencies;
		}
		
		public boolean contains(PomElement element) {
			return element instanceof Dependency && getChildren().contains(element);
		}
	}
	
	public static class Dependency extends PomElement {

		public Dependency() {
			super("dependency");
		}
		
		public Dependency groupId(String groupId) {
			addChild(new PomElement("groupId", groupId));
			return this;
		}
		
		public Dependency artifactId(String artifactId) {
			addChild(new PomElement("artifactId", artifactId));
			return this;
		}
		
		public Dependency version(String version) {
			addChild(new PomElement("version", version));
			return this;
		}
		
		public Dependency type(String type) {
			addChild(new PomElement("type", type));
			return this;
		}
		
		public Dependency scope(String scope) {
			addChild(new PomElement("scope", scope));
			return this;
		}
		
		public String getGroupId() {
			return getChildValue("groupId");
		}
		
		public String getArtifactId() {
			return getChildValue("artifactId");
		}
		
		public String getVersion() {
			return getChildValue("version");
		}
		
		public String getType() {
			return getChildValue("type");
		}
		
		public String getScope() {
			return getChildValue("scope");
		}
	}
	
	public static class DependencyManagement extends PomElement {

		public DependencyManagement() {
			super("dependencyManagement");
			addChild(new Dependencies());
		}
		
		public Dependencies getDependencies() {
			return (Dependencies) getChildren().get(0);
		}
		
	}
}
