package org.jboss.tools.arquillian.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.commands.AddArquillianCommandHandler;

/**
 * A wizard for creating test cases.
 */
public class NewArquillianJUnitTestWizard extends ArquillianWizard {

	public final static String JUNIT4_ANNOTATION_NAME= "org.junit.Test"; //$NON-NLS-1$
	
	private NewArquillianJUnitTestCasePageOne newArquillianJUnitTestCasePageOne;
	private NewArquillianJUnitTestCasePageTwo newArquillianJUnitTestCasePageTwo;
	private NewArquillianJUnitTestCaseDeploymentPage newArquillianJUnitTestCaseDeploymentPage;

	public NewArquillianJUnitTestWizard() {
		super();
		setWindowTitle("New Arquillian JUnit Test Case");
		initDialogSettings();
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/arquillian_icon64.png"));
	}

	/*
	 * @see Wizard#createPages
	 */
	@Override
	public void addPages() {
		super.addPages();
		newArquillianJUnitTestCasePageTwo = new NewArquillianJUnitTestCasePageTwo();
		newArquillianJUnitTestCasePageOne = new NewArquillianJUnitTestCasePageOne(newArquillianJUnitTestCasePageTwo);
		addPage(newArquillianJUnitTestCasePageOne);
		newArquillianJUnitTestCasePageOne.init(getSelection());
		addPage(newArquillianJUnitTestCasePageTwo);
		newArquillianJUnitTestCaseDeploymentPage = new NewArquillianJUnitTestCaseDeploymentPage();
		addPage(newArquillianJUnitTestCaseDeploymentPage);
	}

	/*
	 * @see Wizard#performFinish
	 */
	@Override
	public boolean performFinish() {
		IJavaProject project= newArquillianJUnitTestCasePageOne.getJavaProject();
		IRunnableWithProgress runnable= newArquillianJUnitTestCasePageOne.getRunnable();
		try {
			if (!ArquillianSearchEngine.hasArquillianType(project)) {
				runnable= addArquillianToClasspath(project, runnable);
			}
		} catch (OperationCanceledException e) {
			return false;
		}
		
		if (finishPage(runnable)) {
			IType newClass= newArquillianJUnitTestCasePageOne.getCreatedType();
			IResource resource= newClass.getCompilationUnit().getResource();
			if (resource != null) {
				selectAndReveal(resource);
				openResource(resource);
			}
			return true;
		}
		return false;
	}

	private IRunnableWithProgress addArquillianToClasspath(IJavaProject project, final IRunnableWithProgress runnable) {
		ClasspathFixProposal[] fixProposals = new ArquillianClasspathFixProposal[1];
		fixProposals[0] = new ArquillianClasspathFixProposal(project, 15);
		
		ClasspathFixSelectionDialog dialog = new ClasspathFixSelectionDialog(getShell(), project, fixProposals);
		if (dialog.open() != 0) {
			throw new OperationCanceledException();
		}

		final ClasspathFixProposal fix = dialog.getSelectedClasspathFix();
		if (fix != null) {
			return new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					monitor.beginTask("Create Arquillian JUnit test case", 4);
					try {
						Change change= fix.createChange(new SubProgressMonitor(monitor, 1));
						new PerformChangeOperation(change).run(new SubProgressMonitor(monitor, 1));

						runnable.run(new SubProgressMonitor(monitor, 2));
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			};
		}
		return runnable;
	}

	private static class ClasspathFixSelectionDialog extends MessageDialog implements SelectionListener, IDoubleClickListener {

		static class ClasspathFixLabelProvider extends LabelProvider {

			@Override
			public Image getImage(Object element) {
				if (element instanceof ClasspathFixProposal) {
					ClasspathFixProposal classpathFixProposal= (ClasspathFixProposal) element;
					return classpathFixProposal.getImage();
				}
				return null;
			}

			@Override
			public String getText(Object element) {
				if (element instanceof ClasspathFixProposal) {
					ClasspathFixProposal classpathFixProposal= (ClasspathFixProposal) element;
					return classpathFixProposal.getDisplayString();
				}
				return null;
			}
		}


		private final ClasspathFixProposal[] fFixProposals;
		private final IJavaProject fProject;

		private TableViewer fFixSelectionTable;

		private Button fNoActionRadio;
		private Button fOpenBuildPathRadio;
		private Button fPerformFix;

		private ClasspathFixProposal fSelectedFix;

