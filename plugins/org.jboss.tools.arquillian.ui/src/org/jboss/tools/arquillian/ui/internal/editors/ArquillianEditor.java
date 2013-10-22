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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMText;
import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianXmlElement;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.maven.core.MavenCoreActivator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArquillianEditor extends MultiPageEditorPart {

	private static final int PAGE_GENERAL = 0;
	private static final int PAGE_SOURCE = 1;

	private StructuredTextEditor sourceEditor;
	private ArquillianGeneralEditor generalEditor;

	private IModelStateListener modelStateListener = new IModelStateListener() {

		@Override
		public void modelChanged(IStructuredModel model) {
			modelUpdated(model);
		}

		@Override
		public void modelResourceMoved(IStructuredModel arg0, IStructuredModel arg1) {
		}

		@Override
		public void modelResourceDeleted(IStructuredModel arg0) {
		}

		@Override
		public void modelReinitialized(IStructuredModel arg0) {
		}

		@Override
		public void modelDirtyStateChanged(IStructuredModel arg0, boolean arg1) {
		}

		@Override
		public void modelAboutToBeReinitialized(IStructuredModel arg0) {
		}

		@Override
		public void modelAboutToBeChanged(IStructuredModel arg0) {
		}
	};

	public ArquillianEditor() {
		super();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(input.getName());
	}

	@Override
	public void setFocus() {
		getControl(getActivePage()).setFocus();
	}

	@Override
	protected void createPages() {
		try {
			sourceEditor = new StructuredTextEditor();
			addPage(0, sourceEditor, getEditorInput());
			setPageText(0, "source");
		} catch (PartInitException e) {
			ArquillianUIActivator.log(e);
		}
		IDOMModel model = (IDOMModel) sourceEditor.getAdapter(IStructuredModel.class);
		try {
			generalEditor = new ArquillianGeneralEditor();
			addPage(0, generalEditor, getEditorInput());
			initDocumentElement(model);
			generalEditor.initModel(model);
			setPageText(0, "general");
		} catch (Exception e) {
			ArquillianUIActivator.log(e);
			removePage(0);
		}

		model.addModelStateListener(modelStateListener);
		setActivePage(0);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (generalEditor != null && getActivePage() == PAGE_GENERAL) {
			format();
		}
		sourceEditor.doSave(monitor);
		if (generalEditor != null) {
			try {
				updatePom();
			} catch (Exception e) {
				ArquillianUIActivator.log(e);
			}
		}
	}

	@Override
	public void doSaveAs() {
		// save as is not allowed.
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void dispose() {
		IDOMModel model = (IDOMModel) sourceEditor.getAdapter(IStructuredModel.class);
		model.removeModelStateListener(modelStateListener);
		super.dispose();
	}

	private void modelUpdated(IStructuredModel model) {
		if (getActivePage() == PAGE_SOURCE) {
			generalEditor.updateModel((IDOMModel) model);
		}
	}

	/**
	 * Initializes the root element of arquillian.xml. If the root element does
	 * not exist or the root element is not 'arquillian', then it appends
	 * 'arquillian' element as the root element.
	 * 
	 * @param model
	 *            the <code>IDOMModel</code>
	 * @throws Exception
	 */
	private void initDocumentElement(IDOMModel model) throws Exception {
		if (model.getDocument().getDocumentElement() == null
				|| !ArquillianXmlElement.TAG_ARQUILLIAN.equals(model.getDocument().getDocumentElement().getNodeName())) {
			ArquillianEditor.initArquillianDocumentElement(model.getDocument());
			sourceEditor.doSave(null);
		}
	}

	private void format() {
		// remove empty lines for format.
		// Arquillian#sortContainerGroups may make empty lines.
		IDOMModel model = (IDOMModel) sourceEditor.getAdapter(IStructuredModel.class);
		if (model.getDocument() == null || model.getDocument().getDocumentElement() == null) {
			return;
		}
		removeChildText(model.getDocument().getDocumentElement());
		// execute format
		ITextOperationTarget target = (ITextOperationTarget) sourceEditor.getAdapter(ITextOperationTarget.class);
		target.doOperation(StructuredTextViewer.FORMAT_DOCUMENT);
	}

	private void updatePom() throws Exception {
		if (!(getEditorInput() instanceof IFileEditorInput)) {
			return;
		}
		IProject project = ((FileEditorInput) getEditorInput()).getFile().getProject();
		IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
		PomResourceImpl pomResource = MavenCoreActivator.loadResource(pomFile);
		Arquillian arquillian = generalEditor.getArquillian();
		PomUpdateHelper helper = new PomUpdateHelper(arquillian, pomResource, pomFile);
		try {
			boolean save = false;

			// add profiles
			EList<org.eclipse.m2e.model.edit.pom.Profile> profiles = pomResource.getModel().getProfiles();
			for(Profile profile : helper.generateProfiles()) {
				profiles.add(EcoreUtil.copy(profile));
				save = true;
			}

			// add dependencies
			EList<Dependency> dependencies = pomResource.getModel().getDependencies();
			for (Dependency dependency : helper.generateDependencies()) {
				dependencies.add(EcoreUtil.copy(dependency));
				save = true;
			}

			// add dependencyManagement
			DependencyManagement dependencyManagement = pomResource.getModel().getDependencyManagement();
			List<Dependency> managementDeps = helper.generateManagementDependencies();
			if (managementDeps.size() > 0) {
				if (dependencyManagement == null) {
					dependencyManagement = PomFactory.eINSTANCE.createDependencyManagement();
					pomResource.getModel().setDependencyManagement(dependencyManagement);
				}
				EList<Dependency> managementDependencies = dependencyManagement.getDependencies();
				for (Dependency dependency : managementDeps) {
					managementDependencies.add(EcoreUtil.copy(dependency));
					save = true;
				}
			}

			if (save) {
				Map<String, String> options = new HashMap<String, String>();
				options.put(XMIResource.OPTION_ENCODING, MavenCoreActivator.ENCODING);
				pomResource.save(options);
			}
		} finally {
			pomResource.unload();
		}
	}

	private void removeChildText(Element element) {
		if (ArquillianXmlElement.TAG_PROPERTY.equals(element.getTagName())) {
			return;
		}
		NodeList nodeList = element.getChildNodes();
		if (nodeList.getLength() == 0) {
			return;
		}
		for (int i = nodeList.getLength(); i >= 0; i--) {
			Node node = nodeList.item(i);
			if (node instanceof IDOMText) {
				element.removeChild(node);
			} else if (node instanceof Element) {
				removeChildText((Element) node);
			}
		}
	}

	/**
	 * Initializes the document object of arquillian.xml.
	 * 
	 * @param document
	 *            the document
	 */
	public static void initArquillianDocumentElement(Document document) {
		// clear text
		try {
			NodeList children = document.getChildNodes();
			for (int i = children.getLength(); i > 0; i--) {
				document.removeChild(children.item(i - 1));
			}
		} catch (Exception e) {
			// skip
			ArquillianUIActivator.log(e);
		}
		Element root = document.createElement(ArquillianXmlElement.TAG_ARQUILLIAN);
		root.setAttribute("xmlns", "http://jboss.org/schema/arquillian"); //$NON-NLS-1$//$NON-NLS-2$
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$//$NON-NLS-2$
		root.setAttribute(
				"xsi:schemaLocation", "http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd"); //$NON-NLS-1$//$NON-NLS-2$
		document.appendChild(root);
	}

	/**
	 * Creates property columns.
	 * 
	 * @param viewer
	 *            the viewer
	 */
	public static void createPropertyColumns(TableViewer viewer) {
		createPropertyColumns(viewer, new PropertyEditingSupport(viewer), 200, 400);
	}

	/**
	 * Creates property columns with the given width.
	 * 
	 * @param viewer
	 *            the viewer
	 * @param editingSupport
	 *            the <code>PropertyEditingSupport</code>
	 * @param width1
	 *            the width of column1
	 * @param width2
	 *            the width of column2
	 */
	public static void createPropertyColumns(TableViewer viewer, PropertyEditingSupport editingSupport, int width1,
			int width2) {
		TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.NONE, 0);
		column1.getColumn().setText("name");
		column1.getColumn().setWidth(width1);
		TableViewerColumn column2 = new TableViewerColumn(viewer, SWT.NONE, 1);
		column2.getColumn().setText("value");
		column2.getColumn().setWidth(width2);
		column2.setEditingSupport(editingSupport);
	}

	/**
	 * Restores property from the given properties.
	 * 
	 * @param propertyDefs
	 *            the definition of property
	 * @param baseProperties
	 *            the base properties
	 */
	public static List<Property> restoreProperties(List<Configuration> configurations, List<Property> baseProperties) {
		List<Property> properties = new ArrayList<Property>();
		Map<String, Property> propertyMap = new HashMap<String, Property>();
		for (Property prop : baseProperties) {
			propertyMap.put(prop.getName(), prop);
		}
		for (Configuration configuration : configurations) {
			Property property = new Property(configuration.getName(), "", configuration.getType());
			if (propertyMap.containsKey(configuration.getName())) {
				property.setValue(propertyMap.get(configuration.getName()).getValue());
			}
			properties.add(property);
		}
		return properties;
	}

	private static class PomUpdateHelper extends PomHelper {

		private PomResourceImpl pomResource;
		private IFile pomFile;

		private PomUpdateHelper(Arquillian arquillian, PomResourceImpl pomResource, IFile pomFile) {
			super(arquillian);
			this.pomResource = pomResource;
			this.pomFile = pomFile;
		}

		private List<Profile> generateProfiles() throws CoreException {
			List<Profile> profiles = new ArrayList<Profile>();
			List<String> addedProfiles = ArquillianUtility.getProfiles(MavenPlugin.getMaven().readModel(pomFile.getContents()));
			for (org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Profile p : getProfiles().getProfiles()) {
				if (addedProfiles.contains(p.getId())) continue;
				Profile profile = PomFactory.eINSTANCE.createProfile();
				profile.setId(p.getId());
				for(org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependency dep : p.getDependencies().getDependencies()) {
					Dependency dependency = PomFactory.eINSTANCE.createDependency();
					dependency.setGroupId(dep.getGroupId());
					dependency.setArtifactId(dep.getArtifactId());
					dependency.setVersion(dep.getVersion());
					String type = dep.getType();
					if(StringUtils.isNotEmpty(type)) {
						dependency.setType(type);
					}
					String scope = dep.getScope();
					if(StringUtils.isNotEmpty(scope)) {
						dependency.setScope(scope);
					}
					profile.getDependencies().add(dependency);
				}
				profiles.add(profile);
			}
			return profiles;
		}

		private List<Dependency> generateDependencies() {
			List<Dependency> dependencies = new ArrayList<Dependency>();
			List<String> ids = getDependencyIds();
			for(org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependency dep : getDependencies().getDependencies()) {
				if(ids.contains(dep.getGroupId() + dep.getArtifactId())) continue;
				dependencies.add(createDependency(dep));
			}
			return dependencies;
		}
		
		private List<Dependency> generateManagementDependencies() {
			List<Dependency> dependencies = new ArrayList<Dependency>();
			List<String> ids = getManagementDependencyIds();
			for(org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependency dep : getDependencyManagement().getDependencies().getDependencies()) {
				if(ids.contains(dep.getGroupId() + dep.getArtifactId())) continue;
				dependencies.add(createDependency(dep));
			}
			return dependencies;
		}
		
		private Dependency createDependency(org.jboss.tools.arquillian.ui.internal.editors.model.PomElement.Dependency dep) {
			Dependency dependency = PomFactory.eINSTANCE.createDependency();
			dependency.setArtifactId(dep.getArtifactId());
			dependency.setGroupId(dep.getGroupId());
			dependency.setVersion(dep.getVersion());
			if (StringUtils.isNotEmpty(dep.getType())) {
				dependency.setType(dep.getType());
			}
			if (StringUtils.isNotEmpty(dep.getScope())) {
				dependency.setScope(dep.getScope());
			}
			return dependency;
		}

		private List<String> getDependencyIds() {
			List<String> ids = new ArrayList<String>();
			for (Dependency dependency : pomResource.getModel().getDependencies()) {
				ids.add(dependency.getGroupId() + dependency.getArtifactId());
			}
			return ids;
		}

		private List<String> getManagementDependencyIds() {
			List<String> ids = new ArrayList<String>();
			DependencyManagement dependencyManagement = pomResource.getModel().getDependencyManagement();
			if (dependencyManagement != null) {
				for (Dependency dependency : dependencyManagement.getDependencies()) {
					ids.add(dependency.getGroupId() + dependency.getArtifactId());
				}
			}
			return ids;
		}

	}
}
