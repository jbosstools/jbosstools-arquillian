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
package org.jboss.tools.arquillian.ui.internal.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility.GenStubSettings;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.jboss.forge.arquillian.container.Container;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.launcher.ArquillianProperty;
import org.jboss.tools.arquillian.ui.internal.launcher.AutoResizeTableLayout;
import org.jboss.tools.arquillian.ui.internal.preferences.ContainerEditingSupport;
import org.jboss.tools.arquillian.ui.internal.wizards.NewArquillianJUnitTestCaseDeploymentPage;
import org.jboss.tools.arquillian.ui.internal.wizards.ProjectResource;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianUIUtil {

	private static final String UNKNOWN = "Unknown"; //$NON-NLS-1$
	private static final String CONFIGURATION = "configuration"; //$NON-NLS-1$
	private static final String ARQ_PREFIX = "arq"; //$NON-NLS-1$
	private static final String CONTAINER = "container"; //$NON-NLS-1$
	private static final String PERIOD = "."; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String PROPERTY = "property"; //$NON-NLS-1$
	private static final String QUALIFIER = "qualifier"; //$NON-NLS-1$
	private static final String ARQUILLIAN_XML = "arquillian.xml"; //$NON-NLS-1$
	private static final String ARQUILLIAN_PROPERTIES = "arquillian.properties"; //$NON-NLS-1$
	private static final String ARQUILLIAN_LAUNCH = "arquillian.launch"; //$NON-NLS-1$
	private static final String CREATE_DEPLOYMENT = "createDeployment"; //$NON-NLS-1$
	private static final String ORG_JBOSS_SHRINKWRAP_API_ARCHIVE = "org.jboss.shrinkwrap.api.Archive"; //$NON-NLS-1$
	private static final String ORG_JBOSS_SHRINKWRAP_API_SHRINK_WRAP = "org.jboss.shrinkwrap.api.ShrinkWrap"; //$NON-NLS-1$
	public static final String ADD_AS_MANIFEST_RESOURCE_METHOD = "addAsManifestResource"; //$NON-NLS-1$
	public static final String ADD_AS_WEB_INF_RESOURCE_METHOD = "addAsWebInfResource"; //$NON-NLS-1$
	public static final String ADD_AS_RESOURCE_METHOD = "addAsResource"; //$NON-NLS-1$

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench = ArquillianUIActivator.getDefault()
				.getWorkbench();
		if (workbench == null)
			return null;
		return workbench.getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return null;
		return activeWorkbenchWindow.getActivePage();
	}

	public static IType getActiveType() {
		IWorkbenchPage page = getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart editor = page.getActiveEditor();
		if (editor instanceof CompilationUnitEditor) {
			CompilationUnitEditor cue = (CompilationUnitEditor) editor;
			try {
				IJavaElement element = SelectionConverter.getElementAtOffset(cue);
				if (element == null) {
					return null;
				}
				return SelectionConverter.getTypeAtOffset(cue);
			} catch (JavaModelException e) {
				ArquillianUIActivator.log(e);
			}
		}
		return null;
	}

