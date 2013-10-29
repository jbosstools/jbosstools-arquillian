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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * 
 * @author snjeza
 *
 */
public class DependencyVisitor extends ASTVisitor {

	private Set<DependencyType> types = new HashSet<DependencyType>();
	private String exclude;
	private CompilationUnit cu;
	
	public DependencyVisitor(CompilationUnit cu, String exclude) {
		this.exclude = exclude;
		this.cu = cu;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
	 */
	@Override
	public boolean visit(SimpleName node) {
		ITypeBinding binding = node.resolveTypeBinding();
		if (binding != null && !binding.isPrimitive() && binding.isFromSource()) {
			for (final String name : binding.getQualifiedName()
					.split("[<>,\\s\\[\\]]+")) { //$NON-NLS-1$
				if (!name.equals(exclude)) {
					int nodeType = node.getNodeType();
					ASTNode parent = node.getParent();
					int charStart;
					int charEnd;
					if (parent != null && parent.getNodeType() == ASTNode.QUALIFIED_NAME) {
						charStart = parent.getStartPosition();
						charEnd = charStart + parent.getLength();
					} else {
						charStart = node.getStartPosition();
						charEnd = charStart + node.getLength();
					}
					int lineNumber = cu.getLineNumber(charStart);
					DependencyType type = new DependencyType(name);
					for (DependencyType t:types) {
						if (type.equals(t)) {
							type = t;
							break;
						}
					}
					TypeLocation location =  new TypeLocation(charStart, charEnd, lineNumber);
					type.getLocations().add(location);
					types.add(type);
				}
				
			}
		}
		return true;
	}


	public Set<DependencyType> getTypes() {
		return types;
	}
}
