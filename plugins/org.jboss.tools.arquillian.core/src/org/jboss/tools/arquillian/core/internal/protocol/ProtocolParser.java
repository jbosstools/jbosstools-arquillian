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
package org.jboss.tools.arquillian.core.internal.protocol;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;

public class ProtocolParser {

	private static List<Protocol> protocols;
	
	/**
	 * Refresh parser
	 * 
	 */
	public static void refresh() {
		protocols = null;
	}
	
	/**
	 * Returns list of protocols
	 * 
	 */
	public static List<Protocol> getProtocols() {
		if (protocols != null) {
			return protocols;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		URL url = ArquillianUtility.getUrlFromBundle("/protocols.json"); //$NON-NLS-1$
		if (url != null) {
			try {
				protocols = objectMapper.readValue(url,
						new TypeReference<List<Protocol>>() {
						});
				return protocols;
			} catch (Exception e) {
				ArquillianCoreActivator.log(e);
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Returns a protocol
	 */
	public static Protocol getProtocol(String type) {
		for(Protocol protocol : getProtocols()) {
			if(protocol.getType().equals(type)) {
				return protocol;
			}
		}
		return null;
	}
}
