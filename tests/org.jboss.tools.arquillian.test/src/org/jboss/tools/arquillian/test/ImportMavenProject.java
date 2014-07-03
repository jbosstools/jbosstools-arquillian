/*************************************************************************************
 * Copyright (c) 2008-2011 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue;
import org.eclipse.m2e.core.project.AbstractProjectScanner;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.jboss.tools.maven.ui.Activator;
import org.jboss.tools.project.examples.ProjectExamplesActivator;
import org.jboss.tools.project.examples.model.ProjectExample;
import org.jboss.tools.project.examples.model.ProjectExampleWorkingCopy;

/**
 * @author snjeza
 * 
 */
public class ImportMavenProject {

	public boolean importProject(ProjectExampleWorkingCopy projectDescription, File file,
			Map<String, Object> propertiesMap, IPath rootPath) throws Exception {
		List<ProjectExample> projects = new ArrayList<ProjectExample>();
		projects.add(projectDescription);
		IPath mavenProjectsRoot = rootPath;
		String projectName = projectDescription.getName();
		IPath path = mavenProjectsRoot.append(projectName);
		File destination = new File(path.toOSString());
		if (destination.exists()) {
			boolean deleted = deleteDirectory(destination);
			if (!deleted) {
				throw new RuntimeException( "Cannot delete the '" + destination + "' file.");
				
			}
		}
		boolean ok = false;
		IProgressMonitor monitor = new NullProgressMonitor();
		destination = destination.getParentFile();
		if (file.isFile()) {
			ok = ProjectExamplesActivator.extractZipFile(file, destination, monitor);
		}
		else if (file.isDirectory()) {
			destination.mkdirs();
			IFileStore descStore = EFS.getLocalFileSystem().fromLocalFile(destination);
			IFileStore srcStore = EFS.getLocalFileSystem().fromLocalFile(file);
			try {
				srcStore.copy(descStore, EFS.OVERWRITE, monitor);
				ok = true;
			} catch (Exception e) {
				ArquillianTestActivator.log(e);
			}
		}
		monitor.setTaskName("");
		if (monitor.isCanceled()) {
			return false;
		}
		if (!ok) {
			throw new RuntimeException("Cannot extract/copy the archive.");
		}
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		boolean configureSeam = store.getBoolean(Activator.CONFIGURE_SEAM);
		boolean configureJSF = store.getBoolean(Activator.CONFIGURE_JSF);
		boolean configurePortlet = store.getBoolean(Activator.CONFIGURE_PORTLET);
		boolean configureJSFPortlet = store.getBoolean(Activator.CONFIGURE_JSFPORTLET);
		boolean configureSeamPortlet = store.getBoolean(Activator.CONFIGURE_SEAMPORTLET);
		boolean configureCDI = store.getBoolean(Activator.CONFIGURE_CDI);
		boolean configureHibernate = store.getBoolean(Activator.CONFIGURE_HIBERNATE);
		boolean configureJaxRs = store.getBoolean(Activator.CONFIGURE_JAXRS);
		List<String> projectNames;
		try {
			store.setValue(Activator.CONFIGURE_SEAM, false);
			store.setValue(Activator.CONFIGURE_JSF, false);
			store.setValue(Activator.CONFIGURE_PORTLET, false);
			store.setValue(Activator.CONFIGURE_JSFPORTLET, false);
			store.setValue(Activator.CONFIGURE_SEAMPORTLET, false);
			store.setValue(Activator.CONFIGURE_CDI, false);
			store.setValue(Activator.CONFIGURE_HIBERNATE, false);
			store.setValue(Activator.CONFIGURE_JAXRS, false);
			projectNames = importMavenProjects(destination, projectDescription, monitor);
		} finally {
			store.setValue(Activator.CONFIGURE_SEAM, configureSeam);
			store.setValue(Activator.CONFIGURE_JSF, configureJSF);
			store.setValue(Activator.CONFIGURE_PORTLET, configurePortlet);
			store.setValue(Activator.CONFIGURE_JSFPORTLET, configureJSFPortlet);
			store.setValue(Activator.CONFIGURE_SEAMPORTLET, configureSeamPortlet);
			store.setValue(Activator.CONFIGURE_CDI, configureCDI);
			store.setValue(Activator.CONFIGURE_HIBERNATE, configureHibernate);
			store.setValue(Activator.CONFIGURE_JAXRS, configureJaxRs);
		}
		
		List<String> includedProjects = projectDescription.getIncludedProjects();
		if (includedProjects == null) {
			includedProjects = new ArrayList<String>();
			projectDescription.setIncludedProjects(includedProjects);
		}
		
		if (projectNames != null && projectNames.size() > 0) {
			includedProjects.clear();
			includedProjects.addAll(projectNames);
		} else {
			if (!includedProjects.contains(projectName)) {
				includedProjects.add(projectName);
			}
		}
		waitForMavenJobs(monitor);

		updateMavenConfiguration(projectName, includedProjects, monitor);
		return true;
	}

	private static void waitForMavenJobs(IProgressMonitor monitor) throws InterruptedException, CoreException {
		Job[] jobs = Job.getJobManager().find(null);
		if (jobs != null) {
			for (Job job : jobs) {
				if (job instanceof IBackgroundProcessingQueue) {
					IBackgroundProcessingQueue queue = (IBackgroundProcessingQueue) job;
					queue.join();
					if (!queue.isEmpty()) {
						IStatus status = queue.run(monitor);
						if (!status.isOK()) {
							throw new CoreException(status);
						}
					}
					if (queue.isEmpty()) {
						queue.cancel();
					}
				}
			}
		}
	  }

