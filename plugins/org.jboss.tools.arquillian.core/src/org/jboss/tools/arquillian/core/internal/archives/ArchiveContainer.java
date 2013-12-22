/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.archives;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.ProjectNatureDescriptor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

/**
 * 
 * @author snjeza
 * 
 */
public class ArchiveContainer {

	private static Map<ArchiveLocation, Archive> archives = new HashMap<ArchiveLocation, Archive>();
	
	public static Archive getArchive(ArchiveLocation location) {
		return archives.get(location);
	}

	public static void putArchive(ArchiveLocation location, Archive archive) {
		archives.put(location, archive);
	}

	public static void remove(IResource resource) {
		if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			String projectName = project.getName();
			Set<ArchiveLocation> keySet = archives.keySet();
			Iterator<ArchiveLocation> iter = keySet.iterator();
			while (iter.hasNext()) {
				ArchiveLocation location = iter.next();
				if (projectName.equals(location.getProjectName())) {
					iter.remove();
				}
			}
		} else if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			IJavaElement element = JavaCore.create(file);
    		if (!(element instanceof ICompilationUnit)) {
    			return;
    		}
    		ICompilationUnit unit = (ICompilationUnit) element;
    		IType type = unit.findPrimaryType();
    		if (type != null) {
    			String fqn = type.getFullyQualifiedName();
    			String projectName = resource.getProject().getName();
    			Set<ArchiveLocation> keySet = archives.keySet();
    			Iterator<ArchiveLocation> iter = keySet.iterator();
    			while (iter.hasNext()) {
    				ArchiveLocation location = iter.next();
    				if (projectName.equals(location.getProjectName()) && fqn.equals(location.getFullyQualifiedName())) {
    					iter.remove();
    				}
    			}
    		}
		}
	}
}
