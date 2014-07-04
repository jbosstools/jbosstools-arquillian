/*************************************************************************************
 * Copyright (c) 2008-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.launcher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerEvent;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.natures.ArquillianNature;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.commands.AddArquillianProfilesCommandHandler;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;
import org.osgi.framework.Bundle;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianTab extends AbstractLaunchConfigurationTab {

	private static final String MANAGEMENT_ADDRESS = "managementAddress"; //$NON-NLS-1$
	private static final String MANAGEMENT_PORT = "managementPort"; //$NON-NLS-1$
	private static final String RUN_MODE = "run"; //$NON-NLS-1$
	private static final String DEBUG_MODE = "debug"; //$NON-NLS-1$
	
	public static final String ID = "org.jboss.tools.arquillian.ui.arquillianTab"; //$NON-NLS-1$
	private Image checkboxOn;
	private Image checkboxOff;
	private Set<ArquillianProperty> properties;
	private TableViewer propertiesViewer;
	private TableViewer serversViewer;
	private Button saveButton;
	private ILaunchConfiguration configuration;
	private Button selectProfilesButton;
	private Button addProfilesButton;
	private Image image;
	
	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (configuration == null) {
				return;
			}
			String name = null;
			try {
				name = ArquillianUtility.getJavaProject(configuration).getElementName();
			} catch (CoreException e) {
				// ignore
			}
			if (name == null) {
				return;
			}
			if (event.getType() == IResourceChangeEvent.POST_BUILD) {
				
				IResourceDelta delta = event.getDelta();
				IResourceDelta projectDelta = delta.findMember(new Path(name));
				if (projectDelta == null) {
					return;
				}
				IResource project = projectDelta.getResource();
				if (project != null && project instanceof IProject) {
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							if (propertiesViewer == null || propertiesViewer.getControl() == null) {
								return;
							}
							if (propertiesViewer.getControl().isDisposed()) {
								dispose();
								return;
							}
							properties = null;
							initializeFrom(configuration);
							ILaunchConfigurationTab[] tabs = getLaunchConfigurationDialog().getTabs();
							for (ILaunchConfigurationTab tab:tabs) {
								if (tab instanceof ArquillianLaunchConfigurationTab) {
									((ArquillianLaunchConfigurationTab)tab).validatePage();
									break;
								}
							}
							getLaunchConfigurationDialog().updateButtons();
							getLaunchConfigurationDialog().updateMessage();
						}
					});
				}
			}
		}
	};
	private IServer[] servers;
	
	private IServerListener serverListener = new IServerListener() {
		
		@Override
		public void serverChanged(ServerEvent event) {
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					if (serversViewer != null && !serversViewer.getControl().isDisposed()) {
						serversViewer.refresh();
						updateServerButtons();
					}
				}
			});
		}
	};
	private Button startButton;
	private Button debugButton;
	private Button stopButton;
	private Button testButton;
	
	public ArquillianTab() {
		image = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "icons/arquillian_icon16.png").createImage(); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
		composite.setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		composite.setFont(parent.getFont());
		Dialog.applyDialogFont(composite);
		
		Group propertiesGroup = new Group(composite, SWT.NONE);
		propertiesGroup.setLayout(new GridLayout(1, false));
		propertiesGroup.setText("Configuration properties");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		propertiesGroup.setLayoutData(gd);
		
		propertiesViewer = createPropertiesSection(propertiesGroup);
		
		Composite propertiesButtonComposite = new Composite(composite, SWT.NONE);
		gd = new GridData(SWT.RIGHT, SWT.FILL, true, false);
		propertiesButtonComposite.setLayoutData(gd);
		
		propertiesButtonComposite.setLayout(new GridLayout(4, false));
		testButton = createButton(propertiesButtonComposite, "Test Management");
		testButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (properties == null) {
					return;
				}
				final String[] managementAddress = new String[1];
				final String[] managementPort = new String[1];
				for (ArquillianProperty property : properties) {
					String name = getPropertyName(property);
					if (MANAGEMENT_PORT.equals(name)) {
						managementPort[0] = property.getValue();
					}
					if (MANAGEMENT_ADDRESS.equals(name)) {
						managementAddress[0] = property.getValue();
					}
				}
				if (managementAddress[0] != null && managementPort[0] != null
						&& !managementAddress[0].isEmpty()
						&& !managementPort[0].isEmpty()) {

					BusyIndicator.showWhile(Display.getDefault(),
							new Runnable() {

								@Override
								public void run() {
									Socket socket = null;
									try {
										InetAddress addr = InetAddress
												.getByName(managementAddress[0]);
										int port = 0;
										try {
											port = Integer
													.valueOf(managementPort[0]);
										} catch (NumberFormatException e1) {
											ArquillianUIActivator.log(e1);
											return;
										}
										SocketAddress sockaddr = new InetSocketAddress(
												addr, port);
										socket = new Socket();
										socket.connect(sockaddr, 15000);
										MessageDialog.openInformation(
												getShell(), "Test Management",
												"Test succeeded");

									} catch (Exception e1) {
										MessageDialog
												.openInformation(
														getShell(),
														"Test Management",
														"Test failed.\n"
																+ e1.getLocalizedMessage());
									} finally {
										if (socket != null) {
											try {
												socket.close();
											} catch (IOException e1) {
												// ignore
											}
										}
									}
								}
							});

				}
			}
			
		});
		addProfilesButton = createButton(propertiesButtonComposite, ArquillianConstants.ADD_ARQUILLIAN_PROFILES);
		addProfilesButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject project = getProject();
				new AddArquillianProfilesCommandHandler().execute(project);
			}
		});
		addProfilesButton.setEnabled(isArquillianProject(configuration));
		if (mavenProfileExists()) {
			selectProfilesButton = createButton(propertiesButtonComposite, ArquillianConstants.SELECT_MAVEN_PROFILES);
			selectProfilesButton.addSelectionListener(new SelectionAdapter() {	
				@Override
				public void widgetSelected(SelectionEvent e) {
					ArquillianUtility.runAction(configuration, ArquillianConstants.SELECT_MAVEN_PROFILES_COMMAND, true);
				}
			});
				
			selectProfilesButton.setEnabled(isArquillianProject(configuration));
		} else {
			new Label(propertiesButtonComposite, SWT.NONE);
		}
		
		saveButton = createButton(propertiesButtonComposite, "Save");
		saveButton.setEnabled(false);
		saveButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					ArquillianUIUtil.save(properties, configuration );
					saveButton.setEnabled(false);
				} catch (CoreException e1) {
					MessageDialog.openConfirm(getShell(), "Error", e1.getMessage());
				}
			}
			
		});
		Group serversGroup = new Group(composite, SWT.NONE);
		serversGroup.setLayout(new GridLayout(1, false));
		serversGroup.setText("Servers");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		serversGroup.setLayoutData(gd);
		
		serversViewer = createServersSection(serversGroup);
		
		Composite serversButtonComposite = new Composite(composite, SWT.NONE);
		gd = new GridData(SWT.RIGHT, SWT.FILL, true, false);
		serversButtonComposite.setLayoutData(gd);
		
		serversButtonComposite.setLayout(new GridLayout(3, false));
		
		startButton = createButton(serversButtonComposite, "Start");
		startButton.setEnabled(false);
		startButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final IServer server = getSelectedServer();
				if (server == null) {
					return;
				}
				Job job = new Job("Starting ...") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							server.start(RUN_MODE, monitor);
						} catch (CoreException e) {
							return Status.CANCEL_STATUS;
						}
						return Status.OK_STATUS;
					}
					
				};
				job.setSystem(true);
				job.schedule();
			}
			
		});
		
		debugButton = createButton(serversButtonComposite, "Debug");
		debugButton.setEnabled(false);
		debugButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final IServer server = getSelectedServer();
				if (server == null) {
					return;
				}
				Job job = new Job("Starting ...") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							server.start(DEBUG_MODE, monitor);
						} catch (CoreException e) {
							return Status.CANCEL_STATUS;
						}
						return Status.OK_STATUS;
					}
					
				};
				job.setSystem(true);
				job.schedule();
			}
			
		});
		
		stopButton = createButton(serversButtonComposite, "Stop");
		stopButton.setEnabled(false);
		stopButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final IServer server = getSelectedServer();
				if (server == null) {
					return;
				}
				Job job = new Job("Starting ...") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						server.stop(false);
						return Status.OK_STATUS;
					}
					
				};
				job.setSystem(true);
				job.schedule();
			}
			
		});
		
		serversViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateServerButtons();
				
			}
		});
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener, IResourceChangeEvent.POST_BUILD);
		
	}

	private boolean isArquillianProject(ILaunchConfiguration conf) {
		IJavaProject javaProject;
		try {
			javaProject = ArquillianUtility.getJavaProject(conf);
			return javaProject != null && javaProject.getProject() != null && javaProject.getProject().isAccessible() && javaProject.getProject().hasNature(ArquillianNature.ARQUILLIAN_NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	private boolean mavenProfileExists() {
		Bundle bundle = Platform.getBundle(ArquillianConstants.MAVEN_PROFILES_UI_PLUGIN_ID);
		return bundle != null;
	}
	
	private Button createButton(Composite parent, String label) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);
		GridData gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		gd.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(gd);
		return button;
	}

	private int convertHorizontalDLUsToPixels(int dlus) {
		GC gc = new GC(getControl());
		gc.setFont(getControl().getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return (fontMetrics.getAverageCharWidth() * dlus + 2)
				/ 4;
	}

	private TableViewer createServersSection(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.SINGLE | SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 80;
		viewer.getTable().setLayoutData(gd);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());
		
		viewer.setContentProvider(new ServersContentProvider());
		
		String[] columnHeaders = {"Name", "Runtime", "Home", "Mode"};
		
		for (int i = 0; i < columnHeaders.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new ServersLabelProvider(i));
			column.getColumn().setText(columnHeaders[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			//column.setEditingSupport(new ArquillianPropertiesEditingSupport(viewer, i));
		
		}
		
		ColumnLayoutData[] layouts= {
				new ColumnWeightData(80,80),
				new ColumnWeightData(60,60),
				new ColumnWeightData(140,140),
				new ColumnWeightData(40,40),
				
			};
		
		TableLayout layout = new AutoResizeTableLayout(table);
		for (int i = 0; i < layouts.length; i++) {
			layout.addColumnData(layouts[i]);
		}
		
		viewer.getTable().setLayout(layout);		
		//configureViewer(viewer);
		viewer.setInput(getServers());
		return viewer;
	}
	
	private IServer[] getServers() {
		removeServerListeners();
		servers = ServerCore.getServers();
		List<IServer> l = new ArrayList<IServer>();
		for (IServer server:servers) {
			if (server != null && server.getRuntime() != null && server.getRuntime().getLocation() != null)
				l.add(server);
		}
		addServerListeners();
		return l.toArray(new IServer[0]);
	}

	private void addServerListeners() {
		if (servers != null) {
			for (IServer server:servers) {
				server.addServerListener(serverListener);
			}
		}
	}
	
	private void removeServerListeners() {
		if (servers != null) {
			for (IServer server:servers) {
				server.removeServerListener(serverListener);
			}
		}
	}

	private TableViewer createPropertiesSection(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 90;
		viewer.getTable().setLayoutData(gd);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());
		
		viewer.setContentProvider(new PropertiesContentProvider());
		
		String[] columnHeaders = {"Name", "Value", "Source", "Default?"};
		
		for (int i = 0; i < columnHeaders.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new PropertiesLabelProvider(i));
			column.getColumn().setText(columnHeaders[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.setEditingSupport(new ArquillianPropertiesEditingSupport(viewer, i));
		
		}
		
		ColumnLayoutData[] layouts= {
				new ColumnWeightData(150,150),
				new ColumnWeightData(120,120),
				new ColumnWeightData(60,60),
				new ColumnWeightData(30,30),
				
			};
		
		TableLayout layout = new AutoResizeTableLayout(table);
		for (int i = 0; i < layouts.length; i++) {
			layout.addColumnData(layouts[i]);
		}
		
		viewer.getTable().setLayout(layout);		
		configureViewer(viewer);
		viewer.setInput(properties);
		return viewer;
	}


	private void configureViewer(final TableViewer viewer) {
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(viewer, new FocusCellOwnerDrawHighlighter(viewer));
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				ViewerCell cell = viewer.getColumnViewerEditor().getFocusCell();
				if (cell != null && cell.getColumnIndex() == 1) {
					return super.isEditorActivationEvent(event);
				}
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		properties = null;
		initializeFrom(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		if (propertiesViewer != null && propertiesViewer.getControl() != null && !propertiesViewer.getControl().isDisposed() && isArquillianProject(configuration)) {
			properties = ArquillianUIUtil
					.getArquillianProperties(configuration);
			propertiesViewer.setInput(properties);
			propertiesViewer.refresh();
			updatePropertiesButton();
		}
		this.configuration = configuration;
		updateButton(addProfilesButton);
		updateButton(selectProfilesButton);
		updateButton(testButton);
	}

	private void updateButton(Button button) {
		if (button != null && !button.isDisposed()) {
			button.setEnabled(isArquillianProject(configuration));
		}
	}

	private void updatePropertiesButton() {
		if (properties == null) {
			testButton.setEnabled(false);
		}
		boolean hasManagementPort = false;
		boolean hasManagementAddress = false;
		for (ArquillianProperty property:properties) {
			String name = getPropertyName(property);
			if (MANAGEMENT_PORT.equals(name)) {
				hasManagementPort = true;
			}
			if (MANAGEMENT_ADDRESS.equals(name)) {
				hasManagementAddress = true;
			}
		}
		testButton.setEnabled(hasManagementAddress && hasManagementPort);
	}

	private String getPropertyName(ArquillianProperty property) {
		if (property == null) {
			return null;
		}
		String name = property.getName();
		if (name == null) {
			return null;
		}
		int index = name.lastIndexOf("."); //$NON-NLS-1$
		if (index > 0) {
			name = name.substring(index + 1);
		}
		return name;
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		
	}

	@Override
	public String getName() {
		return "Arquillian";
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 * 
	 * @since 3.3
	 */
	public String getId() {
		return ID;
	}
	
	public Image getCheckOnImage() {
		if (checkboxOn == null) {
			checkboxOn = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "/icons/xpl/complete_tsk.gif").createImage(); //$NON-NLS-1$
		}
		return checkboxOn;
	}
	
	public Image getCheckOffImage() {
		if (checkboxOff == null) {
			checkboxOff = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "/icons/xpl/incomplete_tsk.gif").createImage(); //$NON-NLS-1$
		}
		return checkboxOff;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if (checkboxOn != null) {
			checkboxOn.dispose();
			checkboxOn = null;
		}
		if (checkboxOff != null) {
			checkboxOff.dispose();
			checkboxOff = null;
		}
		if (image != null) {
			image.dispose();
			image = null;
		}
		if (resourceChangeListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		}
		removeServerListeners();
	}
	
	private IServer getSelectedServer() {
		ISelection sel = serversViewer.getSelection();
		IServer  server = null;
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			server = (IServer) selection.getFirstElement();
		}
		return server;
	}

	private void updateServerButtons() {
		startButton.setEnabled(false);
		debugButton.setEnabled(false);
		stopButton.setEnabled(false);
		IServer server = getSelectedServer();
		if (server == null) {
			return;
		}
		int state = server.getServerState();
		switch (state) {
		case IServer.STATE_STOPPED:
			startButton.setEnabled(true);
			debugButton.setEnabled(true);
			break;
		case IServer.STATE_STARTED:
			stopButton.setEnabled(true);
			break;

		default:
			break;
		}
	}

	private class PropertiesLabelProvider extends ColumnLabelProvider {

		private int columnIndex;
		
		public PropertiesLabelProvider(int columnIndex) {
			this.columnIndex = columnIndex;
		}
		
		@Override
		public Image getImage(Object element) {
			if (element == null) {
				return null;
			}
			ArquillianProperty properties = (ArquillianProperty) element;
			if (columnIndex == 3) {
				if (properties.isDefaultValue())
					return getCheckOnImage();
				else
					return getCheckOffImage();
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ArquillianProperty) {
				ArquillianProperty properties = (ArquillianProperty) element;
				if (columnIndex == 0) {
					String name = properties.getName();
					int index = name.lastIndexOf("."); //$NON-NLS-1$
					if (index > 0) {
						name = name.substring(index+1);
					}
					return name;
				}
				if (columnIndex == 1) {
					return properties.getValue();
				}
				if (columnIndex == 2) {
					return properties.getSource();
				}

			}
			return null;
		}
	}
	
	private class PropertiesContentProvider implements IStructuredContentProvider {
		
		@Override
		public Object[] getElements(Object inputElement) {
			return properties.toArray(new ArquillianProperty[0]);
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			properties = (Set<ArquillianProperty>) newInput;
		}

	}
	
	private class ServersContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return servers;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			servers = (IServer[]) newInput;
		}

	}

	private class ServersLabelProvider extends ColumnLabelProvider {

		private int columnIndex;
		
		public ServersLabelProvider(int columnIndex) {
			super();
			this.columnIndex = columnIndex;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof IServer)) {
				return null;
			}
			IServer server = (IServer) element;
			switch (columnIndex) {
			case 0:
				return server.getName();
			
			case 1:
				IRuntime runtime = server.getRuntime();
				if (runtime != null) {
					return runtime.getName();
				}
				break;
			
			case 2:
				IRuntime rt = server.getRuntime();
				if( rt != null && rt.getLocation() != null) {
					return rt.getLocation().toOSString();
				}
				return ""; //$NON-NLS-1$
			
			case 3:
				String mode = server.getMode();
				int state = server.getServerState();
				
				if (state == IServer.STATE_STARTING) {
					return "Starting";
				}
				if (state == IServer.STATE_STOPPING) {
					return "Stopping";
				}
				if (state == IServer.STATE_STOPPED) {
					return "Stopped";
				}
				if (state == IServer.STATE_STARTED) {
					if (DEBUG_MODE.equals(mode)) {
						return "Debugging";
					}
					return "Started";
				}
				return "Unknown";
			default:
				break;
			}
			return null;
		}
		
	}
	
	private class ArquillianPropertiesEditingSupport extends EditingSupport {

		private CellEditor editor;
		private int column;

		public ArquillianPropertiesEditingSupport(ColumnViewer viewer, int column) {
			super(viewer);
			switch (column) {
			case 3:
				editor = new CheckboxCellEditor(((TableViewer) viewer).getTable());
				break;
			default:
				editor = new TextCellEditor(((TableViewer) viewer).getTable());
			}

			
			this.column = column;
		}


		@Override
		protected boolean canEdit(Object element) {
			if (this.column == 1) {
				return true;
			}
			return false;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected Object getValue(Object element) {
			ArquillianProperty properties =  (ArquillianProperty) element;
			String value = null;
			switch (this.column) {
			case 0:
				value = properties.getName();
				if (value == null) {
					value = ""; //$NON-NLS-1$
				}
				return value;
			case 1:
				value = properties.getValue();
				if (value == null) {
					value = ""; //$NON-NLS-1$
				}
				return value;
			case 2:
				value = properties.getSource();
				if (value == null) {
					value = ""; //$NON-NLS-1$
				}
				return value;
			case 3:
				boolean isDefault = properties.isDefaultValue();
				return isDefault;
			default:
				break;
			}
			return null;

		}

		@Override
		protected void setValue(Object element, Object value) {
			ArquillianProperty property =  (ArquillianProperty) element;
			
			switch (this.column) {
			case 1:
				if (value != null) {
					property.setValue(value.toString());
				} else {
					property.setValue(null);
				}
				property.setChanged(true);
				property.setDefaultValue(false);
				property.setSource("arquillian.properties"); //$NON-NLS-1$
				ISelection selection = getViewer().getSelection();
				getViewer().setSelection(null);
				getViewer().setSelection(selection);
				saveButton.setEnabled(true);
				break;
			
			default:
				break;
			}

			getViewer().update(element, null);

		}

	}

	@Override
	public Image getImage() {
		return image;
	}

	private IProject getProject() {
		IProject project = null;
		try {
			IJavaProject javaProject = ArquillianUtility.getJavaProject(configuration);
			
			if (javaProject != null) {
				project = javaProject.getProject();
			}
		} catch (CoreException e1) {
			ArquillianUIActivator.log(e1);
		}
		return project;
	}
	
}
