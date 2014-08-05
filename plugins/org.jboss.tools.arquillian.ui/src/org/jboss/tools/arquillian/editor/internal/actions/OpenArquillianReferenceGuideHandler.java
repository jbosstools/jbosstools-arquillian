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
package org.jboss.tools.arquillian.editor.internal.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.sapphire.ui.Presentation;
import org.eclipse.sapphire.ui.SapphireActionHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 * 
 */
public final class OpenArquillianReferenceGuideHandler extends SapphireActionHandler {
	@Override
	protected Object run(final Presentation context) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				URL url = null;
				try {
					url = new URL("https://docs.jboss.org/author/display/ARQ/Reference+Guide");
				} catch (MalformedURLException e) {
					ArquillianUIActivator.log(e);
					return;
				}
				openUrl(url, getShell(), false);
			}

		});

		return null;
	}

	private static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 0) {
				return windows[0].getShell();
			}
		} else {
			return window.getShell();
		}
		return null;
	}
	
	private static void openUrl(URL url, Shell shell, boolean asExternal) {
		try {
			

			if (WebBrowserPreference.getBrowserChoice() == WebBrowserPreference.EXTERNAL
					|| asExternal) {
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench()
						.getBrowserSupport();
				support.getExternalBrowser().openURL(url);
			} else {
				IWebBrowser browser = null;
				int flags;
				if (WorkbenchBrowserSupport.getInstance()
						.isInternalWebBrowserAvailable()) {
					flags = IWorkbenchBrowserSupport.AS_EDITOR
							| IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				} else {
					flags = IWorkbenchBrowserSupport.AS_EXTERNAL
							| IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				}

				String generatedId = ArquillianUIActivator.PLUGIN_ID
						+ System.currentTimeMillis();
				browser = WorkbenchBrowserSupport.getInstance().createBrowser(
						flags, generatedId, null, null);
				browser.openURL(url);
			}
		} catch (PartInitException e) {
			Status status = new Status(IStatus.ERROR,
					ArquillianUIActivator.PLUGIN_ID,
					"Browser initialization failed");
			ArquillianUIActivator.getDefault().getLog().log(status);
			MessageDialog
					.openError(shell, "Open Location", status.getMessage());
		}
	}


}
