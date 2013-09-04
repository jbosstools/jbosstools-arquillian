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
package org.jboss.tools.arquillian.ui.internal.editors.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.editors.ArquillianEditor;
import org.jboss.tools.arquillian.ui.internal.editors.model.AbstractTreeContentProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.PropertyLabelProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.Protocol;
import org.jboss.tools.arquillian.ui.internal.preferences.ContainerGroupPreferencePage;
import org.jboss.tools.arquillian.ui.internal.preferences.model.PreferenceArquillian;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

/**
 * Wizard page for creating or editing container element. 
 *
 */
public class ContainerWizardPage extends WizardPage {
	
	private static final String[] MODES = new String[]{"suite", "class", "manual", "custom"};
	
	private Arquillian arquillian;
	private Container container;
	private Group group;
	private boolean preference;
	private boolean replication;
	
	private Combo containers;
	private Text qualifier;
	private Label qualifierSuffix;
	private Combo mode;
	private TreeViewer protocols;
	private TableViewer configurations;
	
	private String selectedContainer;
	
	protected ContainerWizardPage(Arquillian arquillian, Container container, Group group) {
		super("Container Wizard Page");
		setTitle("Container");
		if(container == null || replication) {
			setDescription("Create a new container");
		} else {
			setDescription("Edit a container");
		}
		this.arquillian = arquillian;
		this.container = container;
		this.group = group;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new TableWrapLayout());

		Composite innerComposite = new Composite(composite, SWT.NONE);
		innerComposite.setLayout(LayoutUtil.createTableWrapLayout(2));
		new Label(innerComposite, SWT.NONE).setText("container:");
		
		containers = new Combo(innerComposite, SWT.READ_ONLY);
		containers.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		for(org.jboss.forge.arquillian.container.Container container : ContainerParser.getContainers()) {
			containers.add(container.getId());
		}
		
		// when it was opened from preference, link does not show.
		if(!preference) {
			Link loadPreferencelink = new Link(innerComposite, SWT.NONE);
			loadPreferencelink.setText("<A>load container from preference</A>");
			loadPreferencelink.setLayoutData(LayoutUtil.createTableWrapData(2, -1, TableWrapData.RIGHT));
			loadPreferencelink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferenceManager manager = new PreferenceManager();
					ContainerGroupPreferencePage page = new ContainerGroupPreferencePage(PreferenceArquillian.TYPE_CONTAINER);
					page.setTitle("JBoss Test Plugin");
					page.init(ArquillianUIActivator.getDefault().getWorkbench());
					IPreferenceNode node = new PreferenceNode("1", page);
					manager.addToRoot(node);
					PreferenceDialog dialog = new PreferenceDialog(Display.getDefault().getActiveShell(), manager);
					dialog.create();
					if(dialog.open() == IDialogConstants.OK_ID) {
						Container container = page.getSelectedContainer();
						if(container == null) {
							return;
						}
						containers.setText(container.getType());
						qualifier.setText(container.getQualifier());
						mode.setText(container.getMode());
						if(container.getProtocols() != null) {
							protocols.setInput(container.getProtocols());
						}
						if(container.getConfigurations() != null) {
							configurations.setInput(container.getConfigurations());
						}
					}
				}
			});
		}
		
		new Label(innerComposite, SWT.NONE).setText("qualifier:");
		Composite qualifierComposite = new Composite(innerComposite, SWT.NONE);
		qualifierComposite.setLayout(LayoutUtil.createGridLayout(2));
		qualifier = new Text(qualifierComposite, SWT.BORDER | SWT.SINGLE);
		qualifier.setLayoutData(new GridData(200, -1));
		qualifierSuffix = new Label(qualifierComposite, SWT.NONE);
		qualifierSuffix.setLayoutData(new GridData(400, -1));
		
		new Label(innerComposite, SWT.NONE).setText("mode:");
		mode = new Combo(innerComposite, SWT.READ_ONLY);
		for(String m : MODES) {
			mode.add(m);
		}
		mode.setText(MODES[0]);
		
		new Label(composite, SWT.NONE).setText("protocol:");
		Composite protocolComposite = new Composite(composite, SWT.NONE);
		protocolComposite.setLayout(LayoutUtil.createTableWrapLayout(2));
		protocols = new TreeViewer(new Tree(protocolComposite, SWT.BORDER));
