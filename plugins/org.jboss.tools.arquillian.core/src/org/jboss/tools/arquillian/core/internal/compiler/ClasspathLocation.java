/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     JBoss by Red Hat
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.compiler;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public abstract class ClasspathLocation {

	static ClasspathLocation forSourceFolder(IContainer sourceFolder,
			IContainer outputFolder, char[][] inclusionPatterns,
			char[][] exclusionPatterns) {
		return new ClasspathMultiDirectory(sourceFolder, outputFolder,
				inclusionPatterns, exclusionPatterns);
	}

	public static ClasspathLocation forBinaryFolder(IContainer binaryFolder,
			boolean isOutputFolder, AccessRuleSet accessRuleSet) {
		return new ClasspathDirectory(binaryFolder, isOutputFolder,
				accessRuleSet);
	}

	static ClasspathLocation forLibrary(String libraryPathname,
			long lastModified, AccessRuleSet accessRuleSet) {
		return new ClasspathJar(libraryPathname, lastModified, accessRuleSet);
	}

	static ClasspathLocation forLibrary(String libraryPathname,
			AccessRuleSet accessRuleSet) {
		return forLibrary(libraryPathname, 0, accessRuleSet);
	}

	static ClasspathLocation forLibrary(IFile library,
			AccessRuleSet accessRuleSet) {
		return new ClasspathJar(library, accessRuleSet);
	}

	private boolean arquillianDeploymentFolder = false;;

	public abstract NameEnvironmentAnswer findClass(String binaryFileName,
			String qualifiedPackageName, String qualifiedBinaryFileName);

	public abstract IPath getProjectRelativePath();
	
	public abstract String getPath();

	public boolean isOutputFolder() {
		return false;
	}

	public abstract boolean isPackage(String qualifiedPackageName);

	public void cleanup() {
		// free anything which is not required when the state is saved
	}

	public void reset() {
		// reset any internal caches before another compile loop starts
	}

	public abstract String debugPathString();

	public boolean isArquillianDeploymentFolder() {
		return this.arquillianDeploymentFolder;
	}

	public void setArquillianDeploymentFolder(boolean arquillianDeploymentFolder) {
		this.arquillianDeploymentFolder = arquillianDeploymentFolder;
	}
}