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
package org.jboss.tools.arquillian.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.wizards.TypeLabelProvider;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianTypesSelectionDialog extends
		FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.jboss.tools.arquillian.ui.ArquillianTypesSelectionDialog"; //$NON-NLS-1$
	
	private IJavaProject javaProject;
	
	private List<IType> allTypes;

	private List<IType> addedTypes;
	
	public ArquillianTypesSelectionDialog(Shell shell, IJavaProject javaProject, List<IType> addedTypes) {
		super(shell, true);
		this.javaProject = javaProject;
		setTitle("Classes selection");
		setMessage("Select classes:");
		setListLabelProvider(new TypeLabelProvider());
		this.addedTypes = addedTypes;
	}
	
	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = ArquillianUIActivator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = ArquillianUIActivator.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, ArquillianUIActivator.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
	}

	@Override
	protected ItemsFilter createFilter() {
		return new TypesSearchItemsFilter();
	}

	@Override
	protected Comparator getItemsComparator() {
		return new TypesSearchComparator();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {

		getClasses();
		for (IType type:allTypes) {
			contentProvider.add(type, itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	@Override
	public String getElementName(Object item) {
		if (item instanceof IType) {
			return ((IType)item).getFullyQualifiedName();
		}
		return null;
	}

	private void getClasses() {
		allTypes = new ArrayList<IType>();
		if (javaProject != null && javaProject.isOpen()) {
			IPath testSourcePath = null;
			try {
				IProject project = javaProject.getProject();
				if (project.hasNature(IMavenConstants.NATURE_ID)) {
					IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
				    MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
				    Build build = mavenProject.getBuild();
					String testSourceDirectory = build.getTestSourceDirectory();
					testSourcePath = Path.fromOSString(testSourceDirectory);
					IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getRawLocation();
					testSourcePath = testSourcePath.makeRelativeTo(workspacePath).makeAbsolute();
					
				}
				IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
				for (IClasspathEntry entry : rawClasspath) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPackageFragmentRoot[] roots = javaProject
								.findPackageFragmentRoots(entry);
						if (roots == null) {
							continue;
						}
						for (IPackageFragmentRoot root : roots) {
							IPath path = root.getPath();
							String projectLocation = project.getLocation().toOSString();
							IPath projectPath = Path.fromOSString(projectLocation);
							IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getRawLocation();
							projectPath = projectPath.makeRelativeTo(workspacePath).makeAbsolute();
							projectPath = projectPath.removeLastSegments(1);
							path = projectPath.append(path);
							if (path != null && path.equals(testSourcePath)) {
								continue;
							}

							IJavaElement[] children = root.getChildren();
							for (IJavaElement child : children) {
								if (child instanceof IPackageFragment) {
									IPackageFragment packageFragment = (IPackageFragment) child;
									IJavaElement[] elements = packageFragment
											.getChildren();
									for (IJavaElement element : elements) {
										if (element instanceof ICompilationUnit) {
											ICompilationUnit cu = (ICompilationUnit) element;
											IType[] types = cu.getTypes();
											for (IType type : types) {
												if (!addedTypes.contains(type)) {
													allTypes.add(type);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e1) {
				ArquillianUIActivator.log(e1);
			}
		}
	}
	
	private class TypesSearchItemsFilter extends ItemsFilter {

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean matchItem(Object item) {
			String text = null;
			if (item instanceof IType) {
				text = ((IType)item).getFullyQualifiedName();
			}

			return (matches(text));
		}

		protected boolean matches(String text) {
			String pattern = patternMatcher.getPattern();
			if (pattern.indexOf("*") != 0 & pattern.indexOf("?") != 0 & pattern.indexOf(".") != 0) {//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pattern = "*" + pattern; //$NON-NLS-1$
				patternMatcher.setPattern(pattern);
			}
			return patternMatcher.matches(text);
		}
	}
	
	private class TypesSearchComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			String  s1 = getId(o1);
			String s2 = getId(o2);
			if (s1 == null && s2 == null) {
				return 0;
			}
			if (s1 == null && s2 != null) {
				return -1;
			}

			return s1.compareTo(s2);
		}

		private String getId(Object element) {
			if (element instanceof IType) {
				return ((IType)element).getFullyQualifiedName();
			}
			return null;
		}

	}
}
