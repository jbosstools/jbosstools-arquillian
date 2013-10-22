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
package org.jboss.tools.arquillian.ui.internal.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.forge.arquillian.container.Configuration;
import org.jboss.tools.arquillian.core.internal.protocol.Protocol;
import org.jboss.tools.arquillian.core.internal.protocol.ProtocolParser;
import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.jboss.tools.arquillian.ui.internal.editors.model.Arquillian;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.ArquillianXmlElement;
import org.jboss.tools.arquillian.ui.internal.editors.model.Container;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupContentProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.ContainerGroupModel;
import org.jboss.tools.arquillian.ui.internal.editors.model.DefaultProtocolInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.EngineInfo;
import org.jboss.tools.arquillian.ui.internal.editors.model.Extension;
import org.jboss.tools.arquillian.ui.internal.editors.model.ExtensionContentProvider;
import org.jboss.tools.arquillian.ui.internal.editors.model.Group;
import org.jboss.tools.arquillian.ui.internal.editors.model.PomElement;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.editors.model.PropertyLabelProvider;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ContainerWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.ExtensionWizard;
import org.jboss.tools.arquillian.ui.internal.editors.wizard.GroupWizard;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.jboss.tools.arquillian.ui.internal.utils.LayoutUtil;

public class ArquillianGeneralEditor extends EditorPart {

	private Arquillian arquillian;

	protected FormToolkit toolkit;
	protected ScrolledForm form;

	private TreeViewer containers;
	private Button btnCopyContainers;
	private Button btnEditContainers;
	private Button btnRemoveContainers;
	private Button btnDefaultContainers;

	private Text pomFragmentText;

	private TreeViewer extensions;
	private Button btnEditExtensions;
	private Button btnRemoveExtensions;

	private TableViewer engineProperties;

	private Combo defaultProtocols;
	private TableViewer defaultProtocolProperties;

	private boolean updating = false;