//		protocols.getTree().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		protocols.getTree().setLayoutData(LayoutUtil.createTableWrapData(85));
		protocols.setContentProvider(new ProtocolContentProvider());
		protocols.setLabelProvider(new ProtocolLabelProvider());
		protocols.setInput(new ArrayList<Protocol>());
		
		Composite protocolButtonComposite = new Composite(protocolComposite, SWT.NONE);
		protocolButtonComposite.setLayout(new FillLayout(SWT.VERTICAL));
		Button addProtocolBtn = new Button(LayoutUtil.createButtonComposite(protocolButtonComposite), SWT.PUSH);
		addProtocolBtn.setLayoutData(LayoutUtil.createButtonLayoutData());
		addProtocolBtn.setText("Add");
		final Button editProtocolBtn = new Button(LayoutUtil.createButtonComposite(protocolButtonComposite), SWT.PUSH);
		editProtocolBtn.setLayoutData(LayoutUtil.createButtonLayoutData());
		editProtocolBtn.setText("Edit");
		editProtocolBtn.setEnabled(false);
		final Button removeProtocolBtn = new Button(LayoutUtil.createButtonComposite(protocolButtonComposite), SWT.PUSH);
		removeProtocolBtn.setLayoutData(LayoutUtil.createButtonLayoutData());
		removeProtocolBtn.setText("Remove");
		removeProtocolBtn.setEnabled(false);
		
		new Label(composite, SWT.NONE).setText("configuration:");
		configurations = new TableViewer(new Table(composite, SWT.BORDER | SWT.FULL_SELECTION));
		configurations.getTable().setLayoutData(LayoutUtil.createTableWrapData(100));
		configurations.getTable().setLinesVisible(true);
		configurations.getTable().setHeaderVisible(true);
		configurations.setContentProvider(new ArrayContentProvider());
		ArquillianEditor.createPropertyColumns(configurations);
		configurations.setLabelProvider(new PropertyLabelProvider());

		//-------- attach event
		containers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do nothing if same type is selected.
				if(containers.getText().equals(selectedContainer)) {
					return;
				}
				selectedContainer = containers.getText();
				qualifierSuffix.setText("-" + selectedContainer);
				
				configurations.getTable().removeAll();
				List<Property> properties = new ArrayList<Property>();
				for(org.jboss.forge.arquillian.container.Container forgeContainer : ContainerParser.getContainers()) {
					if(forgeContainer.getId().equals(selectedContainer)) {
						if(forgeContainer.getConfigurations() != null) {
							for(Configuration configuration : forgeContainer.getConfigurations()) {
								properties.add(new Property(configuration.getName(), "", configuration.getType()));
							}
						}
						break;
					}
				}
				configurations.setInput(properties);
			}
		});
		containers.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doValidate();
			}
		});
		
		qualifier.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				doValidate();
			}
		});
		
		addProtocolBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianUIUtil.openWizard(new ProtocolWizard(protocols));
			}
		});
		editProtocolBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Protocol protocol = (Protocol) ArquillianUIUtil.getSelection(protocols);
				ArquillianUIUtil.openWizard(new ProtocolWizard(protocols, protocol));
			}
		});
		removeProtocolBtn.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(ArquillianUIUtil.openConfirm(
						"Remove Protocol",
						"Are you sure you want to remove the protocol?")) {
					Protocol protocol = (Protocol) ArquillianUIUtil.getSelection(protocols);
					((List<Protocol>) protocols.getInput()).remove(protocol);
					protocols.refresh();
				}
			}
		});
		
		protocols.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				editProtocolBtn.setEnabled(true);
				removeProtocolBtn.setEnabled(true);
			}
		});
		
		// restore initial value
		if(container != null) {
			containers.setText(container.getType());
			selectedContainer = container.getType();
			if(!replication) {
				qualifier.setText(container.getQualifierPrefix());
			}
			mode.setText(container.getMode());
			// protocol
			List<Protocol> restoreProtocols = new ArrayList<Protocol>();
			for(Protocol protocol : container.getProtocols()) {
				restoreProtocols.add(new Protocol(protocol));
			}
			protocols.setInput(restoreProtocols);
			// configuration
			for(org.jboss.forge.arquillian.container.Container forgeContainer : ContainerParser.getContainers()) {
				if(forgeContainer.getId().equals(container.getType())) {
					qualifierSuffix.setText("-" + container.getType());
					configurations.setInput(
							ArquillianEditor.restoreProperties(
									forgeContainer.getConfigurations(),
									container.getConfigurations()));
					break;
				}
			}
		}

		doValidate();
		setControl(composite);
	}
	
	private void doValidate() {
		if(StringUtils.isEmpty(containers.getText())) {
			setErrorMessage("container is empty.");
			setPageComplete(false);
			return;
		}
		if(StringUtils.isEmpty(qualifier.getText())) {
			setErrorMessage("qualifier is empty.");
			setPageComplete(false);
			return;
		}
		ContainerGroupModel model = null;
		if(group == null) {
			model = arquillian.getContainerOrGroup(qualifier.getText() + "-" + containers.getText());
		} else {
			model = group.getContainer(qualifier.getText() + "-" + containers.getText());
		}
		if(model != null && (replication || model != container)) {
			setErrorMessage("qualifier is already used.");
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	public void setPreference(boolean preference) {
		this.preference = preference;
	}
	
	public void setReplication(boolean replication) {
		this.replication = replication;
	}
	
	public String getQualifier() {
		return qualifier.getText();
	}

	public String getContainerType() {
		return containers.getText();
	}
	
	public String getMode() {
		return mode.getText();
	}
	
	@SuppressWarnings("unchecked")
	public List<Property> getConfigurations() {
		return (List<Property>) configurations.getInput();
	}
	
	@SuppressWarnings("unchecked")
	public List<Protocol> getProtocols() {
		return (List<Protocol>) protocols.getInput();
	}

	private static class ProtocolContentProvider extends AbstractTreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof List) {
				@SuppressWarnings("unchecked")
				List<Protocol> protocols = (List<Protocol>) parentElement;
				return protocols.toArray(new Protocol[protocols.size()]);
			}
			return new Object[0];
		}
	}
	

	private static final ImageDescriptor IMG_DESC_CONTAINER = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/protocol.gif"); //$NON-NLS-1$
	
	private static class ProtocolLabelProvider extends ColumnLabelProvider {

		@Override
		public String getText(Object element) {
			if(element instanceof Protocol) {
				return ((Protocol) element).getType();
			}
			return super.getText(element);
		}
		
		@Override
		public Image getImage(Object element) {
			return ArquillianUIActivator.getImage(IMG_DESC_CONTAINER);
		}
	}
}
