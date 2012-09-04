/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.jboss.tools.arquillian.core.internals.util;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;
import static org.eclipse.m2e.core.ui.internal.editing.PomHelper.addOrUpdateDependency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.maven.core.MavenCoreActivator;
import org.w3c.dom.Element;

/**
 * 
 * @author snjeza
 * 
 */
public class ArquillianUtils {
	
public static final String ARQUILLIAN_CORE_GROUP_ID = "org.jboss.arquillian.core"; //$NON-NLS-1$
	
	public static final String ARQUILLIAN_CORE_API_ARTIFACT_ID = "arquillian-core-api"; //$NON-NLS-1$
	
	public static final String ARQUILLIAN_GROUP_ID = "org.jboss.arquillian"; //$NON-NLS-1$
	
	public static final String ARQUILLIAN_BOM_ARTIFACT_ID = "arquillian-bom"; //$NON-NLS-1$
	
	public static final String ARQUILLIAN_JUNIT_POM_URL = "platform:/plugin/org.jboss.tools.arquillian.core/poms/arquillian-pom-junit.xml"; //$NON-NLS-1$
	
	public static final String ARQUILLIAN_TESTNG_POM_URL = "platform:/plugin/org.jboss.tools.arquillian.core/poms/arquillian-pom-testng.xml"; //$NON-NLS-1$

	public static final String ARQUILLIAN_PROFILE_POM_URL = "platform:/plugin/org.jboss.tools.arquillian.core/poms/arquillian-pom-profiles.xml"; //$NON-NLS-1$
	
	public static final String MAVEN_COMPILER_ARTIFACT_ID = "maven-compiler-plugin"; //$NON-NLS-1$
	
	public static final String MAVEN_GROUP_ID = "org.apache.maven.plugins"; //$NON-NLS-1$

	public static final String MAVEN_COMPILER_VERSION = "2.5.1"; //$NON-NLS-1$
	
	public static final String MAVEN_COMPILER_SOURCE_NODE = "source"; //$NON-NLS-1$
	
	public static final String MAVEN_COMPILER_TARGET_NODE = "target"; //$NON-NLS-1$

	public static final String MAVEN_COMPILER_SOURCE_LEVEL = "1.6"; //$NON-NLS-1$
	
	public static final String MAVEN_COMPILER_TARGET_LEVEL = "1.6"; //$NON-NLS-1$


	public static String getDependencyVersion(MavenProject mavenProject,
			String gid, String aid) {
		List<Artifact> artifacts = getArtifacts(mavenProject);
		for (Artifact artifact : artifacts) {
			String groupId = artifact.getGroupId();
			if (groupId != null && (groupId.equals(gid))) {
				String artifactId = artifact.getArtifactId();
				if (artifactId != null && artifactId.equals(aid)) {
					return artifact.getVersion();
				}
			}
		}
		return null;
	}

	private static List<Artifact> getArtifacts(MavenProject mavenProject) {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		ArtifactFilter filter = new org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter(
				Artifact.SCOPE_TEST);
		for (Artifact artifact : mavenProject.getArtifacts()) {
			if (filter.include(artifact)) {
				artifacts.add(artifact);
			}
		}
		return artifacts;
	}

	public static URL getArquillianPomFile(boolean isJUnit) {
		try {
			URL url;
			if (isJUnit) {
				url = new URL(ARQUILLIAN_JUNIT_POM_URL);
			} else {
				url = new URL(ARQUILLIAN_TESTNG_POM_URL);
			}
			return FileLocator.resolve(url);
		} catch (MalformedURLException e) {
			ArquillianCoreActivator.log(e);
			return null;
		} catch (IOException e) {
			ArquillianCoreActivator.log(e);
			return null;
		}
	}

	public static void addArquillianSupport(IProject project)
			throws CoreException {
		IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
		MavenProject mavenProject = MavenPlugin.getMaven().readProject(
				pomFile.getLocation().toFile(), new NullProgressMonitor());
		String version = getDependencyVersion(mavenProject,
				ARQUILLIAN_CORE_GROUP_ID,
				ARQUILLIAN_CORE_API_ARTIFACT_ID);
		if (version == null) {
			addArtifacts(pomFile, mavenProject);
			updateProject(project);
		}
	}

	private static URL getArquillianProfileUrl() {
		try {
			URL url = new URL(ARQUILLIAN_PROFILE_POM_URL);
			return FileLocator.resolve(url);
		} catch (MalformedURLException e) {
			ArquillianCoreActivator.log(e);
			return null;
		} catch (IOException e) {
			ArquillianCoreActivator.log(e);
			return null;
		}
	}

