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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.utils.ArquillianUIUtil;

/**
 * 
 * @author snjeza
 * 
 */
public class ArquillianPropertyTester extends PropertyTester {

	private static final String IS_ARQUILLIAN_JUNIT_TEST = "isArquillianJUnitTest";
	private static final String CAN_LAUNCH_AS_ARQUILLIAN_TEST = "canLaunchAsArquillianTest";

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (CAN_LAUNCH_AS_ARQUILLIAN_TEST.equals(property)) {
			IJavaElement element = getElement(receiver);
			if (element == null) {
				return false;
			}
			return canLaunchAsArquillianJUnitTest(element);
		} else if (IS_ARQUILLIAN_JUNIT_TEST.equals(property)) {
			if (receiver instanceof IType) {
				return ArquillianSearchEngine.isArquillianJUnitTest((IType) receiver, false);
			}
			if (receiver instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) receiver;
				try {
					IType[] types = icu.getAllTypes();
					if (types.length > 0) {
						return ArquillianSearchEngine.isArquillianJUnitTest(types[0], false);
					}
				} catch (JavaModelException e) {
					ArquillianUIActivator.log(e);
				}
			}
			if (receiver instanceof ITextSelection) {
				return ArquillianSearchEngine.isArquillianJUnitTest(ArquillianUIUtil.getActiveType(), false);
			}
		}
		return false;
	}
	
	private IJavaElement getElement(Object receiver) {
		if (!(receiver instanceof IAdaptable)) {
			return null;
		}
		IJavaElement element;
		if (receiver instanceof IJavaElement) {
			element= (IJavaElement) receiver;
		} else if (receiver instanceof IResource) {
			element = JavaCore.create((IResource) receiver);
		} else {
			element= (IJavaElement) ((IAdaptable) receiver).getAdapter(IJavaElement.class);
			if (element == null) {
				IResource resource= (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
				element = JavaCore.create(resource);
			}
		}
		return element;
	}

	private boolean canLaunchAsArquillianJUnitTest(IJavaElement element) {
		try {
			switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					return true; // can run, let test runner detect if there are tests
				case IJavaElement.PACKAGE_FRAGMENT:
					return ((IPackageFragment) element).hasChildren();
				case IJavaElement.COMPILATION_UNIT:
				case IJavaElement.CLASS_FILE:
				case IJavaElement.TYPE:
				case IJavaElement.METHOD:
					return ArquillianSearchEngine.isArquillianJUnitTest(element, true);
				default:
					return false;
			}
		} catch (JavaModelException e) {
			return false;
		}
	}
	
}
