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
package org.jboss.tools.arquillian.core.internal.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
public class ArquillianLaunchConfigurationDelegate extends
		JUnitLaunchConfigurationDelegate {

	private static final String ADD_ARQUILLIAN_SUPPORT = "Add Arquillian Support...";
	public static final String ID = ArquillianCoreActivator.PLUGIN_ID + ".launchconfig"; //$NON-NLS-1$

	@Override
	protected IMember[] evaluateTests(ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		IMember[] tests = super.evaluateTests(configuration, monitor);
		String testMethodName= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, ""); //$NON-NLS-1$
		if (testMethodName.length() > 0) {
			return tests;
		}
		List<IMember> filteredTests = new ArrayList<IMember>();
		for (IMember member:tests) {
			if (member instanceof IType && ArquillianSearchEngine.isArquillianJUnitTest(member, true, true)) {
				filteredTests.add(member);
			}
		}
		
		return filteredTests.toArray(new IMember[0]);
	}

	@Override
	protected void preLaunchCheck(final ILaunchConfiguration configuration,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.preLaunchCheck(configuration, launch, monitor);
		final IStatus[] statuses= new IStatus[2];
		statuses[0] = ArquillianSearchEngine.validateDeployableContainer(getJavaProject(configuration));
		if (!statuses[0].isOK()) {
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					try {
						statuses[1] = fixArquillianLaunch(configuration, statuses[0]);
					} catch (CoreException e) {
						ArquillianCoreActivator.log(e);
					}
				}
			});
			if (statuses[1].getSeverity() == IStatus.CANCEL) {
				monitor.setCanceled(true);
			}
			return;
		}
	}

	private IStatus fixArquillianLaunch(ILaunchConfiguration configuration, IStatus status) throws CoreException {
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
				status.getMessage(), fixProposals);
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
		
		public LaunchFixSelectionDialog(Shell parent, ILaunchConfiguration configuration, IJavaProject project, String message, ClasspathFixProposal[] fixProposals) {
			super(parent, "Arquillian JUnit test", null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
			fConfiguration = configuration;
			fProject= project;
			this.fixProposals= fixProposals;
			selectedFix= null;
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
					openLaunchConfiguration(fConfiguration);
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

	public static void openLaunchConfiguration(ILaunchConfiguration configuration) {
		LaunchConfigurationManager lcManager = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
		LaunchGroupExtension group = lcManager.getLaunchGroup(RemoteDebugActivator.LAUNCH_CATEGORY);
		LaunchConfigurationsDialog dialog = new LaunchConfigurationsDialog(ArquillianUtility.getShell(), group);
		if (configuration != null) {
			IStructuredSelection selection = new StructuredSelection(configuration);
			dialog.setInitialSelection(selection);
			dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION);
		} else {
			dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED);
		}
		dialog.open();
	}


}
