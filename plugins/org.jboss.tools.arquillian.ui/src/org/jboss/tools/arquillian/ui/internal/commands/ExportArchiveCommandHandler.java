/*************************************************************************************
 * Copyright (c) 2008-2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.commands;

import java.io.File;
import java.lang.reflect.Method;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.archives.Archive;

/**
 * 
 * @author snjeza
 *
 */
public class ExportArchiveCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection =  HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			Object object = ((IStructuredSelection)selection).getFirstElement();
			if (object instanceof Archive) {
				Archive archive = (Archive) object;
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setFileName(archive.getInternalName());
				String name = archive.getInternalName();
				int index = name.indexOf('.');
				if (index >= 0) {
					String extension = "*" + name.substring(index);
					String[] extensions = { extension };
					dialog.setFilterExtensions(extensions);
				}
				String path= dialog.open();
				if (path == null) {
					return null;
				}

				final File localFile= new File(path);
				if (localFile.exists()) {
					MessageDialog overwriteDialog= new MessageDialog(
		        		getShell(),
		        		"Export Archive",
		        		null,
		        		path + " already exists. Do you want to replace it?",
		        		MessageDialog.WARNING,
		        		new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
		        		1); 
					if (overwriteDialog.open() != Window.OK) {
						return null;
					}
				}
				createArchive(archive, localFile);
			}
		}

		return null;
	}
	
	private Shell getShell() {
		if (Display.getCurrent() != null) {
			return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
		}
		return null;
	}
	
	private static Archive createArchive(Archive archive, File file) {
		IJavaProject javaProject = archive.getJavaProject();
		String className = archive.getLocation().getFullyQualifiedName();
		//String className = type.getFullyQualifiedName();
		String methodName = archive.getLocation().getMethodName();
		//String methodName = deploymentMethod.getName();
		ClassLoader loader = ArquillianCoreActivator.getDefault().getClassLoader(javaProject);
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> clazz = Class.forName(className, true, loader);
			Object object = clazz.newInstance();
			Method method = clazz.getMethod(methodName, new Class[0]);
			
			Object archiveObject = method.invoke(object, new Object[0]);
			Class<?> archiveClass = archiveObject.getClass();
			
			//archive.as(ZipExporter.class).exportTo(
			//	    new File("/home/alr/Desktop/myPackage.jar"), true);
			Class<?> exporterClass = Class.forName("org.jboss.shrinkwrap.api.exporter.ZipExporter", true, loader); //$NON-NLS-1$
			Method asMethod = archiveClass.getMethod("as", new Class[] { Class.class }); //$NON-NLS-1$
			Object asObject = asMethod.invoke(archiveObject, new Object[] {exporterClass});
			Class<?> asClass = asObject.getClass();
			Method exportToMethod = asClass.getMethod("exportTo", new Class[] {File.class, boolean.class }); //$NON-NLS-1$
			exportToMethod.invoke(asObject, new Object[] {file, Boolean.TRUE});
		} catch (OutOfMemoryError e) {
			throw new OutOfMemoryError(e.getLocalizedMessage());
		} catch (InternalError e) {
			throw new InternalError(e.getLocalizedMessage());
		} catch (StackOverflowError e) {
			throw new StackOverflowError(e.getLocalizedMessage());
		} catch (UnknownError e) {
			throw new UnknownError(e.getLocalizedMessage());
		} catch (Throwable e) {
			String message = getText(e) + "(project=" + javaProject.getProject().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			Throwable cause = e.getCause();
			int i = 0;
			while (cause != null && i++ < 5) {
				message = getText(cause) + "(project=" + javaProject.getProject().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				cause = cause.getCause();
			}
			ArquillianCoreActivator.logWarning(message);
			if (Platform.inDebugMode()) {
				ArquillianCoreActivator.log(e);
			}

		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
		return null;
	}
	
	private static String getText(Throwable e) {
		String text;
		if (e.getLocalizedMessage() == null || e.getLocalizedMessage().isEmpty()) {
			text = e.getClass().getName() + ": "; //$NON-NLS-1$
		} else {
			text = e.getLocalizedMessage();
		}
		return text;
	}
}
