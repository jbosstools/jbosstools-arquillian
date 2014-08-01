/*******************************************************************************
 * Copyright (c) 2013-2014 JBoss by Red Hat and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.arquillian.core.internal.archives;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author snjeza
 * 
 */
public class Archive implements IEntry {
	
	public static final String ARCHIVE_ASSET = "ArchiveAsset"; //$NON-NLS-1$
	public static final String DIRECTORY = "Directory"; //$NON-NLS-1$
	public static final String PATH_SEPARATOR = "/"; //$NON-NLS-1$
	
	private static SAXParser parser;
	private String description;
	private ArchiveLocation location;
	private Set<IEntry> entries;
	private String name;
	private Set<String> fullyQuallifiedNames = new HashSet<String>();
	private IJavaProject javaProject;
	
	public Archive(String description, ArchiveLocation location, IJavaProject javaProject) {
		this.description = description;
		this.location = location;
		this.javaProject = javaProject;		
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArchiveLocation getLocation() {
		return location;
	}

	public void setLocation(ArchiveLocation location) {
		this.location = location;
	}

	private SAXParser getParser() throws ParserConfigurationException, SAXException {
		if (parser == null) {
			SAXParserFactory parserFactory = ArquillianCoreActivator.getDefault().getFactory();
			if (parserFactory == null) {
				return null;
			}
			parser = parserFactory.newSAXParser();
			final XMLReader reader = parser.getXMLReader();
			try {
				reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
				reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
			} catch (SAXNotRecognizedException e) {
				// ignore
			} catch (SAXNotSupportedException e) {
				// ignore
			}
		}
		return parser;
	}
	
	public Set<IEntry> getChildren() {
		if (entries == null) {
			try {
				entries = new LinkedHashSet<IEntry>();
				if (description != null && getParser() != null) {
					getParser().parse(new InputSource(new StringReader(description)), new Handler());
				}
			} catch (ParserConfigurationException e) {
				ArquillianCoreActivator.logWarning(e.getLocalizedMessage());
			} catch (SAXException e) {
				ArquillianCoreActivator.logWarning(e.getLocalizedMessage());
			} catch (IOException e) {
				ArquillianCoreActivator.logWarning(e.getLocalizedMessage());
			}
		}
		return entries;
	}

	@Override
	public String getName() {
		return location.getMethodName() + "(" + name + ")";  //$NON-NLS-1$//$NON-NLS-2$
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public IEntry getParent() {
		return null;
	}

	@Override
	public void add(IEntry entry) {
		getChildren().add(entry);
	}
	
	public Set<String> getFullyQuallifiedNames() {
		return fullyQuallifiedNames;
	}

	@Override
	public String getType() {
		return ""; //$NON-NLS-1$
	}
	
	@Override
	public boolean isDirectory() {
		return true;
	}
	
	@Override
	public IJavaProject getJavaProject() {
		return javaProject;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((location == null) ? 0 : location.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Archive other = (Archive) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Archive [location=" + location + ", name=" + name  //$NON-NLS-1$//$NON-NLS-2$
				+ ", description=" + description + ", fullyQuallifiedNames="  //$NON-NLS-1$//$NON-NLS-2$
				+ fullyQuallifiedNames + ", entries=" + entries + "]";  //$NON-NLS-1$//$NON-NLS-2$
	}

	private class Handler extends DefaultHandler {

		private static final String WEB_INF_CLASSES = ".WEB-INF.classes."; //$NON-NLS-1$
		private static final String EMPTY_STRING = ""; //$NON-NLS-1$
		private static final String PERIOD = "."; //$NON-NLS-1$
		private static final String DOT_CLASS = ".class"; //$NON-NLS-1$
		private static final String CLASS_LOADER_ASSET = "ClassLoaderAsset"; //$NON-NLS-1$
		private static final String CLASS_ASSET = "ClassAsset"; //$NON-NLS-1$
		private static final String SOURCE = "source"; //$NON-NLS-1$
		private static final String PATH = "path"; //$NON-NLS-1$
		private static final String TYPE = "type"; //$NON-NLS-1$
		private static final String ASSET = "asset"; //$NON-NLS-1$
		private static final String NAME = "name"; //$NON-NLS-1$
		private static final String DEPLOYMENT = "deployment"; //$NON-NLS-1$
		private IEntry currentEntry;
		private IEntry currentArchive;
		private Map<String,IEntry> entriesByPath = new HashMap<String, IEntry>();
		private Map<String,IEntry> archiveEntriesByPath;
		
		private boolean inArchive = false;
		@Override
		public void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
			if (DEPLOYMENT.equals(elementName)) {
				name = attributes.getValue(NAME);
			} else if (ASSET.equals(elementName)) {
				if (currentArchive != null) {
					inArchive = true;
				}
				String type = attributes.getValue(TYPE);
				String path = attributes.getValue(PATH);
				if (DIRECTORY.equals(type) && PATH_SEPARATOR.equals(path)) {
					currentEntry = null;
					return;
				}
				String source = attributes.getValue(SOURCE);
				if (path != null) {
					String fqn = null;
					if ( (CLASS_ASSET.equals(type)
							|| CLASS_LOADER_ASSET.equals(type)) && path.endsWith(DOT_CLASS)) {
						fqn = path.replace(PATH_SEPARATOR, PERIOD);
						if (!fqn.isEmpty()) {
							if (fqn.startsWith(WEB_INF_CLASSES)) {
								fqn = fqn.substring(WEB_INF_CLASSES.length());
							}
							fqn = fqn.replace(DOT_CLASS, EMPTY_STRING);
							if (fqn.startsWith(PERIOD)) {
								fqn = fqn.substring(1);
							}
							fullyQuallifiedNames.add(fqn);
						}
					}
					IPath parentPath = new Path(path);
					parentPath = parentPath.removeLastSegments(1);
					IEntry parent;
					if (currentArchive != null) {
						parent = archiveEntriesByPath.get(parentPath.toString());
					} else {
						parent = entriesByPath.get(parentPath.toString());
					}
					if (parent == null) {
						ArquillianCoreActivator.logWarning("Invalid parent: path=" + path); //$NON-NLS-1$
						parent = Archive.this;
					}

					currentEntry = new Entry(parent, type, path, source, javaProject, fqn);
					parent.add(currentEntry);
					if (ARCHIVE_ASSET.equals(type)) {
						currentArchive = currentEntry;
						archiveEntriesByPath =  new HashMap<String, IEntry>();
						archiveEntriesByPath.put(PATH_SEPARATOR, currentArchive);
					}
					if (DIRECTORY.equals(type)) {
						if (inArchive) {
							archiveEntriesByPath.put(path, currentEntry);
						} else {
							entriesByPath.put(path, currentEntry);
						}
					}
				}
			}
			
		}

		@Override
		public void startDocument() throws SAXException {
			entriesByPath.put(PATH_SEPARATOR, Archive.this);
			currentArchive = null;
			archiveEntriesByPath = null;
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (ASSET.equals(localName)) {
				if (inArchive) {
					inArchive = false;
				} else {
					currentArchive = null;
					archiveEntriesByPath = null;
				}
			}
		}
		
	}

}
