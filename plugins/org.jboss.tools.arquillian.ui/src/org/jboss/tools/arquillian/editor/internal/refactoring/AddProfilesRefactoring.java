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
package org.jboss.tools.arquillian.editor.internal.refactoring;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.refactoring.RangeComparator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A refactoring to add Arquillian profiles to project
 * 
 * @author snjeza
 * 
 */
public class AddProfilesRefactoring extends Refactoring {

	private IProject project;
	private List<String> profiles;

	/**
	 * @param project
	 */
	public AddProfilesRefactoring(IProject project, List<String> profiles) {
		super();
		this.project = project;
		this.profiles = profiles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
	public String getName() {
		return "Add Arquillian profiles";
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
		IFile file = getFile();
		if (file == null || !file.exists()) {
			IStatus s = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID,
					"The pom.xml file does not exist");
			return RefactoringStatus.create(s);
		}
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry()
				.create(project, new NullProgressMonitor());
		if (facade == null) {
			IStatus s = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID,
					"The project is not a valid maven project");
			return RefactoringStatus.create(s);
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
		IFile file = getFile();
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
		MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
		List<Operation> operations = new ArrayList<Operation>();
		if (profiles != null) {
			operations.add(new AddProfiles(mavenProject, profiles));
		}
		CompoundOperation compound = new CompoundOperation(operations.toArray(new Operation[0]));			
		return createChange(file, compound, getName());
	}

	private Change createChange(IFile file, Operation operation, String label) throws CoreException {
		IStructuredModel model = null;
		try {
			model = StructuredModelManager.getModelManager().getModelForRead(file);
			IDocument document = model.getStructuredDocument();
			boolean existing = isOpened(document);
			IStructuredModel tempModel = StructuredModelManager.getModelManager().createUnManagedStructuredModelFor(
							"org.eclipse.m2e.core.pomFile"); //$NON-NLS-1$
			tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), document.get());
			IDocument tempDocument = tempModel.getStructuredDocument();
			performOnDOMDocument(new OperationTuple((IDOMModel) tempModel, operation));
			TextChange change = createChange(existing ? null : file, document, tempDocument, label);
			return change;
		} catch (Exception e) {
			ArquillianUIActivator.log(e);
			throw new CoreException(new Status(IStatus.ERROR,
					ArquillianUIActivator.PLUGIN_ID,
					"An error occurred creating change", e));
		} finally {
			if (model != null) {
				model.releaseFromRead();
			}
		}
	}
	
	private TextChange createChange(IFile oldFile, IDocument oldDocument,
			IDocument newDocument, String label) {
		TextChange change = oldFile == null ? new DocumentChange(label,
				oldDocument) : new TextFileChange(label, oldFile);
		MultiTextEdit textEdit = new MultiTextEdit();
		change.setEdit(textEdit);
		String newText = newDocument.get();
		String oldText = oldDocument.get();
		if (!newText.equals(oldText)) {
			IRangeComparator right = new RangeComparator(oldText);
			IRangeComparator left = new RangeComparator(newText);
			RangeDifference[] differences = RangeDifferencer.findDifferences(right, left);
			for (RangeDifference difference : differences) {
				int rightStart = difference.rightStart();
				int rightEnd = difference.rightEnd();
				String text = newText.substring(rightStart, rightEnd);
				int leftStart = difference.leftStart();
				int leftLength = difference.leftLength();
				textEdit.addChild(new ReplaceEdit(leftStart, leftLength, text));
			}
		}
		return change;
	}

	private static boolean isOpened(IDocument document) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench()
				.getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference ref : page.getEditorReferences()) {
					IEditorPart editor = ref.getEditor(false);
					if (editor != null) {
						IDocument doc = (IDocument) editor
								.getAdapter(IDocument.class);
						if (doc != null && doc.equals(document)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private IFile getFile() throws CoreException {
		if (project == null || !project.hasNature(IMavenConstants.NATURE_ID)) {
			return null;
		}
		return project.getFile(IMavenConstants.POM_FILE_NAME);
	}

	public IProject getProject() {
		return project;
	}

	private static class AddProfiles implements Operation {

		private MavenProject mavenProject;
		private List<String> profiles;
		
		public AddProfiles(MavenProject mavenProject, List<String> profiles) {
			this.mavenProject = mavenProject;
			this.profiles = profiles;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation#process
		 * (org.w3c.dom.Document)
		 */
		public void process(Document document) {
			List<String> selectedProfiles = profiles;
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
				generateProfile(profiles, container);
			}
		}

		private void generateProfile(Element profiles, Container container) {
			if (container == null) {
				return;
			}
			String id = container.getId();
			Element profile = createElement(profiles, PomEdits.PROFILE);
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
					if (isManaged(fd.getGroup_id(), fd.getArtifact_id())) {
						version = null;
					} else {
						version = getVersion(fd.getGroup_id(), fd.getArtifact_id());
					}
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
		
		private boolean isManaged(String groupId, String artifactId) {
			DependencyManagement depMgmt = mavenProject.getDependencyManagement();
			List<Dependency> mgmtDeps = depMgmt.getDependencies();
			for (Dependency mgmtDep:mgmtDeps) {
				if (groupId.equals(mgmtDep.getGroupId()) && artifactId.equals(mgmtDep.getArtifactId())) {
					return true;
				}
			}
			return false;
		}
		
	}
	public List<String> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<String> profiles) {
		this.profiles = profiles;
	}

}