		public ClasspathFixSelectionDialog(Shell parent, IJavaProject project, ClasspathFixProposal[] fixProposals) {
			super(parent, "New Arquillian JUnit test case", null, getDialogMessage(), MessageDialog.QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
			fProject= project;
			fFixProposals= fixProposals;
			fSelectedFix= null;
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		private static String getDialogMessage() {
			return "Arquillian JUnit is not on the build path. Do you want to add it?";
		}

		@Override
		protected Control createCustomArea(Composite composite) {
			fNoActionRadio= new Button(composite, SWT.RADIO);
			fNoActionRadio.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
			fNoActionRadio.setText("&Not now");
			fNoActionRadio.addSelectionListener(this);

			fOpenBuildPathRadio= new Button(composite, SWT.RADIO);
			fOpenBuildPathRadio.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
			fOpenBuildPathRadio.setText("&Open the build path property page");
			fOpenBuildPathRadio.addSelectionListener(this);

			if (fFixProposals.length > 0) {

				fPerformFix= new Button(composite, SWT.RADIO);
				fPerformFix.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
				fPerformFix.setText("&Perform the following action:");
				fPerformFix.addSelectionListener(this);

				fFixSelectionTable= new TableViewer(composite, SWT.SINGLE | SWT.BORDER);
				fFixSelectionTable.setContentProvider(new ArrayContentProvider());
				fFixSelectionTable.setLabelProvider(new ClasspathFixLabelProvider());
				fFixSelectionTable.setComparator(new ViewerComparator());
				fFixSelectionTable.addDoubleClickListener(this);
				fFixSelectionTable.setInput(fFixProposals);
				fFixSelectionTable.setSelection(new StructuredSelection(fFixProposals[0]));

				GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint= convertHeightInCharsToPixels(4);
				gridData.horizontalIndent= convertWidthInCharsToPixels(2);

				fFixSelectionTable.getControl().setLayoutData(gridData);

				fNoActionRadio.setSelection(false);
				fOpenBuildPathRadio.setSelection(false);
				fPerformFix.setSelection(true);

			} else {
				fNoActionRadio.setSelection(true);
				fOpenBuildPathRadio.setSelection(false);
			}

			updateEnableStates();

			return composite;
		}

		private void updateEnableStates() {
			if (fPerformFix != null) {
				fFixSelectionTable.getTable().setEnabled(fPerformFix.getSelection());
			}
		}

		private static final String BUILD_PATH_PAGE_ID= "org.eclipse.jdt.ui.propertyPages.BuildPathsPropertyPage"; //$NON-NLS-1$
		private static final String BUILD_PATH_BLOCK= "block_until_buildpath_applied"; //$NON-NLS-1$

		@Override
		protected void buttonPressed(int buttonId) {
			fSelectedFix= null;
			if (buttonId == 0) {
				if (fNoActionRadio.getSelection()) {
					// nothing to do
				} else if (fOpenBuildPathRadio.getSelection()) {
					String id= BUILD_PATH_PAGE_ID;
					Map<String, Boolean> input= new HashMap<String, Boolean>();
					input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
					if (PreferencesUtil.createPropertyDialogOn(getShell(), fProject, id, new String[] { id }, input).open() != Window.OK) {
						return;
					}
				} else if (fFixSelectionTable != null) {
					IStructuredSelection selection= (IStructuredSelection) fFixSelectionTable.getSelection();
					Object firstElement= selection.getFirstElement();
					if (firstElement instanceof ClasspathFixProposal) {
						fSelectedFix= (ClasspathFixProposal) firstElement;
					}
				}
			}
			super.buttonPressed(buttonId);
		}

		public ClasspathFixProposal getSelectedClasspathFix() {
			return fSelectedFix;
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			updateEnableStates();
		}

		public void widgetSelected(SelectionEvent e) {
			updateEnableStates();
		}

		public void doubleClick(DoubleClickEvent event) {
			okPressed();

		}
	}

	public NewArquillianJUnitTestCasePageOne getNewArquillianJUnitTestCasePageOne() {
		return newArquillianJUnitTestCasePageOne;
	}

	private static class ArquillianClasspathFixProposal extends ClasspathFixProposal {

		private final int fRelevance;
		private final IJavaProject fProject;
		public ArquillianClasspathFixProposal(IJavaProject project, int relevance) {
			fProject= project;
			fRelevance= relevance;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return ArquillianConstants.ADD_ARQUILLIAN_SUPPORT;
		}

		@Override
		public Change createChange(IProgressMonitor monitor) throws CoreException {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			new AddArquillianCommandHandler().execute(fProject.getProject());
			
			return new NullChange();
		}

		@Override
		public String getDisplayString() {
			return ArquillianConstants.ADD_ARQUILLIAN_SUPPORT;
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
		}

		@Override
		public int getRelevance() {
			return fRelevance;
		}
	}

	public NewArquillianJUnitTestCasePageTwo getNewArquillianJUnitTestCasePageTwo() {
		return newArquillianJUnitTestCasePageTwo;
	}

	public NewArquillianJUnitTestCaseDeploymentPage getNewArquillianJUnitTestCaseDeploymentPage() {
		return newArquillianJUnitTestCaseDeploymentPage;
	}

}
