/*************************************************************************************
 * Copyright (c) 2008-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.utils;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.arquillian.ui.internal.wizards.ProjectResource;

/**
 * 
 * Describes a deployment method
 * 
 * @author snjeza
 *
 */
public interface IDeploymentDescriptor {

	/**
	 * Returns whether this archive includes beans.xml
	 * 
	 * @return true if this archive includes beans.xml
	 */
	boolean addBeansXml();

	/**
	 * Returns this archive name or null
	 * 
	 * @return the archive name
	 */
	String getArchiveName();

	/**
	 * Returns this archive type (jar,war,ear or rar)
	 * 
	 * @return the archive type 
	 */
	String getArchiveType();

	/**
	 * Returns this deployment name or null
	 * 
	 * @return the deployment name
	 */
	String getDeploymentName();

	/**
	 * Returns this deployment order or null
	 * 
	 * @return the deployment order
	 */
	String getDeploymentOrder();

	/**
	 * Returns this method name
	 * 
	 * @return the method name
	 */
	String getMethodName();

	/**
	 * Returns resources included in this archive
	 * 
	 * @return resources included in this archive
	 */
	ProjectResource[] getResources();

	/**
	 * Returns types included in this archive
	 * 
	 * @return types included in this archive
	 */
	IType[] getTypes();

}
