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
package org.jboss.tools.arquillian.ui.internal.editors.model;

/**
 * The base model class for ArquillianEditor. 
 *
 */
public abstract class ArquillianModel {
	
	protected Arquillian arquillian;
	
	public ArquillianModel(){}
	
	public ArquillianModel(Arquillian arquillian) {
		this.arquillian = arquillian;
	}
	
	public String getText() {
		return null;
	}
	
	/**
	 * Applies information of the specified {@link ArquillianModelInfo} to this model.
	 * @param info the {@link ArquillianModelInfo}
	 */
	protected abstract void apply(ArquillianModelInfo info);

	/**
	 * Appends itself to the specified {@link Arquillian}.
	 * @param arquillian the {@link Arquillian}
	 */
	protected abstract void appendTo(Arquillian arquillian);
	
	/**
	 * Removes itself from the specified {@link Arquillian}.
	 * @param arquillian the {@link Arquillian}
	 */
	protected abstract void removeFrom(Arquillian arquillian);
	
}
