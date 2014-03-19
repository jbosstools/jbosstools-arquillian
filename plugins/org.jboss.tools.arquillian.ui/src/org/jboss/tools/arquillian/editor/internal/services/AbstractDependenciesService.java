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
package org.jboss.tools.arquillian.editor.internal.services;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.sapphire.services.ValidationService;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public abstract class AbstractDependenciesService extends ValidationService {

	private boolean disposed = false;
	protected class RefreshJob extends Job {

		public RefreshJob() {
			super("Refreshing Arquillian editor...");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD,
						monitor);
				if (!disposed) {
					refresh();
				}
			} catch (Exception e) {
				ArquillianUIActivator.logWarning(e.getLocalizedMessage());
			}
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
		
	}

	private IResourceChangeListener resourceChangeListener;

	@Override
	protected void initValidationService() {
		resourceChangeListener = new IResourceChangeListener() {
			
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
					return;
				}
				IResourceDelta delta = event.getDelta();
				if (delta == null) {
					return;
				}
				try {
					delta.accept(new IResourceDeltaVisitor() {
						
						@Override
						public boolean visit(IResourceDelta delta) throws CoreException {
							IResource resource = delta.getResource();
							if (resource instanceof IWorkspaceRoot || resource instanceof IProject) {
					    		return true;
					    	}
							if (resource instanceof IFile) {
								if (IMavenConstants.POM_FILE_NAME.equals(resource.getName())) {
									Job refreshJob = new RefreshJob();
									refreshJob.setSystem(false);
									refreshJob.setPriority(Job.DECORATE);
									refreshJob.schedule(200);
								}
							}
					        return false;
						}
					});
				} catch (CoreException e) {
					ArquillianUIActivator.log(e);
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void dispose() {
		disposed = true;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		super.dispose();
	}
}
