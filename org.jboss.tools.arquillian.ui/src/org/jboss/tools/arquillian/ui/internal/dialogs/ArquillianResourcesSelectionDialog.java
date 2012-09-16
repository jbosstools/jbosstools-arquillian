package org.jboss.tools.arquillian.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

public class ArquillianResourcesSelectionDialog extends
		FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.jboss.tools.arquillian.ui.ArquillianResourcesSelectionDialog"; //$NON-NLS-1$
	
	private IJavaProject javaProject;
	
	private List<IPath> allResources;

	private List<IPath> addedResources;
	
	public ArquillianResourcesSelectionDialog(Shell shell, IJavaProject javaProject, List<IPath> addedResources) {
		super(shell, true);
		this.javaProject = javaProject;
		setTitle("Resources selection");
		setMessage("Select resources:");
		setListLabelProvider(new ResourceLabelProvider());
		this.addedResources = addedResources;
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
		return new ResourcesSearchItemsFilter();
	}

	@Override
	protected Comparator getItemsComparator() {
		return new ResourcesSearchComparator();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {

		getResources();
		for (IPath resource:allResources) {
			contentProvider.add(resource, itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	@Override
	public String getElementName(Object item) {
		if (item instanceof IPath) {
			return ((IPath)item).toString();
		}
		return null;
	}

	private void getResources() {
		allResources = new ArrayList<IPath>();
		if (javaProject != null && javaProject.isOpen()) {
			IPath testSourcePath = null;
			try {
				IProject project = javaProject.getProject();
				if (project.hasNature(IMavenConstants.NATURE_ID)) {
					IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
					MavenProject mavenProject = MavenPlugin.getMaven().readProject(
							pomFile.getLocation().toFile(), new NullProgressMonitor());
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

							Object[] resources = root.getNonJavaResources();
							for (Object resource:resources) {
								addResource(allResources, resource, root.getPath());
							}
						}
					}
				}
			} catch (Exception e1) {
				ArquillianUIActivator.log(e1);
			}
		}
	}
	
	private void addResource(List<IPath> allResources, Object resource, IPath root) throws CoreException {
		if (resource instanceof IFile) {
			IPath resourcePath = ((IFile) resource).getFullPath().makeRelativeTo(root);
			if (!addedResources.contains(resourcePath)) {
				allResources.add(resourcePath);
			}
		} else if (resource instanceof IFolder) {
			IFolder folder = (IFolder) resource;
			IResource[] children = folder.members();
			for (IResource child:children) {
				addResource(allResources, child, root);
			}
		}
	}
	
	private String convert(String text) {
//		if (text.startsWith("/")) {
//			text = text.substring(1);
//		}
//		text = text.replace("/", ".");
		return text;
	}

	private class ResourcesSearchItemsFilter extends ItemsFilter {

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean matchItem(Object item) {
			String text = null;
			if (item instanceof IPath) {
				text = ((IPath)item).toString();
				text = convert(text);
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
	
	private class ResourcesSearchComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			String  s1 = getName(o1);
			String s2 = getName(o2);
			if (s1 == null && s2 == null) {
				return 0;
			}
			if (s1 == null && s2 != null) {
				return -1;
			}

			return s1.compareTo(s2);
		}

		private String getName(Object element) {
			if (element instanceof IPath) {
				return ((IPath)element).toString();
			}
			return null;
		}

	}
	
	private class ResourceLabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Image getImage(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IPath) {
				String text = ((IPath)element).toString();
				return convert(text);
			}
			return null;
		}

		
	}
}
