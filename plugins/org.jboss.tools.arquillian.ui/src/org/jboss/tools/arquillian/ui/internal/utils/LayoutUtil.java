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
package org.jboss.tools.arquillian.ui.internal.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class LayoutUtil {
	
	private static final int WIDTH_BUTTON = 110;

	/**
	 * Creates a <code>GridLayout</code> with the given columns.
	 *
	 * @param columns number of columns
	 * @return the created <code>GridLayout</code>
	 */
	public static GridLayout createGridLayout(int columns) {
		return createGridLayout(columns, false);
	}
	
	/**
	 * Creates a <code>GridLayout</code> with the given columns and makeColumnsEqualWidth flag.
	 *
	 * @param columns number of columns
	 * @param makeColumnsEqualWidth the flag of whether to make width equal
	 * @return the created <code>GridLayout</code>
	 */
	public static GridLayout createGridLayout(int columns, boolean makeColumnsEqualWidth) {
		GridLayout layout = new GridLayout(columns, makeColumnsEqualWidth);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		return layout;
	}
	
	/**
	 * Creates a <code>TableWrapLayout</code> with the given columns.
	 *
	 * @param columns number of columns
	 * @return the created <code>TableWrapLayout</code>
	 */
	public static TableWrapLayout createTableWrapLayout(int columns) {
		return createTableWrapLayout(columns, false);
	}

	/**
	 * Creates a <code>TableWrapLayout</code> with the given columns and makeColumnsEqualWidth flag.
	 *
	 * @param columns number of columns
	 * @param makeColumnsEqualWidth the flag of whether to make width equal
	 * @return the created <code>TableWrapLayout</code>
	 */
	public static TableWrapLayout createTableWrapLayout(int columns, boolean makeColumnsEqualWidth) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = columns;
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.leftMargin = 0;
		return layout;
	}
	
	/**
	 * Creates a <code>TableWrapData</code> with the given height.
	 *
	 * @param height the height
	 * @return the created <code>TableWrapData</code>
	 */
	public static TableWrapData createTableWrapData(int height) {
		return createTableWrapData(1, height);
	}

	/**
	 * Creates a <code>TableWrapData</code> with the given colspan and height.
	 *
	 * @param colspan the colspan
	 * @param height the height
	 * @return the created <code>TableWrapData</code>
	 */
	public static TableWrapData createTableWrapData(int colspan, int height) {
		return createTableWrapData(colspan, height, TableWrapData.FILL_GRAB);
	}
	
	/**
	 * Creates a <code>TableWrapData</code> with the given colspan and height and align.
	 *
	 * @param colspan the colspan
	 * @param height the height
	 * @param align the align
	 * @return the created <code>TableWrapData</code>
	 */
	public static TableWrapData createTableWrapData(int colspan, int height, int align) {
		TableWrapData data = new TableWrapData(align);
		data.colspan = colspan;
		data.heightHint = height;
		return data;
	}
	
	/**
	 * Creates a <code>FillLayout</code> with the given type and spacing.
	 *
	 * @param type the type
	 * @param spacing the spacing
	 * @return the created <code>FillLayout</code>
	 */
	public static FillLayout createFillLayout(int type, int spacing) {
		FillLayout layout = new FillLayout(type);
		layout.spacing = spacing;
		return layout;
	}
	
	/**
	 * Creates a composite control.
	 *
	 * @param parent the parent control
	 * @return the created composite
	 */
	public static Composite createButtonComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		return composite;
	}
	
	/**
	 * Creates a <code>GridData</code> for button layout.
	 * @return the created <code>GridData</code>
	 */
	public static GridData createButtonLayoutData() {
		GridData data = new GridData();
		data.widthHint = WIDTH_BUTTON;
		return data;
	}
	
	
}
