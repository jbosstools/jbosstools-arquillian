/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.editor.internal.services;

import java.util.List;
import java.util.Set;

import org.eclipse.sapphire.PossibleValuesService;
import org.eclipse.sapphire.modeling.Status;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.internal.container.ContainerParser;

/**
 * 
 * @author snjeza
 *
 */

public final class ContainerQualifierPossibleValuesService extends
		PossibleValuesService {
	
	@Override
	protected void initPossibleValuesService() {
		this.invalidValueSeverity = Status.Severity.OK;
	}

	@Override
	protected void compute(final Set<String> values) {
		List<Container> containers = ContainerParser.getContainers();
		for(Container container:containers) {
			values.add(container.getId());
		}
	}

}
