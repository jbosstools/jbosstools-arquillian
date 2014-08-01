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
package org.jboss.tools.arquillian.ui.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.wizards.CreateArquillianDeploymentMethodWizard;

/**
 * 
 * @author snjeza
 *
 */
public class GenerateArquillianDeploymentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection =  HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			Object object = ((IStructuredSelection)selection).getFirstElement();
			if (object instanceof ICompilationUnit) {
				createWizard((ICompilationUnit) object);
			}
			if (object instanceof IType) {
				createWizard(((IType)object).getCompilationUnit());
			}
		}
		if (selection instanceof ITextSelection) {
			ICompilationUnit cu = ArquillianUIUtil.getActiveCompilationUnit();
			if (cu != null) {
				createWizard(cu);
			}
		}
		return null;
	}

	private void createWizard(ICompilationUnit cu) {
		WizardDialog dialog = new WizardDialog(getShell(), new CreateArquillianDeploymentMethodWizard(cu));
		dialog.open();
	}

	private Shell getShell() {
		if (Display.getCurrent() != null) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		return null;
	}

}
