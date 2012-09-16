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
package org.jboss.tools.arquillian.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.dialogs.ArquillianResourcesSelectionDialog;
import org.jboss.tools.arquillian.ui.internal.dialogs.ArquillianTypesSelectionDialog;

/**
 * 
 * @author snjeza
 *
 */
public class NewArquillianJUnitTestCaseDeploymentPage extends WizardPage {

	private static final int WEB_INF_RESOURCE_COLUMN_WIDTH = 120;
	private static final int HEIGHT_HINT = 150;
	public static final String ORG_JBOSS_TOOLS_ARQUILLIAN_UI_DEPLOYMENT_PAGE = "org.jboss.tools.arquillian.ui.deploymentPage";

	public static String[] archiveTypes = { "jar", "war", "ear" };
	private Text methodNameText;
	private Combo archiveTypeCombo;
	private Button beansXmlButton;
	private Text archiveNameText;
	private IJavaElement javaElement;
	private TableViewer typesViewer;
	private TableViewer resourcesViewer;
	private Text deploymentNameText;
	private Text deploymentOrderText;
	private Image checkboxOn;
	private Image checkboxOff;
	private TableViewerColumn webinfColumn;
	private List<IType> types;
	private List<ProjectResource> resources;
	
	public NewArquillianJUnitTestCaseDeploymentPage() {
		this(null);
	}
	