	private List<String> importMavenProjects(final File destination,
			final ProjectExample projectDescription, IProgressMonitor monitor) throws Exception {
		List<String> projectNames = new ArrayList<String>();
		AbstractProjectScanner<MavenProjectInfo> projectScanner = getProjectScanner(destination);
		projectScanner.run(monitor);
		List<MavenProjectInfo> mavenProjects = projectScanner.getProjects();
		List<MavenProjectInfo> infos = new ArrayList<MavenProjectInfo>();
		infos.addAll(mavenProjects);
		addMavenProjects(infos, mavenProjects);
		final List<IProject> existingProjects = new ArrayList<IProject>();
		ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
		String profiles = projectDescription.getDefaultProfiles();
		if (profiles != null && profiles.trim().length() > 0) {
			importConfiguration.getResolverConfiguration().setActiveProfiles(
					profiles);
		}
		for (MavenProjectInfo info : infos) {
			String projectName = getProjectName(info, importConfiguration);
			IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(projectName);
			if (project != null && project.exists()) {
				existingProjects.add(project);
			}
		}
		if (existingProjects.size() > 0) {
			for (IProject project : existingProjects) {
				try {
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (Exception e) {
					// ignore
				}
				project.delete(true, true, monitor);
			}
		}
		List<String> includedProjects = projectDescription
				.getIncludedProjects();
		if (includedProjects != null && includedProjects.size() > 0) {
			List<MavenProjectInfo> newInfos = new ArrayList<MavenProjectInfo>();
			for (MavenProjectInfo info : infos) {
				Model model = info.getModel();
				if (model != null && model.getArtifactId() != null
						&& model.getArtifactId().trim().length() > 0) {
					for (String includedProject : includedProjects) {
						if (model.getArtifactId().equals(includedProject)) {
							newInfos.add(info);
						}
					}
				}
			}
			infos = newInfos;
		}
		MavenPlugin.getProjectConfigurationManager().importProjects(infos,
				importConfiguration, monitor);
		for (MavenProjectInfo info : infos) {
			Model model = info.getModel();
			if (model != null && model.getArtifactId() != null
					&& model.getArtifactId().trim().length() > 0) {

				IMavenProjectFacade f = MavenPlugin.getMavenProjectRegistry()
						.getMavenProject(model.getGroupId(),
								model.getArtifactId(), model.getVersion());
				if (f != null && f.getProject() != null) {
					projectNames.add(f.getProject().getName());
				}
			}
		}

		return projectNames;
	}

	private List<MavenProjectInfo> addMavenProjects(List<MavenProjectInfo> infos, List<MavenProjectInfo> mavenProjects) {
		if (mavenProjects == null || mavenProjects.isEmpty()) {
			return mavenProjects;
		}
		for (MavenProjectInfo projectInfo:mavenProjects) {
			Collection<MavenProjectInfo> projects = projectInfo.getProjects();
			if (projects != null && !projects.isEmpty()) {
				for(MavenProjectInfo info:projects) {
					infos.add(info);
				}
				List<MavenProjectInfo> childProjects = new ArrayList<MavenProjectInfo>();
				childProjects.addAll(projects);
				addMavenProjects(infos, childProjects);
			}
		}
		return mavenProjects;
	}

	private AbstractProjectScanner<MavenProjectInfo> getProjectScanner(
			File folder) {
		File root = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile();
		MavenPlugin mavenPlugin = MavenPlugin.getDefault();
		MavenModelManager modelManager = mavenPlugin.getMavenModelManager();
		return new LocalProjectScanner(root, folder.getAbsolutePath(), false,
				modelManager);
	}
	
	private static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		return (path.delete());
	}

	private static String getProjectName(MavenProjectInfo projectInfo,
			ProjectImportConfiguration configuration) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		File pomFile = projectInfo.getPomFile();
		Model model = projectInfo.getModel();
		IMaven maven = MavenPlugin.getMaven();
		if (model == null) {
			model = maven.readModel(pomFile);
			projectInfo.setModel(model);
		}

		String projectName = configuration.getProjectName(model);

		File projectDir = pomFile.getParentFile();
		String projectParent = projectDir.getParentFile().getAbsolutePath();

		if (projectInfo.getBasedirRename() == MavenProjectInfo.RENAME_REQUIRED) {
			File newProject = new File(projectDir.getParent(), projectName);
			if (!projectDir.equals(newProject)) {
				projectDir = newProject;
			}
		} else {
			if (projectParent.equals(root.getLocation().toFile()
					.getAbsolutePath())) {
				projectName = projectDir.getName();
			}
		}
		return projectName;
	}
	
	public static void updateMavenConfiguration(String projectName, List<String> includedProjects,final IProgressMonitor monitor) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project != null && project.isAccessible()) {
			try {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			} catch (CoreException e) {
				// ignore
			}
		}
		IProject[] selectedProjects = new IProject[includedProjects.size()+1];
		selectedProjects[0] = project;
		if (includedProjects.size() > 0) {
			int i = 1;
			
			for (String selectedProjectName:includedProjects) {
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(selectedProjectName);
				selectedProjects[i++] = project;
				try {
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (CoreException e) {
					// ignore
				}
			}
		}
		Job updateJob = new UpdateMavenProjectJob(selectedProjects);
		updateJob.schedule();
		try {
			updateJob.join();
		} catch (InterruptedException e) {
			// ignore
		}
			
	}
}
