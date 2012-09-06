/*************************************************************************************
 * Copyright (c) 2010-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

/**
 * @author snjeza
 * 
 */
public class ResourceEditingSupport extends EditingSupport {

	private CellEditor editor;
	private int column;

	public ResourceEditingSupport(ColumnViewer viewer, int column) {
		super(viewer);
		switch (column) {
		case 0:
			editor = new TextCellEditor(((TableViewer) viewer).getTable());
			break;
		case 1:
			editor = new CheckboxCellEditor(((TableViewer) viewer).getTable());
			break;
		default:
			editor = new TextCellEditor(((TableViewer) viewer).getTable());
		}

		
		this.column = column;
	}


	@Override
	protected boolean canEdit(Object element) {
		if (this.column == 0) {
			return false;
		}
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected Object getValue(Object element) {
		ProjectResource resourceDeployment = (ProjectResource) element;
		String value = null;
		switch (this.column) {
		case 0:
			value = resourceDeployment.getPath().toString();
			if (value == null) {
				value = ""; //$NON-NLS-1$
			}
			return value;
		case 1:
			return resourceDeployment.isDeployAsWebInfResource();
		default:
			break;
		}
		return null;

	}

	@Override
	protected void setValue(Object element, Object value) {
		ProjectResource resourceDeployment = (ProjectResource) element;
		
		switch (this.column) {
		case 0:
			if (value != null) {
				resourceDeployment.setPath(new Path(value.toString()));
			} else {
				resourceDeployment.setPath(null);
			}
			ISelection selection = getViewer().getSelection();
			getViewer().setSelection(null);
			getViewer().setSelection(selection);
			break;
		case 1:
			resourceDeployment.setDeployAsWebInfResource((Boolean)value);
			break;
		
		default:
			break;
		}

		getViewer().update(element, null);

	}

}
