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
package org.jboss.tools.arquillian.core.internal.util;

/**
 * Provides utility methods related to {@code String} handling.
 *
 */
public class StringUtils {

	/**
	 * Tests whether a string is empty or not. Returns {@code true} if null or the trimmed string length is {@literal 0}.
	 * @param target the target value
	 * @return {@code true} if the string value is empty, otherwise false
	 */
	public static boolean isEmpty(String target) {
		return target == null || target.trim().length() == 0;
	}

	/**
	 * Tests whether a string is empty or not. Returns {@code true} if it is not empty.
	 * This method returns opposite result of {@link #isEmpty(String)} returns.
	 * @param target the target value
	 * @return {@code true} if it is not empty, otherwise false
	 */
	public static boolean isNotEmpty(String target) {
		return !isEmpty(target);
	}
	
}
