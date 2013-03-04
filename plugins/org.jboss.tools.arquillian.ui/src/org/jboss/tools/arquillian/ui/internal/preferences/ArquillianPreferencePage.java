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
package org.jboss.tools.arquillian.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;
import org.jboss.tools.arquillian.core.internal.preferences.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.launcher.AutoResizeTableLayout;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.jboss.tools.arquillian.ui.preferences.arquillianPreferencePage"; //$NON-NLS-1$
	private Combo combo;
	private CheckboxTableViewer profilesViewer;
	private List<Container> containers;
	private Image checkboxOn;
	private Image checkboxOff;
	
	private static final String[] defaultVersions = new String[] {ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT};
	
	private static final String COORDS = ArquillianUtility.ARQUILLIAN_GROUP_ID + ":" + ArquillianUtility.ARQUILLIAN_BOM_ARTIFACT_ID + ":[0,)";  //$NON-NLS-1$ //$NON-NLS-2$
	
	@Override
	public void init(IWorkbench workbench) {
		containers = ContainerParser.getContainers();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        composite.setLayout(new GridLayout(2, false));
        
        Label label = new Label(composite, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        label.setLayoutData(gd);
        label.setText("Arquillian version:");
        combo = new Combo(composite, SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        combo.setLayoutData(gd);
        combo.setItems(ArquillianUtility.getVersions(COORDS, defaultVersions));
        String value = ArquillianUtility.getPreference(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(value);
        
        Group profilesGroup = new Group(composite, SWT.NONE);
        profilesGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        gd.horizontalSpan = 2;
        profilesGroup.setLayoutData(gd);
        profilesGroup.setText("Profiles");
        Label profilesLabel = new Label(profilesGroup, SWT.NONE);
        gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        profilesLabel.setLayoutData(gd);
        profilesLabel.setText("Select those profiles that you want to include automatically when adding the Arquillian support.");
        new Label(profilesGroup, SWT.NONE);
        profilesViewer = createProfilesViewer(profilesGroup);
        
		return composite;
	}

	private CheckboxTableViewer createProfilesViewer(Composite parent) {
		final CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.heightHint = 300;
		viewer.getTable().setLayoutData(gd);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());
		
		viewer.setContentProvider(new ContainerContentProvider());
		
		String[] columnHeaders = {"ID", "Name"};
		
		for (int i = 0; i < columnHeaders.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new ContainerLabelProvider(i));
			column.getColumn().setText(columnHeaders[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.setEditingSupport(new ContainerEditingSupport(viewer, i));
		}
		
		ColumnLayoutData[] containersLayouts= {
				new ColumnWeightData(200,200),
				new ColumnWeightData(150,150),
				//new ColumnWeightData(50,50)
			};
		
		TableLayout layout = new AutoResizeTableLayout(table);
		for (int i = 0; i < containersLayouts.length; i++) {
			layout.addColumnData(containersLayouts[i]);
		}
		
		viewer.getTable().setLayout(layout);
		
		configureViewer(viewer);
		
		viewer.setInput(containers);
		initializeViewer(viewer);
		
		return viewer;
	}

	private void initializeViewer(CheckboxTableViewer viewer) {
		List<String> selectedProfiles = ArquillianUtility.getProfilesFromPreferences(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES);
		List<String> activatedProfiles = ArquillianUtility.getProfilesFromPreferences(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES);

		for(Container container:containers) {
			container.setActivate(activatedProfiles.contains(container.getId()));
			viewer.setChecked(container, selectedProfiles.contains(container.getId()));
		}
		viewer.refresh();
	}

	private void configureViewer(final CheckboxTableViewer viewer) {
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
    protected void performDefaults() {
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        prefs.setValue(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		prefs.setValue(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, ArquillianConstants.JBOSS_AS_REMOTE_7_X);
		initializeViewer(profilesViewer);
		profilesViewer.refresh();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
    	IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, combo.getText());
		StringBuilder aBuilder = new StringBuilder();
		StringBuilder sBuilder = new StringBuilder();
		for (Iterator<Container> iterator = containers.iterator(); iterator.hasNext();) {
			Container container = (Container) iterator.next();
			if (container.isActivate()) {
				if (aBuilder.length() > 0) {
					aBuilder.append(ArquillianConstants.COMMA);
				}
				aBuilder.append(container.getId());
			}
			if (profilesViewer.getChecked(container)) {
				if (sBuilder.length() > 0) {
					sBuilder.append(ArquillianConstants.COMMA);
				}
				sBuilder.append(container.getId());
			}
		}
		prefs.setValue(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES, sBuilder.toString());
		prefs.setValue(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES, aBuilder.toString());
        return super.performOk();
    }

    class ContainerContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object inputElement) {
			return containers.toArray();
		}
		public void dispose() {
		}
	}
	
	class ContainerLabelProvider extends ColumnLabelProvider {

		private int columnIndex;

		public ContainerLabelProvider(int i) {
			this.columnIndex = i;
		}

		public String getText(Object element) {
			if (element instanceof Container) {
				Container container = (Container) element;
				switch (columnIndex) {
				case 0:
					return container.getId();
				case 1:
					String name = container.getName();
					if (name == null) {
						return null;
					}
					return name.replace(Container.ARQUILLIAN_CONTAINER_NAME_START,""); //$NON-NLS-1$
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element == null) {
				return null;
			}
			Container container = (Container) element;
			if (columnIndex == 2) {
				if (container.isActivate()) {
					return getCheckOnImage();
				}
				return getCheckOffImage();
			}
			
			return null;
		}
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
	}
}
