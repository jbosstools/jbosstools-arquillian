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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.core.internal.IMavenConstants;
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
        Collections.sort(containers, new Comparator<Container>() {

			@Override
			public int compare(Container c1, Container c2) {
				if (c1 == null && c2 == null) {
					return 0;
				}
				if (c1 == null) {
					return -1;
				}
				if (c2 == null) {
					return 1;
				}
				String n1 = c1.getId();
				String n2 = c2.getId();
				if (n1 == null && n2 == null) {
					return 0;
				}
				if (n1 == null) {
					return -1;
				}
				if (n2 == null) {
					return 1;
				}
				return n1.compareTo(n2);
			}
		});
		viewer = ArquillianUIUtil.createProfilesViewer(profilesGroup, containers, 400);
		profiles = ArquillianUtility.getProfiles(project);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				initializeViewer();
				validate();
			}
		});
		setChecked(false);
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
					viewer.setChecked(container, true);
				}
			}
		}
	}

	private Image getDialogImage() {
		if (dialogImage == null) {
			dialogImage = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/arquillian_icon64.png").createImage(); //$NON-NLS-1$
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
		initializeViewer();
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
