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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ElementList;
import org.eclipse.sapphire.Event;
import org.eclipse.sapphire.Listener;
import org.eclipse.sapphire.LocalizableText;
import org.eclipse.sapphire.Property;
import org.eclipse.sapphire.Text;
import org.eclipse.sapphire.Value;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.services.ValidationService;
import org.jboss.tools.arquillian.editor.internal.model.Arquillian;
import org.jboss.tools.arquillian.editor.internal.model.Container;
import org.jboss.tools.arquillian.editor.internal.model.Group;

/**
 * 
 * @author snjeza
 * 
 */
public class UniqueQualifierBetweenContainerAndGroupService extends
		ValidationService {

	@Text("@qualifier must be unique between containers and groups.")
	private static LocalizableText message;
	
	static
    {
        LocalizableText.init( UniqueQualifierBetweenContainerAndGroupService.class );
    }

	private Listener listener;

	@Override
	protected void initValidationService() {
		listener = new Listener()
        {
            @Override
            public void handle( final Event event )
            {
                refresh();
            }
        };
        Arquillian arquillian = context(Arquillian.class);
		arquillian.attach(listener, "Group/Qualifier");
		arquillian.attach(listener, "Container/Qualifier");
	}

	@Override
	public void dispose() {
		super.dispose();
		Arquillian arquillian = context(Arquillian.class);
		arquillian.detach(listener, "Group/Qualifier");
		arquillian.detach(listener, "Container/Qualifier");
	}

	@Override
	protected Status compute() {
		synchronized (this) {
			Arquillian arquillian = context(Arquillian.class);
			if (arquillian != null) {
				Set<String> containerQualifiers = getContainerQualifiers(arquillian);
				Set<String> groupQualifiers = getGroupQualifiers(arquillian);
				for (String qualifier:groupQualifiers) {
					if (containerQualifiers.contains(qualifier)) {
						return Status.createErrorStatus(message.text());
					}
				}
			}	
		}
		return Status.createOkStatus();
	}

	private Set<String> getGroupQualifiers(Arquillian arquillian) {
		Set<String> qualifiers = new HashSet<String>();
		Property containerProperty = arquillian.property("Group");
		if (containerProperty instanceof ElementList) {
			@SuppressWarnings("unchecked")
			ElementList<Element> elements = (ElementList<Element>) containerProperty;
			for (Element element : elements) {
				SortedSet<Property> properties = element.content();
				for (Property property:properties) {
					if (property.element() instanceof Group) {
						Group g = (Group) property.element();
						Value<String> qualifierValue = g.getQualifier();
						if (qualifierValue != null) {
							String qualifer = qualifierValue.content();
							if (qualifer != null && !qualifer.isEmpty()) {
								qualifiers.add(qualifer);
							}
						}
					}
				}
			}
		}
		return qualifiers;
	}

	private Set<String> getContainerQualifiers(Arquillian arquillian) {
		Set<String> qualifiers = new HashSet<String>();
		Property containerProperty = arquillian.property("Container");
		if (containerProperty instanceof ElementList) {
			@SuppressWarnings("unchecked")
			ElementList<Element> elements = (ElementList<Element>) containerProperty;
			for (Element element : elements) {
				SortedSet<Property> properties = element.content();
				for (Property property:properties) {
					if (property.element() instanceof Container) {
						Container c = (Container) property.element();
						Value<String> qualifierValue = c.getQualifier();
						if (qualifierValue != null) {
							String qualifer = qualifierValue.content();
							if (qualifer != null && !qualifer.isEmpty()) {
								qualifiers.add(qualifer);
							}
						}
					}
				}
			}
		}
		return qualifiers;
	}

}
