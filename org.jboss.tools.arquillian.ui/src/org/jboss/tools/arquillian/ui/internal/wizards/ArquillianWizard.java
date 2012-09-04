/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.ui.internal.wizards;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * The wizard base class for Arquillian wizards.
 * Based on org.eclipse.jdt.internal.junit.wizard.JUnitWizard
 */
public abstract class ArquillianWizard extends Wizard implements INewWizard {

	private IWorkbench fWorkbench;
	protected static final String DIALOG_SETTINGS_KEY= "ArquillianWizards"; //$NON-NLS-1$
	private IStructuredSelection fSelection;

	public ArquillianWizard() {
		setNeedsProgressMonitor(true);
		initializeDefaultPageImageDescriptor();
	}

	/*
	 * @see IWizard#performFinish()
	 */
	@Override
	public abstract boolean performFinish();

	/*
	 * Run a runnable
	 */
	protected boolean finishPage(IRunnableWithProgress runnable) {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(getContainer(), op, ResourcesPlugin.getWorkspace().getRoot());

		} catch (InvocationTargetException e) {
			Shell shell= getShell();
			String title= "New";
			String message= "Creation of element failed.";
			handleException(e, shell, title, message);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	private void handleException(CoreException e, Shell shell, String title, String message) {
		ArquillianUIActivator.log(e);
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, title, message, status);
		} else {
			displayMessageDialog(e.getMessage(), shell, title, message);
		}
	}
	
	private void handleException(InvocationTargetException e, Shell shell, String title, String message) {
		Throwable target= e.getTargetException();
		if (target instanceof CoreException) {
			handleException((CoreException)target, shell, title, message);
		} else {
			ArquillianUIActivator.log(e);
			if (e.getMessage() != null && e.getMessage().length() > 0) {
				displayMessageDialog(e.getMessage(), shell, title, message);
			} else {
				displayMessageDialog(target.getMessage(), shell, title, message);
			}
		}
	}
	
	private void displayMessageDialog(String exceptionMessage, Shell shell, String title, String message) {
		StringWriter msg= new StringWriter();
		if (message != null) {
			msg.write(message);
			msg.write("\n\n"); //$NON-NLS-1$
		}
		if (exceptionMessage == null || exceptionMessage.length() == 0)
			msg.write("See error log for details");
		else
			msg.write(exceptionMessage);
		MessageDialog.openError(shell, title, msg.toString());
	}


	protected void openResource(final IResource resource) {
		if (resource.getType() == IResource.FILE) {
			final IWorkbenchPage activePage= ArquillianUIActivator.getActivePage();
			if (activePage != null) {
				final Display display= Display.getDefault();
				if (display != null) {
					display.asyncExec(new Runnable() {
						public void run() {
							try {
								IDE.openEditor(activePage, (IFile)resource, true);
							} catch (PartInitException e) {
								ArquillianUIActivator.log(e);
							}
						}
					});
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		fWorkbench= workbench;
		fSelection= currentSelection;
	}

	public IStructuredSelection getSelection() {
		return fSelection;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, fWorkbench.getActiveWorkbenchWindow());
	}

	protected void initDialogSettings() {
		IDialogSettings pluginSettings= ArquillianUIActivator.getDefault().getDialogSettings();
		IDialogSettings wizardSettings= pluginSettings.getSection(DIALOG_SETTINGS_KEY);
		if (wizardSettings == null) {
			wizardSettings= new DialogSettings(DIALOG_SETTINGS_KEY);
			pluginSettings.addSection(wizardSettings);
		}
		setDialogSettings(wizardSettings);
	}

	protected abstract void initializeDefaultPageImageDescriptor();
}
