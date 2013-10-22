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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.jboss.tools.arquillian.ui.internal.editors.model.Property;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * The implementation of <code>EditingSupport</code> for editing property in {@link ArquillianEditor}.
 *
 */
public class PropertyEditingSupport extends EditingSupport {

	private CellEditor editor;
	private PropertyChangeListener listener;
	
	public PropertyEditingSupport(TableViewer viewer) {
		super(viewer);
		editor = new TextCellEditor(viewer.getTable());
	}
	
	@Override
	protected boolean canEdit(Object element) {
		return true;
	}
	
	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}
	
	@Override
	protected Object getValue(Object element) {
		return ((Property) element).getValue();
	}
	
	@Override
	protected void setValue(Object element, Object value) {
		Property property = (Property) element;
		if(property.getValue().equals(value.toString())) {
			return;
		}
		try {
			property.validate(value.toString());
		} catch (Exception e) {
			ArquillianUIUtil.openErrorDialog("Validate Error", String.format("The value of %s is unavailable.", property.getName()), e);
			return;
		}
		property.setValue(value.toString());
		getViewer().update(element, null);
		if(listener != null) {
			listener.propertyChanged((Property) element);
		}
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.listener = listener;
	}
}
