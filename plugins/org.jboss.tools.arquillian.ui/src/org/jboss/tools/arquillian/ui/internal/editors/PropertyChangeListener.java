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

import org.jboss.tools.arquillian.ui.internal.editors.model.Property;

/**
 * The interface for listeners performed at the time of a propety change.
 *
 */
public interface PropertyChangeListener {

	/**
	 * Performs at the time of property change.
	 * @param property the property
	 */
	public void propertyChanged(Property property);
	
}
