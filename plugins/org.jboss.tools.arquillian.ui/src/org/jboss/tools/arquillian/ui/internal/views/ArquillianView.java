/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.views;

import java.util.HashSet;
import java.util.Set;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.archives.Entry;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianView extends CommonNavigator {

	public static final String ID = "org.jboss.tools.arquillian.ui.views.arquillianView"; //$NON-NLS-1$
	private static final String JAVA = ".java"; //$NON-NLS-1$
	private static final String CLASS = ".class"; //$NON-NLS-1$
	private static final String WEB_INF_CLASSES = "WEB-INF/classes/"; //$NON-NLS-1$
	private static final String META_INF = "META-INF/"; //$NON-NLS-1$
	private static final String WEB_INF = "WEB-INF/"; //$NON-NLS-1$
	
	private IResourceChangeListener resourceChangeListener;
	
	private ISelectionListener selectionListener;

	public ArquillianView() {
		super();
	}

	@Override
	public void dispose() {
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(
					resourceChangeListener);
			resourceChangeListener = null;
		}
		if (selectionListener != null) {
			getSite().getPage().removeSelectionListener(selectionListener);
			selectionListener = null;
		}
		super.dispose();
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		
		CommonViewer commonViewer = super.createCommonViewer(aParent);
		selectionListener = new ISelectionListener() {
			private IProject oldSelected;
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				IProject selected = ArquillianUIUtil.getSelectedProject(selection);
				if (part == ArquillianView.this) {
					oldSelected = selected;
					return;
				}
				if (selected == null) {
					oldSelected = null;
					return;
				}
				if (selected.equals(oldSelected)) {
					return;
				}
				if (getCommonViewer() != null
						&& getCommonViewer().getControl() != null
						&& !getCommonViewer().getControl().isDisposed()) {
					getCommonViewer().refresh();
					oldSelected = selected;
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);
		resourceChangeListener = new IResourceChangeListener() {

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				if (!PlatformUI.isWorkbenchRunning()) {
					return;
				}
				final Set<IProject> toRefresh = new HashSet<IProject>();
				final Set<IProject> toRemove = new HashSet<IProject>();
				final Set<IProject> toAdd = new HashSet<IProject>();
				
				if (event.getType() == IResourceChangeEvent.PRE_DELETE ||
						event.getType() == IResourceChangeEvent.PRE_CLOSE) {
					IResource project = event.getResource();
					if (project != null && project instanceof IProject) {
						toRemove.add((IProject) project);
					}
				} else if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
					handlePostChange(event, toAdd);
				} else {
					// POST_BUILD
					handlePostBuild(event, toRefresh);
				}

				if (toRefresh.size() <= 0 && toRemove.size() <= 0 && toAdd.size() <= 0) {
					return;
				}
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						if (getCommonViewer() != null && getCommonViewer().getControl() != null 
								&& !getCommonViewer().getControl().isDisposed()) {
							for (IProject project:toRefresh) {
								getCommonViewer().refresh(project);
							}
							for (IProject project:toRemove) {
								getCommonViewer().remove(project);
							}
							if (toAdd.size() > 0) {
								getCommonViewer().refresh();
							}
						}
					}
				});
			}

			private void handlePostChange(IResourceChangeEvent event,
					final Set<IProject> projects) {
				IResourceDelta delta = event.getDelta();
				if (delta != null) {
					try {
						delta.accept(new IResourceDeltaVisitor() {
							public boolean visit(IResourceDelta delta)
									throws CoreException {
								if (delta == null) {
									return false;
								}
								IResource resource = delta.getResource();
								if (resource instanceof IWorkspaceRoot) {
									return true;
								}
								if (resource instanceof IProject && delta.getKind() == IResourceDelta.ADDED) {
									if (((IProject) resource).isOpen()) {
										projects.add(resource.getProject());
									}
								}
								return false;
							}
						});
					} catch (CoreException e) {
						ArquillianCoreActivator.log(e);
					}
				}
			}

			public void handlePostBuild(IResourceChangeEvent event,
					final Set<IProject> projects) {
				IResourceDelta delta = event.getDelta();
				if (delta != null) {
					try {
						delta.accept(new IResourceDeltaVisitor() {
							public boolean visit(IResourceDelta delta)
									throws CoreException {
								if (delta == null) {
									return false;
								}
								IResource resource = delta.getResource();
								if (resource instanceof IWorkspaceRoot) {
									return true;
								}
								if (resource instanceof IProject) {
									if (((IProject) resource).isOpen()) {
										projects.add((IProject) resource);
									}
								}
								return false;
							}
						});
					} catch (CoreException e) {
						ArquillianCoreActivator.log(e);
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_BUILD|IResourceChangeEvent.PRE_CLOSE|IResourceChangeEvent.PRE_DELETE| IResourceChangeEvent.POST_CHANGE);
		
		return commonViewer;
	}

	@Override
	protected void handleDoubleClick(DoubleClickEvent anEvent) {
		if (!open(anEvent)) {
			super.handleDoubleClick(anEvent);
		}
	}
	
	private void openFile(IResource resource) throws PartInitException {
		IFile file = (IFile) resource;
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IDE.openEditor(page, file);
	}

	private boolean open(DoubleClickEvent event) {
		if (event == null || ! (event.getSelection() instanceof StructuredSelection)) {
			return false;
		}
		StructuredSelection structured = (StructuredSelection) event.getSelection();
		Object object = structured.getFirstElement();
		if (object instanceof Entry) {
			Entry element = (Entry) object;
			if (element.getFullyQualifiedName() == null) {
				return false;
			}
			IJavaProject project = element.getJavaProject();
			try {
				IType type = project.findType(element.getFullyQualifiedName());
				IResource resource  = type.getUnderlyingResource();
				if (resource != null) {
					openFile(resource);
				}
			} catch (PartInitException e) {
				ArquillianUIActivator.log(e);
			} catch (JavaModelException e) {
				ArquillianUIActivator.log(e);
			}
//			try {
//				IClasspathEntry[] entries = project.getRawClasspath();
//				for (IClasspathEntry entry:entries) {
//					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
//						IPath entryPath = entry.getPath();
//						IPath path = entryPath.append(name);
//						IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
//						if (resource instanceof IFile && resource.exists()) {
//							openFile(resource);
//							return true;
//						} else {
//							String n = name.replace(WEB_INF,""); //$NON-NLS-1$
//							path = entryPath.append(n);
//							resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
//							if (resource instanceof IFile && resource.exists()) {
//								openFile(resource);
//								return true;
//							} else {
//								n = name.replace(META_INF,""); //$NON-NLS-1$
//								path = entryPath.append(n);
//								resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
//								if (resource instanceof IFile && resource.exists()) {
//									openFile(resource);
//									return true;
//								}
//							}
//						}
//					}
//				}
//			} catch (JavaModelException e) {
//				ArquillianUIActivator.log(e);
//			} catch (PartInitException e) {
//				ArquillianUIActivator.log(e);
//			}
		}
		return false;
	}

}