	private Action pomFragmentLinkAction = new Action(
			"show fragment of selected containers or extensions", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
		@Override
		public void run() {
			refreshPomFragment();
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED);
		}
	};

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	protected void initModel(IDOMModel model) {
		arquillian = new Arquillian(model.getDocument());
		arquillian.init();

		containers.setInput(arquillian);
		containers.expandAll();
		extensions.setInput(arquillian);
		updateDefaultProtocol();
		updateEngine();
		refreshPomFragment();
	}

	protected void updateModel(final IDOMModel model) {
		if (!updating) {
			updating = true;
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						arquillian.update();
						containers.refresh();
						extensions.refresh();
						updateDefaultProtocol();
						updateEngine();
						refreshPomFragment();
					} finally {
						updating = false;
					}
				}
			});
		}
	}

	private void refreshPomFragment() {
		PomHelper helper = new PomHelper(arquillian);
		PomElement profiles = null;
		PomElement dependencies = null;
		PomElement dependencyManagement = null;
		
		if(pomFragmentLinkAction.isChecked()) {
			List<String> targetContainers = new ArrayList<String>();
			List<String> targetExtensions = new ArrayList<String>();
			List<String> targetProtocols = new ArrayList<String>();
			for(Object object : ArquillianUIUtil.getSelections(containers)) {
				if(object instanceof Container) {
					Container container = (Container) object;
					if(!targetContainers.contains(container.getType())) {
						targetContainers.add(container.getType());
					}
					for(org.jboss.tools.arquillian.ui.internal.editors.model.Protocol protocol : container.getProtocols()) {
						if(!targetProtocols.contains(protocol.getType())) {
							targetProtocols.add(protocol.getType());
						}
					}
				} else if(object instanceof Group) {
					for(Container container : ((Group) object).getContainers()) {
						if(!targetContainers.contains(container.getType())) {
							targetContainers.add(container.getType());
						}
						for(org.jboss.tools.arquillian.ui.internal.editors.model.Protocol protocol : container.getProtocols()) {
							if(!targetProtocols.contains(protocol.getType())) {
								targetProtocols.add(protocol.getType());
							}
						}
					}
				}
			}
			for(Object object : ArquillianUIUtil.getSelections(extensions)) {
				Extension extension = (Extension) object;
				if(!targetExtensions.contains(extension.getQualifier())) {
					targetExtensions.add(extension.getQualifier());
				}
			}
			if(defaultProtocols.isFocusControl() && StringUtils.isNotEmpty(defaultProtocols.getText())) {
				targetProtocols.add(defaultProtocols.getText());
			}
			profiles = helper.getProfiles(targetContainers);
			dependencies = helper.getDependencies(targetExtensions, targetProtocols);
			dependencyManagement = helper.getDependencyManagement(targetExtensions);
		} else {
			profiles = helper.getProfiles();
			dependencies = helper.getDependencies();
			dependencyManagement = helper.getDependencyManagement();
		}
		
		StringBuilder sb = new StringBuilder();
		if(profiles.getChildren().size() > 0) {
			sb.append(profiles.toString());
		}
		if(dependencyManagement.getChildren().get(0).getChildren().size() > 0) {
			if(sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(dependencyManagement.toString());
		}
		if(dependencies.getChildren().size() > 0) {
			if(sb.length() > 0) {
				sb.append("\n");
			}
			sb.append(dependencies.toString());
		}
		pomFragmentText.setText(sb.toString());
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		form.setText("Arquillian Configuration"); //$NON-NLS-1$
		TableWrapLayout layout = new TableWrapLayout();
		layout.makeColumnsEqualWidth = true;
		form.getBody().setLayout(layout);
		toolkit.decorateFormHeading(form.getForm());

		Composite composite = toolkit.createComposite(form.getBody());
		composite.setLayout(LayoutUtil.createTableWrapLayout(2, true));
		composite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		Composite left = toolkit.createComposite(composite);
		left.setLayout(new TableWrapLayout());
		left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		Composite right = toolkit.createComposite(composite);
		right.setLayout(new TableWrapLayout());
		right.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		createContainerControl(left);
		createExtensionControl(left);
		createDefaultProtocolControl(left);
		createPomFragmentControl(right);
		createEngineControl(form.getBody());
	}

	private void createContainerControl(Composite parent) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Container");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		Composite composite = toolkit.createComposite(section);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		section.setClient(composite);

		containers = new TreeViewer(toolkit.createTree(composite, SWT.MULTI));
		containers.getTree().setLayoutData(LayoutUtil.createTableWrapData(190));
		containers.setContentProvider(new ContainerGroupContentProvider());
		containers.setLabelProvider(new ArquillianEditorLabelProvider());
		containers.addDragSupport(DND.DROP_MOVE, new Transfer[] { TextTransfer.getInstance() },
				new ContainerGroupDragAdapter(containers));
		containers.addDropSupport(DND.DROP_MOVE, new Transfer[] { TextTransfer.getInstance() },
				new ContainerGroupDropAdapter(containers));
		containers.expandAll();

		Composite containerButtons = toolkit.createComposite(composite);
		containerButtons.setLayout(LayoutUtil.createFillLayout(SWT.VERTICAL, 5));
		Button btnGroupAdd = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Add Group",
				SWT.PUSH);
		btnGroupAdd.setLayoutData(LayoutUtil.createButtonLayoutData());
		Button btnContainerAdd = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons),
				"Add Container", SWT.NONE);
		btnContainerAdd.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnCopyContainers = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Copy", SWT.NONE);
		btnCopyContainers.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnCopyContainers.setEnabled(false);
		btnEditContainers = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Edit", SWT.NONE);
		btnEditContainers.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnEditContainers.setEnabled(false);
		btnRemoveContainers = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Remove",
				SWT.NONE);
		btnRemoveContainers.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnRemoveContainers.setEnabled(false);
		btnDefaultContainers = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Default",
				SWT.NONE);
		btnDefaultContainers.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnDefaultContainers.setEnabled(false);

		// --------------- event handler
		containers.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				refreshContainerButtonsState();
				refreshPomFragment();
			}
		});
		containers.getTree().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				// clear selection of extension's TreeViewer
				extensions.getTree().deselectAll();
				btnEditExtensions.setEnabled(false);
				btnRemoveExtensions.setEnabled(false);
			}
		});

		btnGroupAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianUIUtil.openWizard(new GroupWizard(containers));
				refreshPomFragment();
			}
		});
		btnContainerAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianUIUtil.openWizard(new ContainerWizard(containers));
				refreshPomFragment();
			}
		});
		btnCopyContainers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ContainerWizard wizard = new ContainerWizard(containers, (Container) ArquillianUIUtil
						.getSelection(containers));
				wizard.replication();
				ArquillianUIUtil.openWizard(wizard);
				refreshPomFragment();
			}
		});
		btnEditContainers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object element = ArquillianUIUtil.getSelection(containers);
				if (element instanceof Container) {
					ArquillianUIUtil.openWizard(new ContainerWizard(containers, (Container) element));
				} else if (element instanceof Group) {
					ArquillianUIUtil.openWizard(new GroupWizard(containers, (Group) element));
				}
				refreshPomFragment();
			}
		});
		btnRemoveContainers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ArquillianUIUtil.openConfirm("Remove Container/Group",
						"Are you sure you want to remove the container/group?")) {
					arquillian.remove((ArquillianModel) ArquillianUIUtil.getSelection(containers));
					containers.refresh();
					refreshPomFragment();
				}
			}
		});
		btnDefaultContainers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				((ContainerGroupModel) ArquillianUIUtil.getSelection(containers)).setDefault(true);
				containers.refresh();
			}
		});

	}

	private void refreshContainerButtonsState() {
		List<Object> selections = ArquillianUIUtil.getSelections(containers);
		if (selections.size() == 1) {
			btnEditContainers.setEnabled(true);
			btnRemoveContainers.setEnabled(true);
			btnDefaultContainers.setEnabled(true);
			if (selections.get(0) instanceof Container) {
				btnCopyContainers.setEnabled(true);
			} else {
				btnCopyContainers.setEnabled(false);
			}
		} else {
			btnEditContainers.setEnabled(false);
			btnRemoveContainers.setEnabled(false);
			btnDefaultContainers.setEnabled(false);
			btnCopyContainers.setEnabled(false);
		}
	}

	private void createExtensionControl(Composite parent) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Extension");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		Composite composite = toolkit.createComposite(section);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		section.setClient(composite);

		extensions = new TreeViewer(toolkit.createTree(composite, SWT.MULTI));
		extensions.getTree().setLayoutData(LayoutUtil.createTableWrapData(95));
		extensions.setContentProvider(new ExtensionContentProvider());
		extensions.setLabelProvider(new ArquillianEditorLabelProvider());

		Composite containerButtons = toolkit.createComposite(composite);
		containerButtons.setLayout(LayoutUtil.createFillLayout(SWT.VERTICAL, 5));
		Button btnAdd = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Add", SWT.PUSH);
		btnAdd.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnEditExtensions = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Edit", SWT.NONE);
		btnEditExtensions.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnEditExtensions.setEnabled(false);
		btnRemoveExtensions = toolkit.createButton(LayoutUtil.createButtonComposite(containerButtons), "Remove",
				SWT.NONE);
		btnRemoveExtensions.setLayoutData(LayoutUtil.createButtonLayoutData());
		btnRemoveExtensions.setEnabled(false);

		// ------------ event handler
		extensions.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (ArquillianUIUtil.getSelections(extensions).size() == 1) {
					btnEditExtensions.setEnabled(true);
					btnRemoveExtensions.setEnabled(true);
				} else {
					btnEditExtensions.setEnabled(false);
					btnRemoveExtensions.setEnabled(false);
				}
				refreshPomFragment();
			}
		});
		extensions.getTree().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				// clear selection of container's TreeViewer
				containers.getTree().deselectAll();
				btnEditContainers.setEnabled(false);
				btnCopyContainers.setEnabled(false);
				btnRemoveContainers.setEnabled(false);
				btnDefaultContainers.setEnabled(false);
			}
		});

		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianUIUtil.openWizard(new ExtensionWizard(extensions));
				refreshPomFragment();
			}
		});
		btnEditExtensions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ArquillianUIUtil.openWizard(new ExtensionWizard(extensions, (Extension) ArquillianUIUtil
						.getSelection(extensions)));
				refreshPomFragment();
			}
		});
		btnRemoveExtensions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ArquillianUIUtil.openConfirm("Remove Extension", "Are you sure you want to remove the extension?")) {
					arquillian.remove((ArquillianModel) ArquillianUIUtil.getSelection(extensions));
					extensions.refresh();
					refreshPomFragment();
				}
			}
		});
	}

	private void createDefaultProtocolControl(Composite parent) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Default Protocol");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		Composite composite = toolkit.createComposite(section);
		composite.setLayout(LayoutUtil.createTableWrapLayout(2));
		section.setClient(composite);

		toolkit.createLabel(composite, "type:");
		defaultProtocols = new Combo(composite, SWT.READ_ONLY);
		defaultProtocols.add("");
		for (Protocol protocol : ProtocolParser.getProtocols()) {
			defaultProtocols.add(protocol.getType());
		}

		toolkit.createLabel(composite, "properties:").setLayoutData(LayoutUtil.createTableWrapData(2, SWT.DEFAULT));

		defaultProtocolProperties = new TableViewer(toolkit.createTable(composite, SWT.BORDER | SWT.FULL_SELECTION));
		defaultProtocolProperties.getTable().setLayoutData(LayoutUtil.createTableWrapData(2, 95));
		defaultProtocolProperties.getTable().setLinesVisible(true);
		defaultProtocolProperties.getTable().setHeaderVisible(true);

		defaultProtocolProperties.setContentProvider(new ArrayContentProvider());

		PropertyEditingSupport editingSupport = new PropertyEditingSupport(defaultProtocolProperties);
		ArquillianEditor.createPropertyColumns(defaultProtocolProperties, editingSupport, 100, 250);

		defaultProtocolProperties.setLabelProvider(new PropertyLabelProvider());

		defaultProtocols.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				// clear selection of extension's TreeViewer
				extensions.getTree().deselectAll();
				btnEditExtensions.setEnabled(false);
				btnRemoveExtensions.setEnabled(false);
				// clear selection of container's TreeViewer
				containers.getTree().deselectAll();
				btnEditContainers.setEnabled(false);
				btnCopyContainers.setEnabled(false);
				btnRemoveContainers.setEnabled(false);
				btnDefaultContainers.setEnabled(false);
				refreshPomFragment();
			}
		});
		defaultProtocols.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do nothing if same type is selected.
				if (arquillian.getDefaultProtocol() != null
						&& arquillian.getDefaultProtocol().getType().equals(defaultProtocols.getText())) {
					return;
				}
				defaultProtocolProperties.getTable().removeAll();

				if (StringUtils.isNotEmpty(defaultProtocols.getText())) {
					// change gui
					List<Property> properties = new ArrayList<Property>();
					Protocol protocol = ProtocolParser.getProtocol(defaultProtocols.getText());
					if (protocol != null && protocol.getConfigurations() != null) {
						for (Configuration configuration : protocol.getConfigurations()) {
							properties.add(new Property(configuration.getName(), "", configuration.getType()));
						}
					}

					defaultProtocolProperties.setInput(properties);

					// change source
					DefaultProtocolInfo info = new DefaultProtocolInfo(defaultProtocols.getText(), null);
					if (arquillian.getDefaultProtocol() == null) {
						arquillian.add(info);
					} else {
						arquillian.edit(arquillian.getDefaultProtocol(), info);
					}
				} else {
					arquillian.remove(arquillian.getDefaultProtocol());
				}
				refreshPomFragment();
			}
		});
		editingSupport.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChanged(Property property) {
				@SuppressWarnings("unchecked")
				DefaultProtocolInfo info = new DefaultProtocolInfo(defaultProtocols.getText(),
						(List<Property>) defaultProtocolProperties.getInput());
				arquillian.edit(arquillian.getDefaultProtocol(), info);
			}
		});
	}

	private void updateDefaultProtocol() {
		if (arquillian.getDefaultProtocol() != null
				&& ProtocolParser.getProtocol(arquillian.getDefaultProtocol().getType()) != null) {
			defaultProtocols.setText(arquillian.getDefaultProtocol().getType());
			defaultProtocolProperties.setInput(ArquillianEditor.restoreProperties(
					ProtocolParser.getProtocol(defaultProtocols.getText()).getConfigurations(), arquillian
							.getDefaultProtocol().getProperties()));
		} else {
			defaultProtocols.setText("");
			defaultProtocolProperties.setInput(new ArrayList<Property>());
		}
		defaultProtocolProperties.refresh();
	}

	private void createPomFragmentControl(Composite parent) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR);
		section.setText("Pom Fragment");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		// create toolbar
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolBar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolBar.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				handCursor.dispose();
			}
		});
		toolBarManager.add(pomFragmentLinkAction);
		toolBarManager.update(true);
		section.setTextClient(toolBar);

		Composite composite = toolkit.createComposite(section);
		composite.setLayout(new TableWrapLayout());
		section.setClient(composite);

		ImageHyperlink copyImage = toolkit.createImageHyperlink(composite, SWT.RIGHT);
		copyImage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
		copyImage.setToolTipText("Copy to clipboard"); //$NON-NLS-1$
		copyImage.setText("pom.xml fragment of container, group, extension and protocol"); //$NON-NLS-1$
		copyImage.setUnderlined(false);
		copyImage.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

		pomFragmentText = toolkit.createText(composite, "", SWT.READ_ONLY | SWT.V_SCROLL);
		pomFragmentText.setLayoutData(LayoutUtil.createTableWrapData(490));

		// -------------- event handler
		copyImage.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (StringUtils.isEmpty(pomFragmentText.getText())) {
					return;
				}
				Clipboard clipboard = new Clipboard(Display.getDefault());
				clipboard.setContents(new Object[] { pomFragmentText.getText() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		});
	}

	private void createEngineControl(Composite parent) {
		Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		section.setExpanded(false);
		section.setText("Engine");
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		Composite composite = toolkit.createComposite(section);
		composite.setLayout(LayoutUtil.createTableWrapLayout(1));
		section.setClient(composite);

		toolkit.createLabel(composite, "properties:");

		engineProperties = new TableViewer(toolkit.createTable(composite, SWT.BORDER | SWT.FULL_SELECTION));
		engineProperties.getTable().setLinesVisible(true);
		engineProperties.getTable().setHeaderVisible(true);
		engineProperties.getTable().setLayoutData(LayoutUtil.createTableWrapData(95));
		engineProperties.setContentProvider(new ArrayContentProvider());
		final PropertyEditingSupport editingSupport = new PropertyEditingSupport(engineProperties);
		ArquillianEditor.createPropertyColumns(engineProperties, editingSupport, 300, 550);
		engineProperties.setLabelProvider(new PropertyLabelProvider());

		editingSupport.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChanged(Property property) {
				EngineInfo info = new EngineInfo(property);
				if (arquillian.getEngine() == null) {
					arquillian.add(info);
				} else {
					arquillian.edit(arquillian.getEngine(), info);
				}
			}
		});
	}

	private void updateEngine() {
		Property deploymentExportPath = new Property(ArquillianXmlElement.PROP_DEPLOYMENT_EXPORT_PATH, "",
				String.class.getCanonicalName());
		Property maxTestClassesBeforeRestart = new Property(ArquillianXmlElement.PROP_MAX_TEST_CLASSES_BEFORE_RESTART,
				"", Integer.class.getCanonicalName());
		if (arquillian.getEngine() != null) {
			deploymentExportPath.setValue(arquillian.getEngine().getDeploymentExportPath());
			maxTestClassesBeforeRestart.setValue(arquillian.getEngine().getMaxTestClassesBeforeRestart());
		}
		engineProperties.setInput(Arrays.asList(deploymentExportPath, maxTestClassesBeforeRestart));
		engineProperties.refresh();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	Arquillian getArquillian() {
		return arquillian;
	}

	private static class ContainerGroupDragAdapter extends DragSourceAdapter {

		private TreeViewer viewer;

		private ContainerGroupDragAdapter(TreeViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			ContainerGroupModel model = (ContainerGroupModel) ArquillianUIUtil.getSelection(viewer);
			String qualifier = null;
			String childQualifier = null;
			if (model instanceof Container && ((Container) model).isGroupContainer()) {
				qualifier = ((Container) model).getGroup().getQualifier();
				childQualifier = model.getQualifier();
			} else {
				qualifier = model.getQualifier();
			}
			String data = "qualifier:" + qualifier;
			if (childQualifier != null) {
				data = data + ",childQualifier:" + childQualifier;
			}
			event.data = data;
		}
	}

	private static class ContainerGroupDropAdapter extends ViewerDropAdapter {

		private static final Pattern PATTERN_DATA = Pattern.compile("qualifier:(.*)");
		private static final Pattern PATTERN_DATA_G = Pattern.compile("qualifier:(.*),childQualifier:(.*)");

		private ContainerGroupDropAdapter(TreeViewer viewer) {
			super(viewer);
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			return target instanceof ContainerGroupModel;
		}

		@Override
		public boolean performDrop(Object data) {
			if (data == null || ((TreeSelection) getViewer().getSelection()).size() != 1) {
				return false;
			}
			Arquillian arquillian = (Arquillian) getViewer().getInput();
			ContainerGroupModel model = getContainerGroupModel(arquillian, (String) data);
			if (model == null || model == getCurrentTarget()) {
				return false;
			}
			ContainerGroupModel target = (ContainerGroupModel) getCurrentTarget();
			switch (getCurrentLocation()) {
			case ViewerDropAdapter.LOCATION_BEFORE:
				if (target instanceof Container && ((Container) target).isGroupContainer() && model instanceof Group) {
					return false;
				}
				if (!checkDuplicateQualifier(arquillian, model, target)) {
					return false;
				}
				target.insertBefore(model);
				break;
			case ViewerDropAdapter.LOCATION_AFTER:
				if (target instanceof Container && ((Container) target).isGroupContainer() && model instanceof Group) {
					return false;
				}
				if (!checkDuplicateQualifier(arquillian, model, target)) {
					return false;
				}
				target.insertAfter(model);
				break;
			case ViewerDropAdapter.LOCATION_ON:
				if (model instanceof Container && target instanceof Group) {
					Container container = (Container) model;
					Group group = (Group) target;
					// check duplicate qualifier
					if (group.getContainer(container.getQualifier()) != null) {
						return false;
					}
					arquillian.remove(model);
					if (container.getGroup() != null) {
						container.getGroup().removeContainer(container);
					}
					container.setGroup(group);
					container.setDefault(false);
					group.addContainer(container);
					break;
				}
				return false;
			default:
				break;
			}
			getViewer().setInput(arquillian);
			((TreeViewer) getViewer()).expandAll();
			arquillian.doDirty();
			return true;
		}

		private ContainerGroupModel getContainerGroupModel(Arquillian arquillian, String data) {
			Matcher matcher = PATTERN_DATA_G.matcher(data);
			if (matcher.find()) {
				ContainerGroupModel group = arquillian.getContainerOrGroup(matcher.group(1));
				if (group != null && group instanceof Group) {
					return ((Group) group).getContainer(matcher.group(2));
				}
			} else {
				Matcher simpleMatcher = PATTERN_DATA.matcher(data);
				if (simpleMatcher.find()) {
					return arquillian.getContainerOrGroup(simpleMatcher.group(1));
				}
			}
			return null;
		}

		private boolean checkDuplicateQualifier(Arquillian arquillian, ContainerGroupModel model,
				ContainerGroupModel target) {
			if (target instanceof Container && ((Container) target).isGroupContainer()) {
				Container container = ((Container) target).getGroup().getContainer(model.getQualifier());
				if (container != null && container != model) {
					return false;
				}
			} else {
				ContainerGroupModel checked = arquillian.getContainerOrGroup(model.getQualifier());
				if (checked != null && checked != model) {
					return false;
				}
			}
			return true;
		}
	}

	// ------- for test
	public TreeViewer getContainers() {
		return containers;
	}

	public TreeViewer getExtensions() {
		return extensions;
	}
}
