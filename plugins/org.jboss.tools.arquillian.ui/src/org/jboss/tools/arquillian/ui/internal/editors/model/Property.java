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
package org.jboss.tools.arquillian.ui.internal.editors.model;

import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.arquillian.core.internal.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The model class of property element.
 *
 */
public class Property {

	private String name;
	private String value;
	private String type;
	
	private static final Map<String, Validator> validators;
	private static final Validator NUMBER_VALIDATOR = new Validator() {
		@Override
		public void validate(String value) throws Exception {
			if(StringUtils.isNotEmpty(value)){
				try {
					Integer.valueOf(value);
				} catch(NumberFormatException e) {
					throw new Exception("The value should input number.", e); //$NON-NLS-1$
				}
			}
		}
	};
	
	static {
		validators = new HashMap<String, Validator>();
		validators.put(Integer.class.getCanonicalName(), NUMBER_VALIDATOR);
		validators.put(Long.class.getCanonicalName(), NUMBER_VALIDATOR);
		validators.put(Boolean.class.getCanonicalName(), new Validator() {
			@Override
			public void validate(String value) throws Exception {
				if(StringUtils.isNotEmpty(value)){
					if(!value.equals(Boolean.TRUE.toString().toLowerCase())
							&& !value.equals(Boolean.FALSE.toString().toLowerCase())) {
						throw new Exception("The value should input true or false."); //$NON-NLS-1$
					}
				}
			}
		});
		validators.put(Float.class.getCanonicalName(), new Validator() {
			@Override
			public void validate(String value) throws Exception {
				if(StringUtils.isNotEmpty(value)){
					try {
						Float.valueOf(value);
					} catch (NumberFormatException e) {
						throw new Exception("The value should input decimal number.", e); //$NON-NLS-1$
					}
				}
			}
		});
	}
	
	public Property(Element element) {
		name = element.getAttribute(ArquillianXmlElement.ATTR_NAME);
		if(element.hasChildNodes()) {
			value = element.getFirstChild().getNodeValue();
		}
	}
	
	public Property(Property property) {
		this(property.name, property.value, property.type);
	}
	
	public Property(String name, String value) {
		this(name, value, null);
	}
	
	public Property(String name, String value, String type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}
	
	public String getLabel(int index) {
		switch (index) {
		case 0:
			return name;
		case 1:
			return value;
		default:
			return null;
		}
	}
	
	public void validate(String value) throws Exception {
		if(type != null && validators.containsKey(type)) {
			validators.get(type).validate(value);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	interface Validator {
		void validate(String value) throws Exception;
	}
	
}
