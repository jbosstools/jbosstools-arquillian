/*******************************************************************************
 * Copyright (c) 2000-2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.util.xpl;


/**
 * A security exception that is thrown by the ArquillianSecurityManager if
 * 
 * Based on <code>org.eclipse.ant.core.AntSecurityException</code>
 */
public class ArquillianSecurityException extends SecurityException {

	private static final long serialVersionUID = 1L;

	public ArquillianSecurityException(String s) {
		super(s);
	}

}