	private static void addArtifacts(IFile pomFile, MavenProject mavenProject) throws CoreException {
		URL url = ArquillianUtils.getArquillianPomFile(true);
		InputStream in = null;
		try {
			in = url.openStream();
			Model arquillianModel = MavenPlugin.getMaven().readModel(in);
			mergeModel(pomFile, mavenProject, arquillianModel);
		} catch (IOException e) {
			ArquillianCoreActivator.log(e);
			IStatus status = new Status(IStatus.ERROR, ArquillianCoreActivator.PLUGIN_ID, e
					.getLocalizedMessage(), e);
			throw new CoreException(status);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private static void mergeModel(IFile pomFile, MavenProject mavenProject, Model arquillianModel) {
		IDOMModel model = null;
		try {
			model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForEdit(pomFile);
			if (model == null) {
				model = (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(pomFile);
			}
			addProperties(mavenProject, arquillianModel, model);
			addDependencies(mavenProject, arquillianModel, model);
			addDependencyManagement(mavenProject, arquillianModel, model);
			addPlugins(mavenProject, arquillianModel, model);
			addProfiles(mavenProject, pomFile, arquillianModel);
		} catch (Exception e) {
			ArquillianCoreActivator.log(e);
		} finally {
			if (model != null) {
				try {
					model.save();
				} catch (Exception e) {
					ArquillianCoreActivator.log(e);
				} 
				model.releaseFromEdit();
			}
		}
	}

	private static void addProperties(MavenProject mavenProject,
			Model arquillianModel, IDOMModel model) {
		Properties properties = arquillianModel.getProperties();
		if (properties == null || properties.size() <= 0) {
			return;
		}
		Element root = model.getDocument().getDocumentElement();
		Element propertiesEl = getOrCreateElement(root, PomEdits.PROPERTIES);
		
		Set<Entry<Object,Object>> entries = properties.entrySet();
		for (Entry<Object,Object> entry:entries) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			Element property = findChild(propertiesEl, key);
			if (property == null) {
				createElementWithText(propertiesEl, key, value);
			}
		}
		format(propertiesEl);
	}

	private static void addDependencyManagement(MavenProject mavenProject,
			Model arquillianModel, IDOMModel model) throws CoreException {
		DependencyManagement dependencyMgmt = arquillianModel.getDependencyManagement();
		if (dependencyMgmt == null) {
			return;
		}
		List<Dependency> dependencies = dependencyMgmt.getDependencies();
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
			addManagedDependencies(allDependencies, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
		}
		
		Element root = model.getDocument().getDocumentElement();
		Element dependencyMgmtEl = getOrCreateElement(root, PomEdits.DEPENDENCY_MANAGEMENT);
		Element dependenciesEl = getOrCreateElement(dependencyMgmtEl, PomEdits.DEPENDENCIES);
		for (Dependency dependency:dependencies) {
			
			if (!managedDependencyExists(dependency, allDependencies)) {
				addOrUpdateDependency(dependenciesEl,
						dependency.getGroupId(), dependency.getArtifactId(),
						dependency.getVersion(), dependency.getType(),
						dependency.getScope(), dependency.getClassifier());
			}
		}
		format(dependencyMgmtEl);
	}

	private static boolean managedDependencyExists(Dependency dependency,
			List<Dependency> allDependencies) {
		String gid = dependency.getGroupId();
		String aid = dependency.getArtifactId();
		for (Dependency dep:allDependencies) {
			String groupId = dep.getGroupId();
			if ( (gid == null && groupId == null) ||
					(groupId != null && groupId.equals(gid)) ) {
				String artifactId = dep.getArtifactId();
				if ( (aid == null && artifactId == null) || 
						(artifactId != null && artifactId.equals(aid)) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static void addManagedDependencies(List<Dependency> allDependencies,
			String groupId, String artifactId, String version) throws CoreException {
		Model model = getModel(groupId, artifactId, version);
	    if (model == null) {
	    	return;
	    }
		DependencyManagement dependencyMgmt = model.getDependencyManagement();
		if (dependencyMgmt == null) {
			return;
		}
		List<Dependency> dependencies = dependencyMgmt.getDependencies();
		if (dependencies != null) {
			allDependencies.addAll(dependencies);
		}

	    Parent parent = model.getParent();
	    if (parent != null) {
	    	addManagedDependencies(allDependencies, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
	    }
	}

	private static Element getOrCreateElement(Element parent, String name) {
		Element element = findChild(parent, name);
		if (element == null) {
			element = createElement(parent, name);
		}
		return element;
	}

	private static void addPlugins(MavenProject mavenProject,
			Model arquillianModel, IDOMModel model) throws CoreException {
		Build build = arquillianModel.getBuild();
		if (build == null) {
			return;
		}
		List<Plugin> plugins = build.getPlugins();
		Element root = model.getDocument().getDocumentElement();
		Element buildEl = getOrCreateElement(root, PomEdits.BUILD);
		Element pluginsEl = getOrCreateElement(buildEl, PomEdits.PLUGINS);
		for (Plugin plugin:plugins) {
			if (!pluginExists(model, plugin)) {
				PomHelper.createPlugin(pluginsEl, plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
			}
		}
		fixCompilerPlugin(model);
		format(buildEl);
	}

	private static boolean pluginExists(IDOMModel model, Plugin plugin) throws CoreException {
		if (model == null) {
			return false;
		}
		Element pluginEl = getPomPlugin(model, plugin.getGroupId(), plugin.getArtifactId());
		return pluginEl != null;
	}

	private static boolean pluginExists(Model model, Plugin plugin) throws CoreException {
		if (model == null) {
			return false;
		}
		Build build = model.getBuild();
		if (build != null) {
			List<Plugin> plugins = build.getPlugins();
			String gid = plugin.getGroupId();
			if (gid == null) {
				gid = MAVEN_GROUP_ID;
			}
			String aid = plugin.getArtifactId();
			for (Plugin p : plugins) {
				String agid = p.getGroupId();
				if (agid == null) {
					agid = MAVEN_GROUP_ID;
				}
				if (!agid.equals(gid)) {
					continue;
				}
				if (aid != null && aid.equals(p.getArtifactId())) {
					return true;
				}
			}
		}
//		Parent parent = model.getParent();
//		if (parent != null) {
//			Model parentModel = getModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
//			return pluginExists(parentModel, plugin);
//		}
		return false;
	}

	private static void addDependencies(MavenProject mavenProject, Model arquillianModel, IDOMModel model) {
		List<Dependency> dependencies = arquillianModel.getDependencies();
		if (dependencies == null || dependencies.size() <= 0) {
			return;
		}
		Element root = model.getDocument().getDocumentElement();
		Element dependenciesEl = getOrCreateElement(root, PomEdits.DEPENDENCIES);
		for (Dependency dependency:dependencies) {
			String version = getDependencyVersion(mavenProject, dependency.getGroupId(), dependency.getArtifactId());
			if (version == null) {
				addOrUpdateDependency(dependenciesEl,
						dependency.getGroupId(), dependency.getArtifactId(),
						dependency.getVersion(), dependency.getType(),
						dependency.getScope(), dependency.getClassifier());
			}
		}
		format(dependenciesEl);
	}

	private static void addProfiles(MavenProject mavenProject, IFile pomFile, Model arquillianModel) throws CoreException {
		List<Profile> arquillianProfiles = arquillianModel.getProfiles();
		if (arquillianProfiles == null || arquillianProfiles.size() <= 0) {
			return;
		}
		Model projectModel = mavenProject.getModel();
		List<String> allProfiles = getProfiles(projectModel);
		
		PomResourceImpl projectResource = MavenCoreActivator.loadResource(pomFile);
		PomResourceImpl profileResource = MavenCoreActivator.loadResource(getArquillianProfileUrl());
		EList<org.eclipse.m2e.model.edit.pom.Profile> profiles = profileResource.getModel().getProfiles();
		boolean save = false;
		for (org.eclipse.m2e.model.edit.pom.Profile profile:profiles) {
			if (!allProfiles.contains(profile.getId())) {
				org.eclipse.m2e.model.edit.pom.Profile newProfile = EcoreUtil.copy(profile);
				projectResource.getModel().getProfiles().add(newProfile);
				save = true;
			}
		}
		if (save) {
			try {
				Map<String,String> options = new HashMap<String,String>();
				options.put(XMIResource.OPTION_ENCODING, MavenCoreActivator.ENCODING);
				projectResource.save(options);
			} catch (IOException e) {
				MavenCoreActivator.log(e);
			} finally {
				projectResource.unload();
				profileResource.unload();
			}
		}
	}

	private static List<String> getProfiles(Model projectModel) throws CoreException {
		List<String> allProfiles = new ArrayList<String>();
		// settings profiles
		Settings settings = MavenPlugin.getMaven().getSettings();
		List<org.apache.maven.settings.Profile> settingsProfiles = settings.getProfiles();
		for(org.apache.maven.settings.Profile profile:settingsProfiles) {
			allProfiles.add(profile.getId());
		}
		// project profiles
		List<Profile> projectProfiles = projectModel.getProfiles();
		for (Profile profile:projectProfiles) {
			allProfiles.add(profile.getId());
		}
		Parent parent = projectModel.getParent();
		if (parent != null) {
			addProfiles(allProfiles, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
		}
		return allProfiles;
	}


	private static void addProfiles(List<String> allProfiles, String groupId, String artifactId, String version) throws CoreException {
		Model model = getModel(groupId, artifactId, version);
	    if (model == null) {
	    	return;
	    }
		List<Profile> profiles = model.getProfiles();
	    for(Profile profile:profiles) {
	    	allProfiles.add(profile.getId());
	    }
	    Parent parent = model.getParent();
	    if (parent != null) {
	    	addProfiles(allProfiles, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
	    }
	}

	private static Model getModel(String groupId, String artifactId, String version) throws CoreException {
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);
		Model model;
	    if (facade != null) {
	    	model = facade.getMavenProject().getModel();
	    } else {
	    	IMaven maven = MavenPlugin.getMaven();
	    	List<ArtifactRepository> repos = maven.getArtifactRepositories();
		    Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, repos, new NullProgressMonitor()); //$NON-NLS-1$
		    File file = artifact.getFile();
		    if(file == null) {
		      return null;
		    }
		    
		    model = maven.readModel(file);
	    }
		return model;
	}

	private static void updateProject(IProject project) {
		if (project != null && project.isAccessible()) {
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE,
						new NullProgressMonitor());
			} catch (CoreException e) {
				// ignore
			}
			Job updateJob = new UpdateMavenProjectJob(project);
			updateJob.schedule();
		}
	}
	
	private static void fixCompilerPlugin(IDOMModel model) {
		Element compiler = getPomPlugin(model, null, MAVEN_COMPILER_ARTIFACT_ID);
		if (compiler != null) {
			Element configuration = findChild(compiler, PomEdits.CONFIGURATION);
			if (configuration == null) {
				configuration = createElement(compiler, PomEdits.CONFIGURATION);
			}
			Element source = findChild(configuration, MAVEN_COMPILER_SOURCE_NODE);
			if (source == null) {
				createElementWithText(configuration, MAVEN_COMPILER_SOURCE_NODE,
						MAVEN_COMPILER_SOURCE_LEVEL);
			} else {
				setText(source, MAVEN_COMPILER_SOURCE_LEVEL);
			}
			Element target = findChild(configuration, MAVEN_COMPILER_TARGET_NODE);
			if (target == null) {
				createElementWithText(configuration, MAVEN_COMPILER_TARGET_NODE,
						MAVEN_COMPILER_TARGET_LEVEL);
			} else {
				setText(target, MAVEN_COMPILER_TARGET_LEVEL);
			}
			format(configuration);
		}
	}

	private static Element getPomPlugin(IDOMModel model, String groupId, String artifactId) {
		if (groupId == null) {
			groupId = MAVEN_GROUP_ID;
		}
		IDOMDocument document = model.getDocument();
		Element element = document.getDocumentElement();
		Element build = findChild(element, PomEdits.BUILD);
		if (build != null) {
			Element pluginsNode = findChild(build, PomEdits.PLUGINS);
			List<Element> plugins = findChilds(pluginsNode, PomEdits.PLUGIN);
			for (Element plugin : plugins) {
				Element pluginArtifactId = findChild(plugin, PomEdits.ARTIFACT_ID);
				String aid = getTextValue(pluginArtifactId);
				Element pluginGroupId = findChild(plugin, PomEdits.GROUP_ID);
				String gid = pluginGroupId == null ? MAVEN_GROUP_ID : getTextValue(pluginGroupId);
				if (artifactId.equals(aid) && (groupId.equals(gid))) {
					return plugin;
				}
			}
		}
		return null;
	}

	private static class UpdateMavenProjectJob extends WorkspaceJob {

		private final IProject project;

		public UpdateMavenProjectJob(IProject project) {
			super("Updating Maven Project");
			this.project = project;
			setRule(MavenPlugin.getProjectConfigurationManager().getRule());
		}

		public IStatus runInWorkspace(IProgressMonitor monitor) {
			IProjectConfigurationManager configurationManager = MavenPlugin
					.getProjectConfigurationManager();
			boolean autoBuilding = ResourcesPlugin.getWorkspace()
					.isAutoBuilding();
			monitor.beginTask(getName(), 1);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			try {
				MavenUpdateRequest request = new MavenUpdateRequest(project,
						true, false);
				configurationManager.updateProjectConfiguration(request,
						monitor);
				project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
				if (autoBuilding) {
					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				}
			} catch (Exception e) {
				IStatus st = new Status(IStatus.ERROR,
						ArquillianCoreActivator.PLUGIN_ID,
						e.getLocalizedMessage(), e);
				ArquillianCoreActivator.log(e);
				return st;
			}

			return Status.OK_STATUS;
		}
	}
}