//	public static boolean isArquillianJUnitTestCase(IType type) {
//		if (type == null) {
//			return false;
//		}
//		IAnnotation annotation = type.getAnnotation("RunWith");
//		if (annotation != null && annotation.exists()) {
//			IMemberValuePair[] pairs = null;
//			try {
//				pairs = annotation.getMemberValuePairs();
//			} catch (JavaModelException e) {
//				ArquillianUIActivator.log(e);
//			}
//			if (pairs != null) {
//				for (IMemberValuePair pair : pairs) {
//					if ("value".equals(pair.getMemberName())
//							&& "Arquillian".equals(pair.getValue())) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}

	public static void createDeploymentMethod(ICompilationUnit icu, IType type,
			ImportsManager imports, boolean isAddComments, String delimiter,
			NewArquillianJUnitTestCaseDeploymentPage deploymentPage,
			IJavaElement sibling, boolean force) throws CoreException {
		String content = null;
		ImportRewrite importsRewrite = null;
		if (icu != null) {
			importsRewrite = StubUtility.createImportRewrite(icu, true);
		}
		String annotation = '@' + addImport(imports, importsRewrite,
				ArquillianUtility.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT);
		String methodName = CREATE_DEPLOYMENT;
		addImport(imports, importsRewrite,
				ORG_JBOSS_SHRINKWRAP_API_SHRINK_WRAP);
		addImport(imports, importsRewrite, ORG_JBOSS_SHRINKWRAP_API_ARCHIVE);
		GenStubSettings settings = JUnitStubUtility
				.getCodeGenerationSettings(type.getJavaProject());
		settings.createComments = isAddComments;

		StringBuffer buffer = new StringBuffer();
		if (settings.createComments) {
			String retTypeSig = Signature.createTypeSignature(
					ORG_JBOSS_SHRINKWRAP_API_ARCHIVE, true); //$NON-NLS-1$
			String comment = CodeGeneration.getMethodComment(
					type.getCompilationUnit(), type.getElementName(),
					methodName, new String[0], new String[0], retTypeSig, null,
					delimiter);
			if (comment != null) {
				buffer.append(comment);
			}
		}

		String archiveType = ArquillianUIActivator.JAR;
		String archiveName = "test";
		String deploymentName = null;
		String deploymentOrder = null;
		boolean addBeansXml = true;
		IType[] types = null;

		List<String> resources = new ArrayList<String>();
		List<String> webInfResources = new ArrayList<String>();
		if (deploymentPage != null) {
			methodName = deploymentPage.getMethodName();
			archiveType = deploymentPage.getArchiveType();
			archiveName = deploymentPage.getArchiveName();
			addBeansXml = deploymentPage.addBeansXml();
			deploymentName = deploymentPage.getDeploymentName();
			deploymentOrder = deploymentPage.getDeploymentOrder();
			types = deploymentPage.getTypes();
			ProjectResource[] allResources = deploymentPage.getResources();
			for (ProjectResource resource : allResources) {
				if (ArquillianUIActivator.WAR.equals(archiveType)
						&& resource.isDeployAsWebInfResource()) {
					webInfResources.add(resource.getPath().toString());
				} else {
					resources.add(resource.getPath().toString());
				}
			}
		}

		buffer.append(annotation);
		if ((deploymentName != null && !deploymentName.isEmpty())
				|| (deploymentOrder != null && !deploymentOrder.isEmpty())) {
			buffer.append("(");
			if ((deploymentName != null && !deploymentName.isEmpty())) {
				buffer.append("name = \"");
				buffer.append(deploymentName);
				buffer.append("\"");
				if (deploymentOrder != null && !deploymentOrder.isEmpty()) {
					buffer.append(" , ");
				}
			}
			if (deploymentOrder != null && !deploymentOrder.isEmpty()) {
				buffer.append("order = ");
				buffer.append(deploymentOrder);
			}

			buffer.append(")");
		}
		buffer.append(delimiter);

		buffer.append("public static Archive<?> "); //$NON-NLS-1$

		buffer.append(methodName);
		buffer.append("()");
		buffer.append(" {").append(delimiter);
		if (ArquillianUIActivator.JAR.equals(archiveType)) {
			addImport(imports, importsRewrite,
					ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_JAVA_ARCHIVE);
			buffer.append("JavaArchive archive = ShrinkWrap.create(JavaArchive.class");
		}
		if (ArquillianUIActivator.WAR.equals(archiveType)) {
			addImport(imports, importsRewrite,
					ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_WEB_ARCHIVE);
			buffer.append("WebArchive archive = ShrinkWrap.create(WebArchive.class");
		}
		if (ArquillianUIActivator.EAR.equals(archiveType)) {
			addImport(imports, importsRewrite,
					"org.jboss.shrinkwrap.api.spec.EnterpriseArchive");
			buffer.append("EnterpriseArchive archive = ShrinkWrap.create(EnterpriseArchive.class");
		}
		if (archiveName != null && !archiveName.isEmpty()) {
			if (archiveName.indexOf(PERIOD) == -1) {
				archiveName = archiveName + PERIOD + archiveType;
			}
			buffer.append(", ");
			buffer.append("\"");
			buffer.append(archiveName);
			buffer.append("\"");
		}
		buffer.append(")");

		if (types != null && types.length > 0) {
			buffer.append(delimiter);
			buffer.append(".addClasses( ");
			boolean first = true;
			for (IType t : types) {
				if (!first) {
					buffer.append(" , ");
				} else {
					first = false;
				}
				String typeName = t.getFullyQualifiedName();
				int lastPeriod = typeName.lastIndexOf(PERIOD);
				String className = typeName;
				if (lastPeriod >= 0 && lastPeriod < typeName.length()) {
					className = typeName.substring(lastPeriod + 1,
							typeName.length());
					addImport(imports, importsRewrite, typeName);
				}
				buffer.append(className);
				buffer.append(".class");
			}
			buffer.append(" )");
		}

		for (String resource : resources) {
			buffer.append(delimiter);
			buffer.append(".addAsResource( ");
			buffer.append("\"");
			buffer.append(resource);
			buffer.append("\"");
			buffer.append(" )");
		}
		for (String resource : webInfResources) {
			buffer.append(delimiter);
			buffer.append(".addAsWebInfResource( ");
			buffer.append("\"");
			buffer.append(resource);
			buffer.append("\"");
			buffer.append(" )");
		}

		if (addBeansXml) {
			addImport(imports, importsRewrite,
					"org.jboss.shrinkwrap.api.asset.EmptyAsset");
			buffer.append(delimiter);
			buffer.append(".addAsManifestResource(EmptyAsset.INSTANCE, \"beans.xml\")");
		}

		buffer.append(";").append(delimiter);
		buffer.append("// System.out.println(archive.toString(true));").append(
				delimiter);
		buffer.append("return archive;").append(delimiter);
		buffer.append("}"); //$NON-NLS-1$
		buffer.append(delimiter);
		content = buffer.toString();

		IMethod createdMethod = type
				.createMethod(content, sibling, force, null);

		if (icu != null) {
			TextEdit edit = importsRewrite.rewriteImports(null);
			JavaModelUtil.applyEdit(importsRewrite.getCompilationUnit(), edit,
					false, null);
			ISourceRange range = createdMethod.getSourceRange();

			IBuffer buf = icu.getBuffer();
			String originalContent = buf.getText(range.getOffset(),
					range.getLength());
			int indent = StubUtility.getIndentUsed(type) + 1;
			String formattedContent = CodeFormatterUtil.format(
					CodeFormatter.K_CLASS_BODY_DECLARATIONS, originalContent,
					indent, delimiter, type.getJavaProject());
			formattedContent = Strings
					.trimLeadingTabsAndSpaces(formattedContent);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);

			icu.reconcile(ICompilationUnit.NO_AST, false, null, null);

			icu.commitWorkingCopy(false, null);

		}
	}

	private static String addImport(ImportsManager imports,
			ImportRewrite importsRewrite, String qtn) {
		if (imports != null) {
			return imports.addImport(qtn);
		}
		if (importsRewrite != null) {
			return importsRewrite.addImport(qtn);
		}
		return null;
	}

	public static ICompilationUnit getActiveCompilationUnit() {
		IWorkbenchPage page = getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart editor = page.getActiveEditor();
		if (editor instanceof CompilationUnitEditor) {
			CompilationUnitEditor cue = (CompilationUnitEditor) editor;
			return SelectionConverter.getInputAsCompilationUnit(cue);
		}
		return null;
	}
	
	public static Set<ArquillianProperty> getArquillianProperties(ILaunchConfiguration configuration) {
		Set<ArquillianProperty> properties = new TreeSet<ArquillianProperty>();
		try {
			IJavaProject javaProject = ArquillianUtility.getJavaProject(configuration);
			if (javaProject == null) {
				return properties;
			}
			if (!ArquillianSearchEngine.hasArquillianType(javaProject)) {
				return properties;
			}
			String arguments = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
			String args = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(arguments);
			Properties vmProperties = getVMProperties(args);
			String configurationName = vmProperties.getProperty(ARQUILLIAN_LAUNCH);
			
			Properties arquillianXmlProperties = null;
			try {
				arquillianXmlProperties = getArquillianXmlProperties(javaProject, configurationName);
			} catch (Exception e) {
				ArquillianUIActivator.log(e);
			}
			String qualifier = arquillianXmlProperties.getProperty(QUALIFIER, null);
			if (qualifier == null) {
				qualifier = UNKNOWN;
				IFile file = getFile(javaProject, ARQUILLIAN_XML);
				if (file == null) {
					file = getNewFile(javaProject, ARQUILLIAN_XML);
					String s = "<arquillian xmlns=\"http://jboss.org/schema/arquillian\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" //$NON-NLS-1$
		                    + "            xsi:schemaLocation=\"http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd\">\n" //$NON-NLS-1$
		                    + "</arquillian>"; //$NON-NLS-1$
					file.create(new ByteArrayInputStream(s.getBytes()), true,null);
				}
//				InputStream is = null;
//				try {
//					is = file.getContents();
//					org.jboss.forge.parser.xml.Node xml = XMLParser.parse(is);
//					
//					xml.getOrCreate("container@qualifier=" + qualifier + "&default=true"); //$NON-NLS-1$ //$NON-NLS-2$
//						
//					String content = XMLParser.toXMLString(xml);
//					ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
//					file.setContents(input, IResource.FORCE, null);
//				} catch (XMLParserException e) {
//					ArquillianCoreActivator.log(e);
//				} finally {
//					close(is);
//				}
			}
			arquillianXmlProperties.remove(QUALIFIER);
			addContainerProperties(javaProject, properties, qualifier);
			
			Enumeration<Object> keys = arquillianXmlProperties.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = arquillianXmlProperties.getProperty(key);
				ArquillianProperty property = new ArquillianProperty(key, value, ARQUILLIAN_XML, false);
				properties.remove(property);;
				properties.add(property);
			}
			String fileName = vmProperties.getProperty(ARQUILLIAN_PROPERTIES, ARQUILLIAN_PROPERTIES);
			Properties arquillianProperties = getArquillianProperties(javaProject, fileName);
			keys = arquillianProperties.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = arquillianProperties.getProperty(key);
				ArquillianProperty property = new ArquillianProperty(key, value, ARQUILLIAN_PROPERTIES, false);
				properties.remove(property);;
				properties.add(property);
			}
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
		}
		return properties;
	}

	private static void addContainerProperties(IJavaProject javaProject,
			Set<ArquillianProperty> properties, String qualifier) {
		IStatus status = ArquillianSearchEngine.validateDeployableContainer(javaProject);
		if (!status.isOK()) {
			ArquillianUIActivator.getDefault().getLog().log(status);
			return;
		}
		try {
			IType type = javaProject.findType(ArquillianSearchEngine.CONTAINER_DEPLOYABLE_CONTAINER);
			if (type == null) {
				status = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, "Cannot find 'org.jboss.arquillian.container.spi.client.container.DeployableContainer' on project build path. Arquillian tests can only be run if DeployableContainer is on the build path.");
				ArquillianUIActivator.getDefault().getLog().log(status);
			}
			ITypeHierarchy hierarchy = type.newTypeHierarchy(new NullProgressMonitor());
            IType[] subTypes = hierarchy.getAllSubtypes(type);
            for (IType subType:subTypes) {
            	if (ArquillianSearchEngine.isNonAbstractClass(subType)) {
            		Object containerObject = null;
            		try {
						containerObject = ArquillianUtility.newInstance(javaProject, subType.getFullyQualifiedName());
					} catch (Exception e) {
						ArquillianUIActivator.log(e);
						return;
					}
            		
            		ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
            		
            		ClassLoader loader = ArquillianCoreActivator.getDefault().getClassLoader(javaProject);
            		
            		Thread.currentThread().setContextClassLoader(loader);
            		try {
						Class<?> clazz = containerObject.getClass();
						Method configurationMethod = clazz.getDeclaredMethod("getConfigurationClass", //$NON-NLS-1$
						        new Class[0]);
						Class<?> configuration =  (Class<?>) configurationMethod.invoke(containerObject, new Object[0]);
						
						Object configurationObject = ArquillianUtility.newInstance(javaProject, configuration.getName());
						Method[] methods = configuration.getMethods();
						for (Method method : methods) {
							String methodName = method.getName();
							if (methodName.matches("^set[A-Z].*") //$NON-NLS-1$
									&& method.getReturnType().equals(Void.TYPE)
									&& method.getParameterTypes().length == 1) {
								method.setAccessible(true);
								String name = methodName.substring(3, 4)
										.toLowerCase() + methodName.substring(4);
								String getterName = "get" + methodName.substring(3); //$NON-NLS-1$
								String value = null;
								try {
									Method getter =  configuration.getMethod(getterName, new Class[0]);
									Object valueObject = getter.invoke(configurationObject, new Object[0]);
									if (valueObject != null) {
										value = valueObject.toString();
									}
								} catch (Exception e) {
									//ArquillianUIActivator.log(e);
									// FIXME
								}
								if (value == null) {
									value = ""; 
								}
								String propertyName = getContainerConfigurationPropertyName(
										qualifier, name);
								ArquillianProperty property = new ArquillianProperty(
										propertyName, value, CONTAINER, true);
								
								properties.add(property);
							}
						}
					} catch (Exception e) {
						ArquillianUIActivator.log(e);
					} finally {
						Thread.currentThread().setContextClassLoader(currentLoader);
					}
            		break;
            	}
            }
            
		} catch (JavaModelException e) {
			ArquillianUIActivator.log(e);
		}
		
	}

	private static Properties getArquillianProperties(IJavaProject javaProject, String fileName) throws JavaModelException {
		IFile file = getFile(javaProject, fileName);
		Properties properties = new Properties();
		if  (file != null && file.exists()) {
			InputStream input = null;
			
			try {
				input = file.getContents();
				properties.load(input);
			} catch (CoreException e) {
				ArquillianUIActivator.log(e);
			} catch (IOException e) {
				ArquillianUIActivator.log(e);
			} finally {
				close(input);
			}
		}
		return properties;
	}

	private static Properties getArquillianXmlProperties(
			IJavaProject javaProject, String configurationName) throws CoreException, ParserConfigurationException, SAXException, IOException {
		Properties properties = new Properties();
		IFile file = getFile(javaProject, ARQUILLIAN_XML);
		if (file != null && file.exists()) {
			InputStream input = null;
			try {
				input = file.getContents(true);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(input);
				Element root = doc.getDocumentElement();
				Element container = getDefaultContainer(configurationName, root);
				if (container != null) {
					String qualifier = container.getAttribute(QUALIFIER);
					properties.put(QUALIFIER, qualifier);
					Element configuration = getConfiguration(container);
					if (configuration != null) {
						NodeList configurationList = configuration.getChildNodes();
						for (int i = 0; i < configurationList.getLength(); i++) {
							Node node = configurationList.item(i);
							if ( (node instanceof Element) && PROPERTY.equals(node.getNodeName())) {
								Element property = (Element) node;
								String name = property.getAttribute(NAME);
								String propertyName = getContainerConfigurationPropertyName(
										qualifier, name);
								String value = property.getTextContent();
								if (value != null) {
									value = value.trim();
									properties.put(propertyName, value);
								}
							}
						}
					}
				}
			} catch (DOMException e) {
				ArquillianUIActivator.log(e);
			} finally {
				close(input);
			}
			
		}
		return properties;
	}

	private static String getContainerConfigurationPropertyName(
			String qualifier, String name) {
		StringBuffer buf = new StringBuffer();
		buf.append(ARQ_PREFIX);
		buf.append(PERIOD);
		buf.append(CONTAINER);
		buf.append(PERIOD);
		if (!UNKNOWN.equals(qualifier)) {
			buf.append(qualifier);
			buf.append(PERIOD);
		}
		buf.append( CONFIGURATION);
		buf.append(PERIOD);
		buf.append(name);
		return buf.toString();
	}

	private static IFile getFile(IJavaProject javaProject, String fileName) throws JavaModelException {
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		for (IClasspathEntry entry : rawClasspath) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPackageFragmentRoot[] roots = javaProject
						.findPackageFragmentRoots(entry);
				if (roots == null) {
					continue;
				}
				for (IPackageFragmentRoot root : roots) {
					Object[] resources = root.getNonJavaResources();
					int segments = root.getPath().segmentCount();
					for (Object resource:resources) {
						if (resource instanceof IFile) {
							IFile file = (IFile) resource;
							IPath filePath = file.getProjectRelativePath();
							IPath relativePath = filePath.removeFirstSegments(segments-1);
							if (fileName.equals(relativePath.toString())) {
								return file;
							}
						}
					}
					
				}
			}
		}
		return null;
	}

	private static void close(InputStream input) {
		if (input != null) {
			try {
				input.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	private static void close(OutputStream output) {
		if (output != null) {
			try {
				output.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private static Element getConfiguration(Element container) {
		Element configuration = null;
		NodeList containerList = container.getChildNodes();
		for (int i = 0; i < containerList.getLength(); i++) {
			Node node = containerList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (CONFIGURATION.equals(element.getNodeName()) ) {
					configuration = element;
					break;
				}
			}
		}
		return configuration;
	}

	private static Element getDefaultContainer(String configurationName,
			Element root) {
		NodeList containers = root.getElementsByTagName(CONTAINER);
		for (int i = 0; i < containers.getLength(); i++) {
			Node containerNode = containers.item(i);
			if (containerNode instanceof Element) {
				Element container = (Element) containerNode;
				if (configurationName != null && !configurationName.isEmpty()) {
					String qualifier = container.getAttribute(QUALIFIER);
					if (configurationName.equals(qualifier)) {
						return container;
					}
				} else {
					String defaultString = container.getAttribute("default"); //$NON-NLS-1$
					if ("true".equals(defaultString)) { //$NON-NLS-1$
						return container;
					}
				}
			}
		}
		return null;
	}

	private static Properties getVMProperties(String vmArgs) {
		Properties properties = new Properties();
		if (vmArgs != null) {
			String[] arguments = vmArgs.split(" ");
			if (arguments != null) {
				for (String arg:arguments) {
					arg = arg.trim();
					if (arg.startsWith("-Darq")) {
						String[] props = arg.split("=");
						if (props != null && props.length == 2) {
							String name = props[0].substring(2);
							String value = props[1];
							properties.put(name, value);
						}
					}
				}
			}
		}
		return properties;
	}

	public static void save(Set<ArquillianProperty> arquillianProperties, ILaunchConfiguration configuration) throws CoreException {
		if (arquillianProperties == null || configuration == null) {
			return;
		}
		IJavaProject javaProject = ArquillianUtility.getJavaProject(configuration);
		if (javaProject == null) {
			return;
		}
		IFile file = getFile(javaProject, ARQUILLIAN_PROPERTIES);
		if (file == null) {
			file = getNewFile(javaProject, ARQUILLIAN_PROPERTIES);
		}
		if (!file.exists()) {
			createEmptyFile(file);
		}
		InputStream input = null;
		Properties properties = new Properties();
		input = file.getContents();
		try {
			properties.load(input);
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, ArquillianUIActivator.PLUGIN_ID, e.getMessage(), e));
		} finally {
			if (input != null) {
				close(input);
			}
		}
		boolean changed = false;
		for (ArquillianProperty arquillianProperty:arquillianProperties) {
			// FIXME
			if (arquillianProperty.isChanged() && ARQUILLIAN_PROPERTIES.equals(arquillianProperty.getSource())) {
				properties.put(arquillianProperty.getName(), arquillianProperty.getValue());
				changed = true;
			}
		}
		if (changed) {
			ByteArrayOutputStream out = null;
			ByteArrayInputStream in = null;
			
			try {
				out = new ByteArrayOutputStream();
				properties.store(out, "Created by JBoss Tools");
				String outputString = out.toString();
				in = new ByteArrayInputStream(out.toByteArray());
				file.setContents(in, true, true, null);
			} catch (IOException e) {
				throw new CoreException(new Status(Status.ERROR, ArquillianUIActivator.PLUGIN_ID, e.getMessage(), e));
			} finally {
				close(in);
				close(out);
			}
		}
	}

	private static IFile getNewFile(IJavaProject javaProject,
			String arquillianProperties) throws CoreException {
		IPath path = null;
		IProject project = javaProject.getProject();
		if (project.hasNature(IMavenConstants.NATURE_ID)) {
			IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
			MavenProject mavenProject = MavenPlugin.getMaven().readProject(
					pomFile.getLocation().toFile(), new NullProgressMonitor());
			Build build = mavenProject.getBuild();
			String testDirectory = null;
			List<Resource> testResources = build.getTestResources();
			if (testResources != null && testResources.size() > 0) {
				testDirectory = testResources.get(0).getDirectory();
			} else {
				testDirectory = build.getTestSourceDirectory();
			}
			path = Path.fromOSString(testDirectory);
			IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot()
					.getRawLocation();
			path = path.makeRelativeTo(workspacePath).makeAbsolute();
			IPath projectPath = javaProject.getPath();
			path = path.makeRelativeTo(projectPath);
			IFolder folder = javaProject.getProject().getFolder(path);
			if (!folder.exists()) {
				path = null;
			}
		}
		if (path == null) {
			IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
			for (IClasspathEntry entry : rawClasspath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPackageFragmentRoot[] roots = javaProject
							.findPackageFragmentRoots(entry);
					if (roots == null) {
						continue;
					}
					for (IPackageFragmentRoot root : roots) {
						path = root.getPath();
						break;
					}
				}
			}
		}
		if (path == null) {
			throw new CoreException(new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, "Invalid project"));
		}
		IFolder folder = javaProject.getProject().getFolder(path);
		return folder.getFile(arquillianProperties);
	}

    
	private static void createEmptyFile(IFile file) throws CoreException {
		InputStream input = null;
		try {
			input = new ByteArrayInputStream("".getBytes()); //$NON-NLS-1$
			file.create(input, true, null);
		} catch (Exception e) {
			throw new CoreException(new Status(Status.ERROR, ArquillianUIActivator.PLUGIN_ID, e.getMessage(), e));
		} finally {
			if (input != null) {
				close(input);
			}
		}
	}
	
	public static CheckboxTableViewer createProfilesViewer(Composite parent, List<Container> containers, int heightHint) {
		final CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.FILL,true,false);
		gd.heightHint = heightHint;
		viewer.getTable().setLayoutData(gd);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());
		
		viewer.setContentProvider(new ContainerContentProvider(containers));
		
		String[] columnHeaders = {"ID", "Name"};
		
		for (int i = 0; i < columnHeaders.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new ContainerLabelProvider(i));
			column.getColumn().setText(columnHeaders[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.setEditingSupport(new ContainerEditingSupport(viewer, i));
		}
		
		ColumnLayoutData[] containersLayouts= {
				new ColumnWeightData(200,200),
				new ColumnWeightData(150,150),
				//new ColumnWeightData(50,50)
			};
		
		TableLayout layout = new AutoResizeTableLayout(table);
		for (int i = 0; i < containersLayouts.length; i++) {
			layout.addColumnData(containersLayouts[i]);
		}
		
		viewer.getTable().setLayout(layout);
		
		configureViewer(viewer);
		
		viewer.setInput(containers);
		
		return viewer;
	}
	
	public static void initializeViewer(CheckboxTableViewer viewer, List<Container> containers) {
		List<String> selectedProfiles = ArquillianUtility.getProfilesFromPreferences(ArquillianConstants.SELECTED_ARQUILLIAN_PROFILES);
		List<String> activatedProfiles = ArquillianUtility.getProfilesFromPreferences(ArquillianConstants.ACTIVATED_ARQUILLIAN_PROFILES);

		for(Container container:containers) {
			container.setActivate(activatedProfiles.contains(container.getId()));
			viewer.setChecked(container, selectedProfiles.contains(container.getId()));
		}
		viewer.refresh();
	}

	static class ContainerContentProvider implements IStructuredContentProvider {
		private List<Container> containers;

		public ContainerContentProvider(List<Container> containers) {
			this.containers = containers;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			return containers.toArray();
		}

		public void dispose() {
		}
	}
	
	static class ContainerLabelProvider extends ColumnLabelProvider {

		private int columnIndex;

		public ContainerLabelProvider(int i) {
			this.columnIndex = i;
		}

		public String getText(Object element) {
			if (element instanceof Container) {
				Container container = (Container) element;
				switch (columnIndex) {
				case 0:
					return container.getId();
				case 1:
					String name = container.getName();
					if (name == null) {
						return null;
					}
					return name.replace(Container.ARQUILLIAN_CONTAINER_NAME_START,""); //$NON-NLS-1$
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}
	}

	private static void configureViewer(final CheckboxTableViewer viewer) {
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(viewer, new FocusCellOwnerDrawHighlighter(viewer));
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				ViewerCell cell = viewer.getColumnViewerEditor().getFocusCell();
				if (cell != null && cell.getColumnIndex() == 1) {
					return super.isEditorActivationEvent(event);
				}
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION);
	}

}
