/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.common.jdt.debug.RemoteDebugActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianLaunchShortcut extends JUnitLaunchShortcut {

	private static final String LAUNCH_GROUP_RUN = "org.eclipse.debug.ui.launchGroup.run"; //$NON-NLS-1$
	private static final String RUN_MODE = "run"; //$NON-NLS-1$
	private static final String LAUNCHING_OF_ARQILLIAN_J_UNIT_TESTS_FAILED = "Launching of Arqillian JUnit tests unexpectedly failed. Check log for details.";
	private static final String ARQUILLIAN_J_UNIT_LAUNCH = "Arquillian JUnit Launch";
	private static final String EMPTY_STRING= ""; //$NON-NLS-1$
	
	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
			IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(element);
		//config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, ArquillianRuntimeClasspathProvider.ID);
		return config;
	}

	@Override
	protected String getLaunchConfigurationTypeId() {
		return JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		ITypeRoot element= JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
		if (element != null) {
			IMethod selectedMethod= resolveSelectedMethodName(editor, element);
			if (selectedMethod != null) {
				launch(new Object[] { selectedMethod }, mode);
			} else {
				launch(new Object[] { element }, mode);
			}
		} else {
			showNoTestsFoundDialog();
		}
	}
	
	private IMethod resolveSelectedMethodName(IEditorPart editor, ITypeRoot element) {
		try {
			ISelectionProvider selectionProvider= editor.getSite().getSelectionProvider();
			if (selectionProvider == null)
				return null;

			ISelection selection= selectionProvider.getSelection();
			if (!(selection instanceof ITextSelection))
				return null;

			ITextSelection textSelection= (ITextSelection) selection;

			IJavaElement elementAtOffset= SelectionConverter.getElementAtOffset(element, textSelection);
			if (! (elementAtOffset instanceof IMethod))
				return null;

			IMethod method= (IMethod) elementAtOffset;

			ISourceRange nameRange= method.getNameRange();
			if (nameRange.getOffset() <= textSelection.getOffset()
					&& textSelection.getOffset() + textSelection.getLength() <= nameRange.getOffset() + nameRange.getLength())
				return method;
		} catch (JavaModelException e) {
			// ignore
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	@Override
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			launch(((IStructuredSelection) selection).toArray(), mode);
		} else {
			showNoTestsFoundDialog();
		}
	}
	
	private void showNoTestsFoundDialog() {
		MessageDialog.openInformation(getShell(), ARQUILLIAN_J_UNIT_LAUNCH, "No Arquillian JUnit tests found");
	}

	private static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 0) {
				return windows[0].getShell();
			}
		}
		else {
			return window.getShell();
		}
		return null;
	}
	
	private void launch(Object[] elements, String mode) {
		try {
			IJavaElement elementToLaunch= null;

			if (elements.length == 1) {
				Object selected= elements[0];
				if (!(selected instanceof IJavaElement) && selected instanceof IAdaptable) {
					selected= ((IAdaptable) selected).getAdapter(IJavaElement.class);
				}
				if (selected instanceof IJavaElement) {
					IJavaElement element= (IJavaElement) selected;
					switch (element.getElementType()) {
						case IJavaElement.JAVA_PROJECT:
						case IJavaElement.PACKAGE_FRAGMENT_ROOT:
						case IJavaElement.PACKAGE_FRAGMENT:
							IJavaProject javaProject = element.getJavaProject();
							if (ArquillianSearchEngine.hasArquillianType(javaProject)) {
								elementToLaunch= element;
							}
							break;
						case IJavaElement.TYPE:
							IType type = (IType) element;
							if (ArquillianSearchEngine.isArquillianJUnitTest(type, true, true)) {
								elementToLaunch= type;
							}
						case IJavaElement.METHOD:
							javaProject = element.getJavaProject();
							if (ArquillianSearchEngine.hasArquillianType(javaProject)) {
								elementToLaunch= element;
							}
							break;
						case IJavaElement.CLASS_FILE:
							type = ((IClassFile) element).getType();
							if (ArquillianSearchEngine.isArquillianJUnitTest(type, true, true, false)) {
								elementToLaunch= type;
							}
							break;
						case IJavaElement.COMPILATION_UNIT:
							elementToLaunch= findTypeToLaunch((ICompilationUnit) element, mode);
							break;
					}
				}
			}
			if (elementToLaunch == null) {
				showNoTestsFoundDialog();
				return;
			}
			performLaunch(elementToLaunch, mode);
		} catch (InterruptedException e) {
			// OK, silently move on
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ARQUILLIAN_J_UNIT_LAUNCH, LAUNCHING_OF_ARQILLIAN_J_UNIT_TESTS_FAILED);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), ARQUILLIAN_J_UNIT_LAUNCH, LAUNCHING_OF_ARQILLIAN_J_UNIT_TESTS_FAILED);
		}
	}

	private IType findTypeToLaunch(ICompilationUnit cu, String mode) throws InterruptedException, InvocationTargetException {
		IType[] types= findTypesToLaunch(cu);
		List<IType> arquillianTypes = new ArrayList<IType>();
		for(IType type:types) {
			if (ArquillianSearchEngine.isArquillianJUnitTest(type, true, true)) {
				arquillianTypes.add(type);
			}
		}
		types = arquillianTypes.toArray(new IType[0]);
		if (types.length == 0) {
			return null;
		} else if (types.length > 1) {
			return chooseType(types, mode);
		}
		return types[0];
	}

	private IType[] findTypesToLaunch(ICompilationUnit cu) throws InterruptedException, InvocationTargetException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(cu);
		return TestSearchEngine.findTests(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), cu, testKind);
	}

	private void performLaunch(IJavaElement element, String mode) throws InterruptedException, CoreException {
		ILaunchConfigurationWorkingCopy temparary= createLaunchConfiguration(element);
		ILaunchConfiguration config= findExistingLaunchConfiguration(temparary, mode);
		if (config == null) {
			// no existing found: create a new one
			config= temparary.doSave();
		}
		if (preLaunchCheck(config, mode)) {
			DebugUITools.launch(config, mode);
		}
	}
	
	protected boolean preLaunchCheck(final ILaunchConfiguration configuration, final String mode) throws CoreException {
		final IStatus[] statuses= new IStatus[2];
		statuses[0] = ArquillianSearchEngine.validateDeployableContainer(getJavaProject(configuration));
		if (!statuses[0].isOK()) {
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					try {
						statuses[1] = fixArquillianLaunch(configuration, statuses[0], mode);
					} catch (CoreException e) {
						ArquillianCoreActivator.log(e);
					}
				}
			});
			if (statuses[1].getSeverity() == IStatus.CANCEL) {
				return false;
			}
		}
		return true;
	}

	public IJavaProject getJavaProject(ILaunchConfiguration configuration)
			throws CoreException {
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		if (projectName != null) {
			projectName = projectName.trim();
			if (projectName.length() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				IJavaProject javaProject = JavaCore.create(project);
				if (javaProject != null && javaProject.exists()) {
					return javaProject;
				}
			}
		}
		return null;
	}
	
	private ILaunchConfiguration findExistingLaunchConfiguration(ILaunchConfigurationWorkingCopy temporary, String mode) throws InterruptedException, CoreException {
		List<ILaunchConfiguration> candidateConfigs= findExistingLaunchConfigurations(temporary);

		// If there are no existing configs associated with the IType, create
		// one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the
		// IType, prompt the
		// user to choose one.
		int candidateCount= candidateConfigs.size();
		if (candidateCount == 0) {
			return null;
		} else if (candidateCount == 1) {
			return candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config. A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching
			// anything.
			ILaunchConfiguration config= chooseConfiguration(candidateConfigs, mode);
			if (config != null) {
				return config;
			}
		}
		return null;
	}

	/**
	 * Show a selection dialog that allows the user to choose one of the
	 * specified launch configurations. Return the chosen config, or
	 * <code>null</code> if the user cancelled the dialog.
	 *
	 * @param configList list of {@link ILaunchConfiguration}s
	 * @param mode launch mode
	 * @return ILaunchConfiguration
	 * @throws InterruptedException if cancelled by the user
	 */
	private ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList, String mode) throws InterruptedException {
		IDebugModelPresentation labelProvider= DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(JUnitMessages.JUnitLaunchShortcut_message_selectConfiguration);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.JUnitLaunchShortcut_message_selectDebugConfiguration);
		} else {
			dialog.setMessage(JUnitMessages.JUnitLaunchShortcut_message_selectRunConfiguration);
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		throw new InterruptedException(); // cancelled by user
	}
	
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	private List<ILaunchConfiguration> findExistingLaunchConfigurations(ILaunchConfigurationWorkingCopy temporary) throws CoreException {
		ILaunchConfigurationType configType= temporary.getType();

		ILaunchConfiguration[] configs= getLaunchManager().getLaunchConfigurations(configType);
		String[] attributeToCompare= getAttributeNamesToCompare();

		ArrayList<ILaunchConfiguration> candidateConfigs= new ArrayList<ILaunchConfiguration>(configs.length);
		for (ILaunchConfiguration config : configs) {
			if (hasSameAttributes(config, temporary, attributeToCompare)) {
				candidateConfigs.add(config);
			}
		}
		return candidateConfigs;
	}

	private static boolean hasSameAttributes(ILaunchConfiguration config1, ILaunchConfiguration config2, String[] attributeToCompare) {
		try {
			for (String element : attributeToCompare) {
				String val1= config1.getAttribute(element, EMPTY_STRING);
				String val2= config2.getAttribute(element, EMPTY_STRING);
				if (!val1.equals(val2)) {
					return false;
				}
			}
			return true;
		} catch (CoreException e) {
			// ignore access problems here, return false
		}
		return false;
	}
	private IType chooseType(IType[] types, String mode) throws InterruptedException {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_POST_QUALIFIED));
		dialog.setElements(types);
		dialog.setTitle(JUnitMessages.JUnitLaunchShortcut_dialog_title2);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.JUnitLaunchShortcut_message_selectTestToDebug);
		} else {
			dialog.setMessage(JUnitMessages.JUnitLaunchShortcut_message_selectTestToRun);
		}
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			return (IType) dialog.getFirstResult();
		}
		throw new InterruptedException(); // cancelled by user
	}

	private IStatus fixArquillianLaunch(ILaunchConfiguration configuration, IStatus status, String mode) throws CoreException {
		ClasspathFixProposal[] fixProposals;
		if (status.getCode() == ArquillianSearchEngine.CONTAINER_DEPLOYABLE_CONTAINER_NOT_EXISTS) {
			fixProposals = new ClasspathFixProposal[1];
			fixProposals[0] = new ArquillianLaunchFixProposal(configuration,
					ArquillianConstants.ADD_ARQUILLIAN_SUPPORT_COMMAND,
					ArquillianConstants.ADD_ARQUILLIAN_SUPPORT, true, 15);
		} else {
			fixProposals = new ClasspathFixProposal[2];
			fixProposals[0] = new ArquillianLaunchFixProposal(configuration,
					ArquillianConstants.ADD_ARQUILLIAN_PROFILES_COMMAND,
					ArquillianConstants.ADD_ARQUILLIAN_PROFILES, true, 15);
			fixProposals[1] = new ArquillianLaunchFixProposal(configuration,
					ArquillianConstants.SELECT_MAVEN_PROFILES_COMMAND,
					ArquillianConstants.SELECT_MAVEN_PROFILES, true, 15);
		}
		
		LaunchFixSelectionDialog dialog = new LaunchFixSelectionDialog(
				ArquillianUtility.getShell(),
				configuration,
				ArquillianUtility.getJavaProject(configuration),
				status.getMessage(), fixProposals, mode);
		if (dialog.open() == Window.CANCEL) {
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}
	
	private static class LaunchFixSelectionDialog extends MessageDialog implements SelectionListener, IDoubleClickListener {

		static class LaunchFixLabelProvider extends LabelProvider {

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


		private final ClasspathFixProposal[] fixProposals;
		private final IJavaProject fProject;

		private TableViewer fFixSelectionTable;

		private Button fNoActionRadio;
		private Button fOpenBuildPathRadio;
		private Button fOpenLaunchConfiguration;
		private Button fPerformFix;

		private ArquillianLaunchFixProposal selectedFix;
		
		private IResourceChangeListener resourceChangeListener;
		
		private ILaunchConfiguration fConfiguration;
		private String mode;
		
		public LaunchFixSelectionDialog(Shell parent, ILaunchConfiguration configuration, IJavaProject project, String message, ClasspathFixProposal[] fixProposals, String mode) {
			super(parent, "Arquillian JUnit test", null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
			fConfiguration = configuration;
			fProject= project;
			this.fixProposals= fixProposals;
			selectedFix= null;
			this.mode = mode;
		}

		@Override
		protected boolean isResizable() {
			return true;
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

			fOpenLaunchConfiguration= new Button(composite, SWT.RADIO);
			fOpenLaunchConfiguration.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
			fOpenLaunchConfiguration.setText("Open the &Launch Configuration");
			fOpenLaunchConfiguration.addSelectionListener(this);

			if (fixProposals.length > 0) {

				fPerformFix= new Button(composite, SWT.RADIO);
				fPerformFix.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false));
				fPerformFix.setText("&Perform the following action:");
				fPerformFix.addSelectionListener(this);

				fFixSelectionTable= new TableViewer(composite, SWT.SINGLE | SWT.BORDER);
				fFixSelectionTable.setContentProvider(new ArrayContentProvider());
				fFixSelectionTable.setLabelProvider(new LaunchFixLabelProvider());
				fFixSelectionTable.setComparator(new ViewerComparator());
				fFixSelectionTable.addDoubleClickListener(this);
				fFixSelectionTable.setInput(fixProposals);
				fFixSelectionTable.setSelection(new StructuredSelection(fixProposals[0]));

				GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint= convertHeightInCharsToPixels(4);
				gridData.horizontalIndent= convertWidthInCharsToPixels(2);

				fFixSelectionTable.getControl().setLayoutData(gridData);

				fNoActionRadio.setSelection(false);
				fOpenBuildPathRadio.setSelection(false);
				fOpenLaunchConfiguration.setSelection(false);
				fPerformFix.setSelection(true);

			} else {
				fNoActionRadio.setSelection(true);
				fOpenBuildPathRadio.setSelection(false);
				fOpenLaunchConfiguration.setSelection(false);
			}

			updateEnableStates();

			resourceChangeListener = new IResourceChangeListener() {
				
				@Override
				public void resourceChanged(IResourceChangeEvent event) {
					IStatus status = ArquillianSearchEngine.validateDeployableContainer(fProject);
					if (status.isOK()) {
						Display.getDefault().asyncExec(new Runnable() {
							
							@Override
							public void run() {
								setReturnCode(OK);
								close();
							}
						});
						
					}
				}
			};
			ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_BUILD);
			return composite;
		}

		private void updateEnableStates() {
			if (fPerformFix != null) {
				fFixSelectionTable.getTable().setEnabled(fPerformFix.getSelection());
			}
		}

		@Override
		public boolean close() {
			if (resourceChangeListener != null) {
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
				resourceChangeListener = null;
			}
			return super.close();
		}
		private static final String BUILD_PATH_PAGE_ID= "org.eclipse.jdt.ui.propertyPages.BuildPathsPropertyPage"; //$NON-NLS-1$
		private static final String BUILD_PATH_BLOCK= "block_until_buildpath_applied"; //$NON-NLS-1$

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == IDialogConstants.CANCEL_ID) {
				super.buttonPressed(buttonId);
				return;
			}
			selectedFix= null;
			if (buttonId == 0) {
				if (fNoActionRadio.getSelection()) {
					super.buttonPressed(buttonId);
				} else if (fOpenBuildPathRadio.getSelection()) {
					String id= BUILD_PATH_PAGE_ID;
					Map<String, Boolean> input= new HashMap<String, Boolean>();
					input.put(BUILD_PATH_BLOCK, Boolean.TRUE);
					if (PreferencesUtil.createPropertyDialogOn(getShell(), fProject, id, new String[] { id }, input).open() != Window.OK) {
						return;
					}
				} else if (fOpenLaunchConfiguration.getSelection()) {
					openLaunchConfiguration(fConfiguration, mode);
					setReturnCode(CANCEL);
					close();
				} else if (fFixSelectionTable != null) {
					IStructuredSelection selection= (IStructuredSelection) fFixSelectionTable.getSelection();
					Object firstElement= selection.getFirstElement();
					if (firstElement instanceof ArquillianLaunchFixProposal) {
						selectedFix= (ArquillianLaunchFixProposal) firstElement;
					}
				}
			}
			final ArquillianLaunchFixProposal fix = getSelectedClasspathFix();
			if (fix != null) {
				try {
					IProgressMonitor monitor = new NullProgressMonitor();

					Change change = fix.createChange(monitor);
					new PerformChangeOperation(change).run(monitor);
					IStatus status = ArquillianSearchEngine.validateDeployableContainer(fProject);
					if (status.isOK()) {
						super.okPressed();
					}
				} catch (OperationCanceledException e) {
					cancelPressed();
				} catch (Exception e) {
					ArquillianCoreActivator.log(e);
				}							
			}

		}

		public ArquillianLaunchFixProposal getSelectedClasspathFix() {
			return selectedFix;
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
	
	private static class ArquillianLaunchFixProposal extends ClasspathFixProposal {

		private int relevance;
		private ILaunchConfiguration configuration;
		private String actionId;
		private boolean select;
		private String message;
		
		public ArquillianLaunchFixProposal(ILaunchConfiguration configuration, String actionId, String message,
				boolean select, int relevance) {
			this.configuration = configuration;
			this.relevance= relevance;
			this.actionId = actionId;
			this.select = select;
			this.message = message;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return message;
		}

		@Override
		public Change createChange(IProgressMonitor monitor) throws CoreException {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}
			ArquillianUtility.runAction(configuration, actionId, select);
			
			return new NullChange();
		}

		@Override
		public String getDisplayString() {
			return message;
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
		}

		@Override
		public int getRelevance() {
			return relevance;
		}

		public String getActionId() {
			return actionId;
		}
	}
	
	private static void openLaunchConfiguration(ILaunchConfiguration configuration, String mode) {
		LaunchConfigurationManager lcManager = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
		IStructuredSelection selection = null;
		String category = RemoteDebugActivator.LAUNCH_CATEGORY;
		if (RUN_MODE.equals(mode)) {
			category = LAUNCH_GROUP_RUN;
		}
		if (configuration != null) {
			selection = new StructuredSelection(configuration);
		}
		LaunchGroupExtension group = lcManager.getLaunchGroup(category);
		LaunchConfigurationsDialog dialog = new LaunchConfigurationsDialog(getShell(), group);
		if (selection != null) {
			dialog.setInitialSelection(selection);
			dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION);
		} else {
			dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED);
		}
		dialog.open();
	}

}
