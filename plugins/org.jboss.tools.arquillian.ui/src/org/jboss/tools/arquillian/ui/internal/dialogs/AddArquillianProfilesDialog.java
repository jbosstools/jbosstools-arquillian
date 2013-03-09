package org.jboss.tools.arquillian.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.maven.profiles.core.MavenProfilesCoreActivator;
import org.jboss.tools.maven.profiles.core.profiles.ProfileStatus;

/**
 * @author snjeza
 * 
 */
public class AddArquillianProfilesDialog extends TitleAreaDialog {
	
	private CheckboxTableViewer viewer;
	private IProject project;
	private Image dialogImage;
	private List<Container> containers;
	private List<String> profiles;
	
	public AddArquillianProfilesDialog(Shell parentShell, IProject project) {
		super(parentShell);
		Assert.isNotNull(project);
		setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER
				| SWT.RESIZE | getDefaultOrientation());
		this.project = project;
	}

	
	private List<String> getProfiles() {
		List<String> profiles = new ArrayList<String>();
		IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
		List<ProfileStatus> profileStatuses = null;
		try {
			profileStatuses = MavenProfilesCoreActivator.getDefault().getProfileManager().getProfilesStatuses(facade, new NullProgressMonitor());
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		if (profileStatuses != null) {
			for(ProfileStatus profileStatus:profileStatuses) {
				profiles.add(profileStatus.getId());
			}
		}
		return profiles;
	}


	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Add Arquillian Profiles");
		setTitle("Add Arquillian Profiles");
		setMessage("Select profiles to add to the '" + project.getName() + "' project.");
		setTitleImage(getDialogImage());
		
		Composite area = (Composite) super.createDialogArea(parent);
		Composite contents = new Composite(area, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		contents.setLayoutData(gd);
		contents.setLayout(new GridLayout(1, false));
		applyDialogFont(contents);
		initializeDialogUnits(area);

		Group profilesGroup = new Group(contents, SWT.NONE);
		profilesGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        
        profilesGroup.setLayoutData(gd);
        profilesGroup.setText("Profiles");
		
        containers = ContainerParser.getContainers();
		viewer = ArquillianUIUtil.createProfilesViewer(profilesGroup, containers, 400);
		profiles = getProfiles();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				initializeViewer();
				validate();
			}
		});
		
		initializeViewer();
		
		return area;
	}


	private void initializeViewer() {
		TableItem[] items = viewer.getTable().getItems();
		for (TableItem item:items) {
			Object data = item.getData();
			if (data instanceof Container) {
				Container container = (Container) data;
				if (profiles.contains(container.getId())) {
					item.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					viewer.setChecked(container, false);
				}
			}
		}
	}

	private Image getDialogImage() {
		if (dialogImage == null) {
			dialogImage = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/arquillian_icon64.png").createImage();
		}
		return dialogImage;
	}

	@Override
	public boolean close() {
		if (dialogImage != null) {
			dialogImage.dispose();
		}
		return super.close();
	}
	
	@Override
	protected void okPressed() {
		IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
		Object[] checkedElements = viewer.getCheckedElements();
		List<Container> checkedContainers = new ArrayList<Container>();
		for (Object element:checkedElements) {
			if (element instanceof Container) {
				Container container = (Container) element;
				if (!profiles.contains(container.getId())) {
					checkedContainers.add(container);
				}
			}
			
		}
		if (pomFile != null && checkedContainers.size() > 0) {
			try {
				ArquillianUtility.addProfiles(pomFile, checkedContainers);
			} catch (CoreException e) {
				ArquillianUIActivator.log(e);
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.SELECT_ALL_ID, "Select All", false);
		createButton(parent, IDialogConstants.DESELECT_ALL_ID, "Deselect All", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		validate();
	}

	protected void validate() {
		Object[] checkedElements = viewer.getCheckedElements();
		getButton(IDialogConstants.OK_ID).setEnabled(checkedElements.length != 0);
		getButton(IDialogConstants.SELECT_ALL_ID).setEnabled(checkedElements.length < containers.size());
		getButton(IDialogConstants.DESELECT_ALL_ID).setEnabled(checkedElements.length > 0);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		if (IDialogConstants.SELECT_ALL_ID == buttonId) {
			selectAllPressed();
		} else if (IDialogConstants.DESELECT_ALL_ID == buttonId) {
			deselectAllPressed();
		}
		if (!viewer.getTable().isDisposed()) {
			validate();
		}
	}

	private void deselectAllPressed() {
		setChecked(false);
	}

	private void setChecked(boolean checked) {
		for (Container container:containers) {
			viewer.setChecked(container, checked);
			
		}
		viewer.refresh();
	}

	private void selectAllPressed() {
		setChecked(true);
	}

}
