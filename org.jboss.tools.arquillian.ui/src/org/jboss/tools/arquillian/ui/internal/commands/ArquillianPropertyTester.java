/*************************************************************************************
 * Copyright (c) 2008-2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.commands;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (receiver instanceof IType) {
			return ArquillianUIActivator.isArquillianJUnitTestCase((IType) receiver);
		}
		if (receiver instanceof ICompilationUnit) {
			ICompilationUnit icu = (ICompilationUnit) receiver;
			try {
				IType[] types = icu.getAllTypes();
				if (types.length > 0) {
					return ArquillianUIActivator.isArquillianJUnitTestCase(types[0]);
				}
			} catch (JavaModelException e) {
				ArquillianUIActivator.log(e);
			}
		}
		if (receiver instanceof ITextSelection) {
			//ITextSelection textSelection = (ITextSelection) receiver;
			return ArquillianUIActivator.isArquillianJUnitTestCase(ArquillianUIActivator.getActiveType());
		}
		return false;
	}
	
}
