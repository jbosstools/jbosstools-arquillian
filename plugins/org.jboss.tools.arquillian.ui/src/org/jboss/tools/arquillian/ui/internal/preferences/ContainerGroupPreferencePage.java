/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Nippon Telegraph and Telephone Corporation - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditorLabelProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupContentProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ContainerWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.GroupWizard;
import org.jboss.tools.arquillian.ui.internal.preferences.model.PreferenceArquillian;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

/**
 * Preference page for registering containers and groups.
 *
 */
public class ContainerGroupPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private TreeViewer containers;

	private Arquillian arquillian;
	private Container container;
	private Group group;
	
	private Object previousSelection;

	private int type;
	
	public ContainerGroupPreferencePage() {
		this(PreferenceArquillian.TYPE_ALL);
	}
	
	public ContainerGroupPreferencePage(int type) {
		this.type = type;
	}

	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
		setPreferenceStore(ArquillianUIActivator.getDefault().getPreferenceStore());
		setDescription("Add, remove, or edit container/group.");
		arquillian = new PreferenceArquillian(getPreferenceStore(), type);
		arquillian.init();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		containers = new TreeViewer(new Tree(composite, SWT.BORDER));
		containers.getTree().setLayoutData(LayoutUtil.createTableWrapData(400));
		containers.setContentProvider(new PreferenceContainerGroupContentProvider());
		containers.setLabelProvider(new ArquillianEditorLabelProvider());
		containers.setInput(arquillian);

		Composite containerButtons = new Composite(composite, SWT.NONE);
		containerButtons.setLayout(LayoutUtil.createFillLayout(SWT.VERTICAL, 5));
		Button btnGroupAdd = new Button(LayoutUtil.createButtonComposite(containerButtons), SWT.PUSH);
		btnGroupAdd.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnGroupAdd.setText("Add Group");
		if(type == PreferenceArquillian.TYPE_CONTAINER) {
			btnGroupAdd.setEnabled(false);
		}
		final Button btnContainerAdd = new Button(LayoutUtil.createButtonComposite(containerButtons), SWT.PUSH);
		btnContainerAdd.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnContainerAdd.setText("Add Container");
		if(type == PreferenceArquillian.TYPE_GROUP) {
			btnContainerAdd.setEnabled(false);
		}
		final Button btnCopy = new Button(LayoutUtil.createButtonComposite(containerButtons), SWT.PUSH);
		btnCopy.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnCopy.setText("Copy");
		btnCopy.setEnabled(false);
		final Button btnEdit = new Button(LayoutUtil.createButtonComposite(containerButtons), SWT.PUSH);
		btnEdit.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnEdit.setText("Edit");
		btnEdit.setEnabled(false);
		final Button btnRemove = new Button(LayoutUtil.createButtonComposite(containerButtons), SWT.PUSH);
		btnRemove.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnRemove.setText("Remove");
		btnRemove.setEnabled(false);

		//---------- event handler
		containers.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ArquillianUIUtil.getSelection(containers);
				if(selection != null && selection != previousSelection) {
					previousSelection = selection;
					btnEdit.setEnabled(true);
					btnRemove.setEnabled(true);
					if(selection instanceof Container) {
						btnCopy.setEnabled(true);
						if(type == PreferenceArquillian.TYPE_GROUP) {
							btnContainerAdd.setEnabled(false);
						}
					} else {
						btnCopy.setEnabled(false);
						btnContainerAdd.setEnabled(true);
					}
				} else {
					containers.getTree().deselectAll();
					previousSelection = null;
					if(type == PreferenceArquillian.TYPE_GROUP) {
						btnContainerAdd.setEnabled(false);
					}
					btnEdit.setEnabled(false);
					btnRemove.setEnabled(false);
					btnCopy.setEnabled(false);
				}
			}
		});

		btnContainerAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ContainerWizard wizard = new ContainerWizard(containers);
				wizard.preference();
				ArquillianUIUtil.openWizard(wizard);
			}
		});
		btnGroupAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				GroupWizard wizard = new GroupWizard(containers);
				wizard.preference();
				ArquillianUIUtil.openWizard(wizard);
			}
		});
		btnCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ContainerWizard wizard = new ContainerWizard(containers, (Container) ArquillianUIUtil.getSelection(containers));
				wizard.preference();
				wizard.replication();
				ArquillianUIUtil.openWizard(wizard);
			}
		});
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianModel element = (ArquillianModel) ArquillianUIUtil.getSelection(containers);
				if(element instanceof Container) {
					ContainerWizard wizard = new ContainerWizard(containers, (Container) element);
					wizard.preference();
					ArquillianUIUtil.openWizard(wizard);
				} else if(element instanceof Group) {
					GroupWizard wizard = new GroupWizard(containers, (Group) element);
					wizard.preference();
					ArquillianUIUtil.openWizard(wizard);
				}
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(ArquillianUIUtil.openConfirm(
						"Remove Container/Group",
						"Are you sure you want to remove the container/group?")) {
					arquillian.remove((ArquillianModel) ArquillianUIUtil.getSelection(containers));
					containers.refresh();
				}
			}
		});

		return composite;
	}

	@Override
	public boolean performOk() {
		container = null;
		group = null;
		Object object = ArquillianUIUtil.getSelection(containers);
		if(object instanceof Container) {
			container = (Container) object;
			if(container.isGroupContainer()) {
				group = container.getGroup();
			}
		} else if(object instanceof Group) {
			group = (Group) object;
		}
		return super.performOk();
	}

	public Container getSelectedContainer() {
		return container;
	}

	public Group getSelectedGroup() {
		return group;
	}

	private static class PreferenceContainerGroupContentProvider extends ContainerGroupContentProvider {
		
		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof PreferenceArquillian) {
				PreferenceArquillian arquillian = (PreferenceArquillian) parentElement;
				List<ContainerGroupModel> containerGroups = new ArrayList<ContainerGroupModel>();
				for(ContainerGroupModel model : arquillian.getContainerGroups()) {
					if(model instanceof Container) {
						if(arquillian.getType() == PreferenceArquillian.TYPE_ALL
								|| arquillian.getType() == PreferenceArquillian.TYPE_CONTAINER) {
							containerGroups.add(model);
						}
					} else if(model instanceof Group) {
						if(arquillian.getType() == PreferenceArquillian.TYPE_ALL
								|| arquillian.getType() == PreferenceArquillian.TYPE_GROUP) {
							containerGroups.add(model);
						}
					}
				}
				return containerGroups.toArray(new ContainerGroupModel[containerGroups.size()]);
			}
			return super.getChildren(parentElement);
		}
		
	}
}