	public NewArquillianJUnitTestCaseDeploymentPage(IJavaElement javaElement) {
		super(ORG_JBOSS_TOOLS_ARQUILLIAN_UI_DEPLOYMENT_PAGE); //$NON-NLS-1$
		
		setTitle("Create Arquillian Deployment Method");
		setDescription("Create Arquillian Deployment Method");
		this.javaElement = javaElement;
		types = new ArrayList<IType>();
		resources = new ArrayList<ProjectResource>();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2,false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		Dialog.applyDialogFont(composite);
		setControl(composite);
		
		// method name
		Label methodNameLabel = new Label(composite, SWT.NONE);
		methodNameLabel.setText("Method name:");
		
		methodNameText = new Text(composite, SWT.BORDER);
		methodNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// deployment name
		Label deploymentNameLabel = new Label(composite, SWT.NONE);
		deploymentNameLabel.setText("Deployment name:");
				
		deploymentNameText = new Text(composite, SWT.BORDER);
		deploymentNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// deployment order
		Label deploymentOrderLabel = new Label(composite, SWT.NONE);
		deploymentOrderLabel.setText("Deployment order:");
						
		deploymentOrderText = new Text(composite, SWT.BORDER);
		deploymentOrderText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deploymentOrderText.addListener(SWT.Verify, new Listener() {
			
			public void handleEvent(Event e) {
				String string = e.text;
				char[] chars = new char[string.length()];
				string.getChars(0, chars.length, chars, 0);
				for (int i = 0; i < chars.length; i++) {
					if (!('0' <= chars[i] && chars[i] <= '9')) {
						Display.getCurrent().beep();
						e.doit = false;
						return;
					}
				}
			}
		});
	
		// archive type
		Label  archiveTypeLabel = new Label(composite, SWT.NONE);
		archiveTypeLabel.setText("Archive type:");
		
		archiveTypeCombo = new Combo(composite, SWT.READ_ONLY);
		archiveTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		archiveTypeCombo.setItems(archiveTypes);
		
		//archive name
		Label archiveNameLabel = new Label(composite, SWT.NONE);
		archiveNameLabel.setText("Archive name:");
		
		archiveNameText = new Text(composite, SWT.BORDER);
		archiveNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		// beans.xml
		beansXmlButton  = new Button(composite, SWT.CHECK);
		gd = new GridData();
		gd.horizontalSpan = 2;
		beansXmlButton.setText("Add an empty beans.xml file");
		
		// FIXME 
		methodNameText.setText("createDeployment");
		archiveTypeCombo.select(0);
		archiveNameText.setText("test");
		beansXmlButton.setSelection(true);
		
		methodNameText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				String text = methodNameText.getText();
				setErrorMessage(null);
				if (text.isEmpty()) {
					setErrorMessage("The deployment method name is required.");
					return;
				}
				IJavaElement context = null;
				if (javaElement != null) {
					context = javaElement;
				}
				IJavaProject javaProject = getJavaProject();
				if (javaProject != null) {
					context = javaProject;
				}
				if (context != null) {
					IStatus status = validateMethodName(text, context);
					if (!status.isOK()) {
						setErrorMessage(status.getMessage());
					}
				}
			}
		});
		
		archiveTypeCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshResourceViewer();
			}
		
		});

		createTypesControls(composite);
		createResourcesControls(composite);
	}
	
	private void createResourcesControls(Composite composite) {
		
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setFont(composite.getFont());
		group.setText("Resources");
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.heightHint = HEIGHT_HINT;
		group.setLayoutData(gd);

		//resourcesViewer = CheckboxTableViewer.newCheckList(group, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		resourcesViewer = new TableViewer(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		Table table = resourcesViewer.getTable();
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		
		table.setLayoutData(gd);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnHeaders = new String[] { "Path", "WEB-INF resource?"};
		int[] columnWidths = new int[] {250, WEB_INF_RESOURCE_COLUMN_WIDTH};

		TableViewerColumn pathColumn = new TableViewerColumn(resourcesViewer, SWT.NONE);
		pathColumn.setLabelProvider(new ResourceLabelProvider(0));
		pathColumn.getColumn().setText(columnHeaders[0]);
		pathColumn.getColumn().setResizable(false);
		pathColumn.getColumn().setMoveable(false);
		pathColumn.getColumn().setWidth(columnWidths[0]);
		//pathColumn.setEditingSupport(new ResourceEditingSupport(resourcesViewer, 0));
		
		webinfColumn = new TableViewerColumn(resourcesViewer, SWT.NONE);
		webinfColumn.setLabelProvider(new ResourceLabelProvider(1));
		webinfColumn.getColumn().setText(columnHeaders[1]);
		webinfColumn.getColumn().setResizable(false);
		webinfColumn.getColumn().setMoveable(false);
		if (ArquillianUIActivator.WAR.equals(archiveTypeCombo.getText())) {
			webinfColumn.getColumn().setWidth(WEB_INF_RESOURCE_COLUMN_WIDTH);
		} else {
			webinfColumn.getColumn().setWidth(0);
		}
		webinfColumn.setEditingSupport(new ResourceEditingSupport(resourcesViewer, 1));
				
		configureViewer(resourcesViewer);
		resourcesViewer.setContentProvider(new ResourceContentProvider());
		
		createButtons(group, resourcesViewer, resources);

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
	
	private void createTypesControls(Composite composite) {
		
		Group group = new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout(2, false));
		group.setFont(composite.getFont());
		group.setText("Classes");
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		gd.heightHint = HEIGHT_HINT;
		group.setLayoutData(gd);

		//typesViewer = CheckboxTableViewer.newCheckList(group, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		typesViewer = new TableViewer(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		gd= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		typesViewer.getTable().setLayoutData(gd);

		typesViewer.setLabelProvider(new TypeLabelProvider());
		typesViewer.setContentProvider(new TypeContentProvider());
		
		createButtons(group, typesViewer, types);

	}

	private void createButtons(Group group, final TableViewer viewer, final List<?> elements) {
		GridData gd;
		Composite buttonContainer= new Composite(group, SWT.NONE);
		gd= new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout= new GridLayout();
		buttonLayout.marginWidth= 0;
		buttonLayout.marginHeight= 0;
		buttonContainer.setLayout(buttonLayout);

		Button addButton = new Button(buttonContainer, SWT.PUSH);
		addButton.setText("Add");
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		addButton.setLayoutData(gd);
		LayoutUtil.setButtonDimensionHint(addButton);

		final Button removeButton = new Button(buttonContainer, SWT.PUSH);
		removeButton.setText("Remove");
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		removeButton.setLayoutData(gd);
		LayoutUtil.setButtonDimensionHint(removeButton);
		
		final Button removeAllButton = new Button(buttonContainer, SWT.PUSH);
		removeAllButton.setText("Remove All");
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		removeAllButton.setLayoutData(gd);
		removeAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				elements.clear();
				if (viewer == typesViewer) {
					viewer.setInput(elements.toArray(new IType[0]));
				}
				if (viewer == resourcesViewer) {
					viewer.setInput(elements.toArray(new ProjectResource[0]));
				}
				viewer.refresh();
				updateButtons(viewer, elements, removeButton, removeAllButton);
			}
		});
		LayoutUtil.setButtonDimensionHint(removeAllButton);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons(viewer, elements, removeButton, removeAllButton);
			}
		});
		updateButtons(viewer, elements, removeButton, removeAllButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					Object[] selectedElements = sel.toArray();
					for (Object element:selectedElements) {
						elements.remove(element);
					}
				}
				if (viewer == typesViewer) {
					viewer.setInput(elements.toArray(new IType[0]));
				}
				if (viewer == resourcesViewer) {
					viewer.setInput(elements.toArray(new ProjectResource[0]));
				}
				viewer.refresh();
				updateButtons(viewer, elements, removeButton, removeAllButton);
			}
		});
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (viewer == typesViewer) {
					ArquillianTypesSelectionDialog dialog = new ArquillianTypesSelectionDialog(getShell(), getJavaProject(), types);
					dialog.create();
					if (dialog.open() == Window.OK) {
						Object[] results = dialog.getResult();
						if (results != null)  {
							for (Object result:results) {
								if (result instanceof IType) {
									types.add((IType) result);
								}
							}
						}
						viewer.setInput(types.toArray(new IType[0]));
						viewer.refresh();
						updateButtons(viewer, elements, removeButton, removeAllButton);
					}
				}
				
				if (viewer == resourcesViewer) {
					List<IPath> res = new ArrayList<IPath>();
					for (ProjectResource resource:resources) {
						res.add(resource.getPath());
					}
					ArquillianResourcesSelectionDialog dialog = new ArquillianResourcesSelectionDialog(getShell(), getJavaProject(), res);
					dialog.create();
					if (dialog.open() == Window.OK) {
						Object[] results = dialog.getResult();
						if (results != null)  {
							for (Object result:results) {
								if (result instanceof IPath) {
									resources.add(new ProjectResource((IPath) result, false));
								}
							}
						}
						viewer.setInput(resources.toArray(new ProjectResource[0]));
						viewer.refresh();
						updateButtons(viewer, elements, removeButton, removeAllButton);
					}
				}
			}
		});
		
	}

	protected void updateButtons(TableViewer viewer, List<?> elements, Button removeButton, Button removeAllButton) {
		removeAllButton.setEnabled(elements.size() > 0);
		removeButton.setEnabled(!viewer.getSelection().isEmpty());
	}

	protected IJavaProject getJavaProject() {
		if (javaElement != null) {
			return javaElement.getJavaProject();
		}
		IWizardPage page = getWizard().getPages()[0];
		if (page instanceof NewArquillianJUnitTestCasePageOne) {
			NewArquillianJUnitTestCasePageOne pageOne = (NewArquillianJUnitTestCasePageOne) page;
			IPackageFragmentRoot root = pageOne.getPackageFragmentRoot();
			if (root != null) {
				return root.getJavaProject();
			}
		}
		return null;
	}

	private static IStatus validateMethodName(String name, IJavaElement context) {
		String[] sourceComplianceLevels= getSourceComplianceLevels(context);
		return JavaConventions.validateMethodName(name, sourceComplianceLevels[0], sourceComplianceLevels[1]);
	}
	
	private static String[] getSourceComplianceLevels(IJavaElement context) {
		if (context != null) {
			IJavaProject javaProject= context.getJavaProject();
			if (javaProject != null) {
				return new String[] {
						javaProject.getOption(JavaCore.COMPILER_SOURCE, true),
						javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true)
				};
			}
		}
		return new String[] {
				JavaCore.getOption(JavaCore.COMPILER_SOURCE),
				JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE)
		};
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		IWizard wizard = getWizard();
		if (wizard instanceof NewArquillianJUnitTestWizard) {
			NewArquillianJUnitTestWizard arquillianWizard = (NewArquillianJUnitTestWizard) wizard;
			NewArquillianJUnitTestCasePageOne pageOne = arquillianWizard.getNewArquillianJUnitTestCasePageOne();
			if (pageOne.getClassUnderTest() != null) {
				return super.getPreviousPage();
			} else {
				return arquillianWizard.getNewArquillianJUnitTestCasePageOne();
			}
		}
		return super.getPreviousPage();
	}

	public String getMethodName() {
		return methodNameText.getText();
	}
	
	public String getDeploymentName() {
		return deploymentNameText.getText();
	}
	
	public String getDeploymentOrder() {
		return deploymentOrderText.getText();
	}
	
	public String getArchiveType() {
		return archiveTypeCombo.getText();
	}
	
	public String getArchiveName() {
		return archiveNameText.getText();
	}

	public boolean addBeansXml() {
		return beansXmlButton.getSelection();
	}
	
	public IType[] getTypes() {
		return types.toArray(new IType[0]);
	}
	
	public ProjectResource[] getResources() {
		return resources.toArray(new ProjectResource[0]);
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
	}
	
	public Image getCheckOnImage() {
		if (checkboxOn == null) {
			checkboxOn = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "/icons/xpl/complete_tsk.gif").createImage();
		}
		return checkboxOn;
	}
	
	public Image getCheckOffImage() {
		if (checkboxOff == null) {
			checkboxOff = ArquillianUIActivator.imageDescriptorFromPlugin(ArquillianUIActivator.PLUGIN_ID, "/icons/xpl/incomplete_tsk.gif").createImage();
		}
		return checkboxOff;
	}
	
	private void refreshResourceViewer() {
		if (resourcesViewer != null && !resourcesViewer.getControl().isDisposed()) {
			if (ArquillianUIActivator.WAR.equals(archiveTypeCombo.getText())) {
				webinfColumn.getColumn().setWidth(WEB_INF_RESOURCE_COLUMN_WIDTH);
			} else {
				webinfColumn.getColumn().setWidth(0);
			}
			resourcesViewer.refresh(true);
		}
	}

	private class ResourceLabelProvider extends ColumnLabelProvider {

		private int columnIndex;

		public ResourceLabelProvider(int i) {
			this.columnIndex = i;
		}

		public String getText(Object element) {
			if (element instanceof ProjectResource) {
				switch (columnIndex) {
				case 0:
					return ((ProjectResource) element).getPath().toString();
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element == null || !ArquillianUIActivator.WAR.equals(archiveTypeCombo.getText())) {
				return null;
			}
			ProjectResource resourceDeployment = (ProjectResource) element;
			if (columnIndex == 1) {
				return resourceDeployment.isDeployAsWebInfResource() ? getCheckOnImage() : getCheckOffImage();
			}
			
			return null;
		}

	}

}
