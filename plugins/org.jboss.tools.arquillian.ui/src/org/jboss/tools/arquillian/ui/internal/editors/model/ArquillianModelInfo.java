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
 * The interface for generating model class.
 *
 */
public interface ArquillianModelInfo {

	/**
	 * Generates the instance of {@link ArquillianModel}.
	 * @param arquillian the {@link Arquillian}
	 * @return the instance of {@link ArquillianModel}
	 */
	public ArquillianModel generate(Arquillian arquillian);
	
}
