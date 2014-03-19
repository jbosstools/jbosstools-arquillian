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

import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.PossibleValuesService;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.modeling.Status;
import org.jboss.tools.arquillian.editor.internal.model.Configuration;
import org.jboss.tools.arquillian.editor.internal.model.Container;
import org.jboss.tools.arquillian.editor.internal.model.Protocol;
import org.jboss.tools.arquillian.editor.internal.utils.ArquillianEditorUtils;

/**
 * 
 * @author snjeza
 *
 */

public final class PropertyNamePossibleValuesService extends
		PossibleValuesService {
	
		@Override
	protected void initPossibleValuesService() {
		this.invalidValueSeverity = Status.Severity.OK;
	}

	@Override
	protected void compute(final Set<String> values) {
		Value<?> value = context(Value.class);
		if (value != null && value.element() != null && value.element().parent() != null) {
			Element element = value.element().parent().element();
			if (element instanceof Protocol) {
				Value<String> type = ((Protocol)element).getType();
				if (type != null && (Protocol.SERVLET_25.equals(type.text()) || Protocol.SERVLET_30.equals(type.text()))  ) {
					values.add("host");
					values.add("port");
					values.add("contextRoot");
				}
			}
			else if (element instanceof Configuration) {
				Element parentElement = element.parent().element();
				if (parentElement instanceof Container) {
					Value<String> qualifierValue = ((Container)parentElement).getQualifier();
					org.jboss.forge.arquillian.container.Container c = ArquillianEditorUtils.getContainer(qualifierValue.text());
					if (c != null) {
						List<org.jboss.forge.arquillian.container.Configuration> configurations = c.getConfigurations();
						for (org.jboss.forge.arquillian.container.Configuration configuration:configurations) {
							if (configuration != null && configuration.getName() != null) {
								values.add(configuration.getName());
							}
						}
					}
				}
			}
		}
	}

}
