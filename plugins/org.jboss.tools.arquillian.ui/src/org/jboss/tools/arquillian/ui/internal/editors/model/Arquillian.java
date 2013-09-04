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

import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditor;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class Arquillian {
	
	private ArquillianElement arquillian;

	protected IDOMDocument document;
	
	protected List<ContainerGroupModel> containerGroups;
	private List<Extension> extensions;
	private DefaultProtocol defaultProtocol;
	private Engine engine;
	
	public Arquillian(IDOMDocument document) {
		this.document = document;
	}
	
	public void init() {
		arquillian = new ArquillianElement(document.getDocumentElement());
		update();
	}
	
	public void update() {
		initContainerGroup();
		initExtension();
		initDefaultProtocol();
		initEngine();
	}
	
	/**
	 * Initializes containers and groups. These are created by arquillian.xml.
	 */
	protected void initContainerGroup() {
		containerGroups = new ArrayList<ContainerGroupModel>();
		for(ContainerGroupElement element : arquillian.getContainerGroups()) {
			if(element instanceof GroupElement) {
				containerGroups.add(new Group(this, (GroupElement) element));
			} else if(element instanceof ContainerElement) {
				containerGroups.add(new Container(this, (ContainerElement) element));
			}
		}
	}
	
	/**
	 * Initializes extensions. These are created by arquillian.xml.
	 */
	protected void initExtension() {
		extensions = new ArrayList<Extension>();
		for(ExtensionElement element : arquillian.getExtensions()) {
			extensions.add(new Extension(this, element));
		}
	}
	
	protected void initDefaultProtocol() {
		defaultProtocol = null;
		DefaultProtocolElement defaultProtocolElement = arquillian.getDefaultProtocol();
		if(defaultProtocolElement != null) {
			defaultProtocol = new DefaultProtocol(this, defaultProtocolElement);
		}
	}
	
	protected void initEngine() {
		engine = null;
		EngineElement engineElement = arquillian.getEngine();
		if(engineElement != null) {
			engine = new Engine(this, engineElement);
		}
	}
	
	public ContainerGroupModel getContainerOrGroup(String qualifier) {
		for(ContainerGroupModel model : containerGroups) {
			if(qualifier.equals(model.getQualifier())) {
				return model;
			}
		}
		return null;
	}
	
	/**
	 * Adds an element to this model by specified {@link ArquillianModelInfo}.
	 * If this model has no root element or root element is not 'arquillian', then it initializes root element
	 * 
	 * @param info the {@link ArquillianModelInfo}
	 */
	public void add(ArquillianModelInfo info) {
		if(document.getDocumentElement() == null
				|| !ArquillianXmlElement.TAG_ARQUILLIAN.equals(document.getDocumentElement().getNodeName())) {
			ArquillianEditor.initArquillianDocumentElement(document);
		}
		ArquillianModel model = info.generate(this);
		model.apply(info);
		model.appendTo(this);
	}
	
	public void edit(ArquillianModel model, ArquillianModelInfo info) {
		model.apply(info);
	}
	
	public void remove(ArquillianModel model) {
		if(model != null) {
			model.removeFrom(this);
		}
	}
	
	/**
	 * Inserts the specified new model before the specified model.
	 * 
	 * @param newModel the model for insert
	 * @param refModel the model for reference of position 
	 */
	public void insertBefore(ContainerGroupModel newModel, ContainerGroupModel refModel) {
		List<ContainerGroupModel> containerGroups = new ArrayList<ContainerGroupModel>();
		for(ContainerGroupModel model : this.containerGroups) {
			if(model == refModel) {
				containerGroups.add(newModel);
			}
			if(model != newModel) {
				containerGroups.add(model);
			}
		}
		this.containerGroups = containerGroups;
		document.getDocumentElement().insertBefore(newModel.getElement().getNode(), refModel.getElement().getNode());
	}
	
	/**
	 * Inserts the specified new model after the specified model.
	 * 
	 * @param newModel the model for insert
	 * @param refModel the model for reference of position 
	 */
	public void insertAfter(ContainerGroupModel newModel, ContainerGroupModel refModel) {
		List<ContainerGroupModel> containerGroups = new ArrayList<ContainerGroupModel>();
		for(ContainerGroupModel model : this.containerGroups) {
			if(model != newModel) {
				containerGroups.add(model);
			}
			if(model == refModel) {
				containerGroups.add(newModel);
			}
		}
		this.containerGroups = containerGroups;
		Node nextNode = refModel.getElement().getNode().getNextSibling();
		if(nextNode != null) {
			document.getDocumentElement().insertBefore(newModel.getElement().getNode(), nextNode);
		} else {
			document.getDocumentElement().appendChild(newModel.getElement().getNode());
		}
	}
	
	/**
	 * Checks whether default container or default group exist.
	 * And if not, sets the head of container or group to the default
	 */
	public void checkDefaultContainerGroup() {
		for(ContainerGroupModel model : containerGroups) {
			if(model.isDefault()) {
				return;
			}
		}
		if(containerGroups.size() > 0) {
			containerGroups.get(0).setDefault(true);
		}
	}
	
	/**
	 * Sets the dirty flag on.
	 */
	public void doDirty() {
		Text node = document.createTextNode(" ");
		document.appendChild(node);
		document.removeChild(node);
	}
	
	public IDOMDocument getDocument() {
		return document;
	}
	
	public List<ContainerGroupModel> getContainerGroups() {
		return containerGroups;
	}
	
	/**
	 * Returns a list of {@link Container} including container of group.
	 * @return a list of {@link Container} including container of group
	 */
	public List<Container> getAllContainers() {
		List<Container> list = new ArrayList<Container>();
		for(ContainerGroupModel model : containerGroups) {
			if(model instanceof Group) {
				for(Container container : ((Group) model).getContainers()) {
					list.add(container);
				}
			} else {
				list.add((Container) model);
			}
		}
		return list;
	}
	
	public List<Extension> getExtensions() {
		return extensions;
	}
	
	public List<String> getExtensionQualifiers() {
		List<String> qualifiers = new ArrayList<String>();
		for(Extension extension : extensions) {
			qualifiers.add(extension.getQualifier());
		}
		return qualifiers;
	}
	
	public DefaultProtocol getDefaultProtocol() {
		return defaultProtocol;
	}
	
	public void setDefaultProtocol(DefaultProtocol defaultProtocol) {
		this.defaultProtocol = defaultProtocol;
	}
	
	public Engine getEngine() {
		return engine;
	}
	
	public void setEngine(Engine engine) {
		this.engine = engine;
	}
}
