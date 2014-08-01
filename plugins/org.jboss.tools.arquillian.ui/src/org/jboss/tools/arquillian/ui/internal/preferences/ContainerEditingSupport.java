/*************************************************************************************
 * Copyright (c) 2010-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.preferences;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.jboss.forge.arquillian.container.Container;

/**
 * @author snjeza
 * 
 */
public class ContainerEditingSupport extends EditingSupport {

	private CellEditor editor;
	private int column;

	public ContainerEditingSupport(ColumnViewer viewer, int column) {
		super(viewer);
		switch (column) {
		case 0:
			editor = new TextCellEditor(((TableViewer) viewer).getTable());
			break;
		case 1:
			editor = new TextCellEditor(((TableViewer) viewer).getTable());
			break;
		case 2:
			editor = new CheckboxCellEditor(((TableViewer) viewer).getTable());
			break;
		default:
			editor = new TextCellEditor(((TableViewer) viewer).getTable());
		}

		
		this.column = column;
	}


	@Override
	protected boolean canEdit(Object element) {
		if (this.column == 2) {
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
		Container container = (Container) element;
		String value = null;
		switch (this.column) {
		case 0:
			value = container.getId();
			if (value == null) {
				value = ""; //$NON-NLS-1$
			}
			return value;
		case 1:
			value = container.getName();
			if (value == null) {
				value = ""; //$NON-NLS-1$
			}
			return value;
		case 2:
			return container.isActivate();
		default:
			break;
		}
		return null;

	}

	@Override
	protected void setValue(Object element, Object value) {
		Container container = (Container) element;
		switch (this.column) {
		case 2:
			if (value instanceof Boolean) {
				container.setActivate((Boolean) value);
			} else {
				container.setActivate(false);
			}
			break;
		
		default:
			break;
		}

		getViewer().update(element, null);

	}

}
