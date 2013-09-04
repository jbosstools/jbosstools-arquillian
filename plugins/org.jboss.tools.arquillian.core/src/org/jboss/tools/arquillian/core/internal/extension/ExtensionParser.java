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
package org.jboss.tools.arquillian.core.internal.extension;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

public class ExtensionParser {

	private static List<Extension> extensions;
	
	/**
	 * Refresh parser
	 * 
	 */
	public static void refresh() {
		extensions = null;
	}
	
	/**
	 * Returns list of extensions
	 * 
	 */
	public static List<Extension> getExtensions() {
		if (extensions != null) {
			return extensions;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		URL url = ArquillianUtility.getUrlFromBundle("/extensions.json"); //$NON-NLS-1$
		if (url != null) {
			try {
				extensions = objectMapper.readValue(url,
						new TypeReference<List<Extension>>() {
						});
				return extensions;
			} catch (Exception e) {
				ArquillianCoreActivator.log(e);
			}
		}
		return Collections.emptyList();
	}
	
	public static Extension getExtension(String qualifier) {
		for(Extension extension : getExtensions()) {
			if(extension.getQualifier().equals(qualifier)) {
				return extension;
			}
		}
		return null;
	}
}
