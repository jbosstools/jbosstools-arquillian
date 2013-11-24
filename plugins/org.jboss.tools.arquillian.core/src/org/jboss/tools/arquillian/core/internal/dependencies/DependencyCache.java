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
package org.jboss.tools.arquillian.core.internal.dependencies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;

/**
 * 
 * @author snjeza
 *
 */
public class DependencyCache {
	
	private static Map<ICompilationUnit, Set<DependencyType>> dependencies = new HashMap<ICompilationUnit, Set<DependencyType>>();

	public static Map<ICompilationUnit, Set<DependencyType>> getDependencies() {
		return dependencies;
	}
	
	public static void collectDependencies(ICompilationUnit unit) {
		if (unit == null || getDependencies().get(unit) != null) {
			return;
		}
		CompilationUnit cu = getAST(unit);
		IType primaryType = unit.findPrimaryType();
		if (primaryType == null) {
			return;
		}
		String name = primaryType.getFullyQualifiedName();
		DependencyVisitor visitor = new DependencyVisitor(cu, name);
		cu.accept(visitor);
		getDependencies().put(unit, visitor.getTypes());
		IJavaProject javaProject = unit.getJavaProject();
		for (DependencyType dependencyType: visitor.getTypes()) {
			String typeName = dependencyType.getName();
			try {
				IType type = javaProject.findType(typeName);
				if (type == null) {
					continue;
				}
				ICompilationUnit compilationUnit = type.getCompilationUnit();
				collectDependencies(compilationUnit);
			} catch (JavaModelException e) {
				ArquillianCoreActivator.log(e);
			}
		}
		return;
	}
	
	public static Set<DependencyType> getDependentTypes(ICompilationUnit unit) {
		Set<DependencyType> types = new HashSet<DependencyType>();
		if (unit == null) {
			return types;
		}
		IJavaProject javaProject = unit.getJavaProject();
		DependencyCache.collectDependencies(unit);
		Set<DependencyType> toVisit = new HashSet<DependencyType>();
		toVisit.addAll(getDependencies().get(unit));
		types.addAll(toVisit);
		Set<DependencyType> visited = new HashSet<DependencyType>();
		Set<DependencyType> add = new HashSet<DependencyType>();
		while (!toVisit.isEmpty()) {
			Iterator<DependencyType> iter = toVisit.iterator();
			add.clear();
			while (iter.hasNext()) {
				DependencyType type = iter.next();
				if (visited.contains(type)) {
					iter.remove();
					continue;
				}
				
				try {
					IType javaType = javaProject.findType(type.getName());
					if (javaType != null) {
						ICompilationUnit cu = javaType.getCompilationUnit();
						if (cu != null && DependencyCache.getDependencies().get(cu) != null) {
							add.addAll(DependencyCache.getDependencies().get(cu));
							types.addAll(DependencyCache.getDependencies().get(cu));
						}
					}
				} catch (JavaModelException e) {
					ArquillianCoreActivator.log(e);
				}
				visited.add(type);
			}
			toVisit.addAll(add);
		}
		return types;
	}
	
	private static CompilationUnit getAST(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.JLS4);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(cu.getJavaProject());
		return (CompilationUnit) parser.createAST(null);
	}

	public static void removeDependencies(IResource resource) {
		if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			Set<ICompilationUnit> keySet = getDependencies().keySet();
			Iterator<ICompilationUnit> iter = keySet.iterator();
			while (iter.hasNext()) {
				ICompilationUnit unit = iter.next();
				if (project.equals(unit.getJavaProject().getProject())) {
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
    		getDependencies().remove(unit);
		}
	}
}
