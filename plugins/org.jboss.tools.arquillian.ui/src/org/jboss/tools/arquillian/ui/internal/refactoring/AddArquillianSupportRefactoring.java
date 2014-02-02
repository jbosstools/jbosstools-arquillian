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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PROPERTIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;
import static org.eclipse.m2e.core.ui.internal.editing.PomHelper.addOrUpdateDependency;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A refactoring to add Arquillian support to project
 * 
 * @author snjeza
 * 
 */
public class AddArquillianSupportRefactoring extends Refactoring {

	private IProject project;
	private String version;
	private boolean updatePom;
	private boolean addProfiles;
	private boolean updateBuild;
	private boolean updateDependencies;

	/**
	 * @param project
	 */
	public AddArquillianSupportRefactoring(IProject project) {
		super();
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
	public String getName() {
		return "Add Arquillian support";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org
	 * .eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		boolean isMavenProject = project != null
				&& project.hasNature(IMavenConstants.NATURE_ID);
		if (!isMavenProject) {
			IStatus status = new Status(IStatus.ERROR,
					ArquillianUIActivator.PLUGIN_ID,
					"The project is not a valid maven project");
			return RefactoringStatus.create(status);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org
	 * .eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = checkInitialConditions(pm);
		if (!status.isOK()) {
			return status;
		}
		if (isUpdatePom()) {
			IFile file = getFile();
			if (file == null || !file.exists()) {
				IStatus s = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID,
						"The pom.xml file does not exist");
				return RefactoringStatus.create(s);
			}
			IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
			if (facade == null) {
				IStatus s = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID,
						"The project is not a valid maven project");
				return RefactoringStatus.create(s);
			}
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse
	 * .core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		if (!isUpdatePom()) {
			return new NullChange();
		}
		IFile file = getFile();
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
		MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
		String version = ArquillianUtility.getArquillianVersion(mavenProject);
		if (version == null) {
			List<Operation> operations = new ArrayList<Operation>();

			Model model = ArquillianUtility.getArquilianModel(true);
			if (isUpdateDependencies()) {
				operations.add(new AddProperties(
						ArquillianUtility.ARQUILLIAN_VERSION, getVersion()));

				List<Dependency> dependencies = model.getDependencies();
				operations.add(new AddDependencies(dependencies, mavenProject));

				DependencyManagement dependencyMgmt = model.getDependencyManagement();
				if (dependencyMgmt != null) {
					dependencies = dependencyMgmt.getDependencies();
					if (dependencies != null || dependencies.size() > 0) {
						operations.add(new AddDependencyManagement(dependencies, mavenProject));
					}
				}
			}
			if (isUpdateBuild()) {
				Build build = model.getBuild();
				operations.add(new AddPlugins(build, file));
			}

			if (isAddProfiles()) {
				operations.add(new AddProfiles(mavenProject));
			}
			CompoundOperation compound = new CompoundOperation(
					operations.toArray(new Operation[0]));
			
			return PomHelper.createChange(file, compound, getName());
		}
		return new NullChange();
	}

	private IFile getFile() throws CoreException {
		if (project == null || !project.hasNature(IMavenConstants.NATURE_ID)) {
			return null;
		}
		return project.getFile(IMavenConstants.POM_FILE_NAME);
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isUpdatePom() {
		return updatePom;
	}

	public void setUpdatePom(boolean updatePom) {
		this.updatePom = updatePom;
	}

	public boolean isAddProfiles() {
		return addProfiles;
	}

	public void setAddProfiles(boolean addProfiles) {
		this.addProfiles = addProfiles;
	}

	public IProject getProject() {
		return project;
	}

	private static class AddProperties implements Operation {

		private final String name;

		private final String value;

		public AddProperties(String name, String value) {
			this.name = name;
			this.value = value;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process
		 * (org.w3c.dom.Document)
		 */
		public void process(Document document) {
			Element props = findChild(document.getDocumentElement(),
					PROPERTIES);
			
			Element existing = findChild(props, name);
			if (existing != null) {
				setText(existing, value);
			} else {
				Element dm = getChild(document.getDocumentElement(),
						PROPERTIES);
				Element prop = PomEdits.createElement(dm, name);
				setText(prop, value);
				PomEdits.format(dm);
			}
		}
	}
	
	private static class AddDependencies implements Operation {

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
			format(dependenciesEl);
		}
	}

	private static class AddDependencyManagement implements Operation {

		private List<Dependency> dependencies;
		private MavenProject mavenProject;

		public AddDependencyManagement(List<Dependency> dependencies, MavenProject mavenProject) {
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
			List<Dependency> allDependencies = new ArrayList<Dependency>();
			DependencyManagement pdMgmt = mavenProject.getDependencyManagement();
			if (pdMgmt != null) {
				List<Dependency> pmd = pdMgmt.getDependencies();
				if (pmd != null) {
					allDependencies.addAll(pmd);
				}
			}
			Parent parent = mavenProject.getModel().getParent();
			if (parent != null) {
				try {
					ArquillianUtility.addManagedDependencies(allDependencies, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
				} catch (CoreException e) {
					ArquillianUIActivator.log(e);
				}
			}
			
			Element root = document.getDocumentElement();
			Element dependencyMgmtEl = getChild(root, PomEdits.DEPENDENCY_MANAGEMENT);
			Element dependenciesEl = getChild(dependencyMgmtEl, PomEdits.DEPENDENCIES);
			for (Dependency dependency:dependencies) {
				
				if (!ArquillianUtility.managedDependencyExists(dependency, allDependencies)) {
					addOrUpdateDependency(dependenciesEl,
							dependency.getGroupId(), dependency.getArtifactId(),
							dependency.getVersion(), dependency.getType(),
							dependency.getScope(), dependency.getClassifier());
				}
			}
			format(dependencyMgmtEl);
		}
	}

	private static class AddPlugins implements Operation {

		private Build build;
		private IFile file;

		public AddPlugins(Build build, IFile file) {
			this.build = build;
			this.file = file;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process
		 * (org.w3c.dom.Document)
		 */
		public void process(Document document) {
			if (build == null) {
				return;
			}
			List<Plugin> plugins = build.getPlugins();
			Element root = document.getDocumentElement();
			Element buildEl = getChild(root, PomEdits.BUILD);
			Element pluginsEl = getChild(buildEl, PomEdits.PLUGINS);
			IDOMModel model = null;
			try {
				model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(file);
				if (model == null) {
					model = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(file);
				}
			for (Plugin plugin:plugins) {
				if (!ArquillianUtility.pluginExists(model, plugin)) {
					PomHelper.createPlugin(pluginsEl, plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
				}
			}
			Element compiler = null;
			List<Element> elements = PomEdits.findChilds(pluginsEl, PomEdits.PLUGIN);
			for (Element element:elements) {
				Element pluginArtifactId = findChild(element, PomEdits.ARTIFACT_ID);
				String aid = getTextValue(pluginArtifactId);
				Element pluginGroupId = findChild(element, PomEdits.GROUP_ID);
				String gid = pluginGroupId == null ? ArquillianUtility.MAVEN_GROUP_ID : getTextValue(pluginGroupId);
				if (ArquillianUtility.MAVEN_COMPILER_ARTIFACT_ID.equals(aid) && (ArquillianUtility.MAVEN_GROUP_ID.equals(gid))) {
					compiler = element;
					break;
				}
			}
			ArquillianUtility.fixCompilerPlugin(compiler);
			} catch (Exception e) {
				ArquillianCoreActivator.log(e);
			} finally {
				if (model != null) {
					model.releaseFromRead();
				}
			}
			format(buildEl);
		}
	}

	private static class AddProfiles implements Operation {

		private MavenProject mavenProject;
		
		public AddProfiles(MavenProject mavenProject) {
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
			List<String> selectedProfiles = ArquillianUtility.getProfilesFromPreferences(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES);
			if (selectedProfiles == null || selectedProfiles.size() <= 0) {
				return;
			}
			List<Container> selectedContainers = new ArrayList<Container>();
			Model projectModel = mavenProject.getModel();
			List<String> allProfiles;
			try {
				allProfiles = ArquillianUtility.getProfiles(projectModel);
			} catch (CoreException e) {
				ArquillianUIActivator.log(e);
				return;
			}
			for(Container container:ContainerParser.getContainers()) {
				if (selectedProfiles.contains(container.getId()) &&
						!allProfiles.contains(container.getId())) {
					selectedContainers.add(container);
				}
			}
			if (selectedContainers.size() <= 0) {
				return;
			}
			
			Element root = document.getDocumentElement();
			Element profiles = getChild(root, PomEdits.PROFILES);
			for (Container container:selectedContainers) {
				//org.eclipse.m2e.model.edit.pom.Profile profile = ProfileGenerator.getProfile(container);
				generateProfile(profiles, container);
			}
		}

		private void generateProfile(Element profiles, Container container) {
			if (container == null) {
				return;
			}
			String id = container.getId();
			Element profile = getChild(profiles, PomEdits.PROFILE);
			Element idEl = getChild(profile, PomEdits.ID);
			setText(idEl, id);
			Element dependencies = getChild(profile, PomEdits.DEPENDENCIES);
			String version = getVersion(container.getGroup_id(),
					container.getArtifact_id());
			PomHelper.addOrUpdateDependency(dependencies,
					container.getGroup_id(), container.getArtifact_id(),
					version, null, null, null);
			List<org.jboss.forge.arquillian.container.Dependency> deps = container
					.getDependencies();
			if (deps != null) {
				for (org.jboss.forge.arquillian.container.Dependency fd : deps) {
					version = getVersion(fd.getGroup_id(), fd.getArtifact_id());
					PomHelper.addOrUpdateDependency(dependencies,
							fd.getGroup_id(), fd.getArtifact_id(), version,
							null, null, null);
				}
			}
			format(profile);
		}
		
		private String getVersion(String gid, String aid) {
			String coords = gid + ":" + aid + ":[0,)";  //$NON-NLS-1$//$NON-NLS-2$
			return ArquillianUtility.getHighestVersion(coords);
		}
		
	}

	public boolean isUpdateBuild() {
		return updateBuild;
	}

	public void setUpdateBuild(boolean updateBuild) {
		this.updateBuild = updateBuild;
	}

	public boolean isUpdateDependencies() {
		return updateDependencies;
	}

	public void setUpdateDependencies(boolean updateDependencies) {
		this.updateDependencies = updateDependencies;
	}


}
