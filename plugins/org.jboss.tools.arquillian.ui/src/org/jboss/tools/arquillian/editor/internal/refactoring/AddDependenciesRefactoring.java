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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
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
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
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
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.refactoring.AddDependencies;
import org.jboss.tools.arquillian.ui.internal.refactoring.RangeComparator;

/**
 * A refactoring to add Arquillian dependencies to project
 * 
 * @author snjeza
 * 
 */
public class AddDependenciesRefactoring extends Refactoring {

	private IProject project;
	private List<Dependency> dependencies;

	/**
	 * @param project
	 */
	public AddDependenciesRefactoring(IProject project, List<Dependency> dependencies) {
		super();
		this.project = project;
		this.dependencies = dependencies;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
	public String getName() {
		return "Add Arquillian dependencies";
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
		if (dependencies != null) {
			operations.add(new AddDependencies(dependencies, mavenProject));
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

	public List<Dependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

}
