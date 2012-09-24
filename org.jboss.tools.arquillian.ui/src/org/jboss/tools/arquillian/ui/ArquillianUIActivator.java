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
package org.jboss.tools.arquillian.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.tools.arquillian.ui.internal.wizards.NewArquillianJUnitTestCaseDeploymentPage;
import org.jboss.tools.arquillian.ui.internal.wizards.ProjectResource;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArquillianUIActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jboss.tools.arquillian.ui"; //$NON-NLS-1$

	public static final String JAR = "jar";
	public static final String WAR = "war";
	public static final String EAR = "ear";

	// The shared instance
	private static ArquillianUIActivator plugin;
	
	/**
	 * The constructor
	 */
	public ArquillianUIActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ArquillianUIActivator getDefault() {
		return plugin;
	}
	
	public static void log(Exception e, String message) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
		plugin.getLog().log(status);
	}

	public static void log(Throwable e) {
		IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, e
				.getLocalizedMessage(), e);
		plugin.getLog().log(status);
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null)
			return null;
		IWorkbench workBench= plugin.getWorkbench();
		if (workBench == null)
			return null;
		return workBench.getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow= getActiveWorkbenchWindow();
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
				return SelectionConverter.getTypeAtOffset(cue);
			} catch (JavaModelException e) {
				log(e);
			}
		}
		return null;
	}
	
	public static boolean isArquillianJUnitTestCase(IType type) {
		if (type == null) {
			return false;
		}
		IAnnotation annotation = type.getAnnotation("RunWith");
		if (annotation != null && annotation.exists()) {
			IMemberValuePair[] pairs = null;
			try {
				pairs = annotation.getMemberValuePairs();
			} catch (JavaModelException e) {
				log(e);
			}
			if (pairs != null) {
				for (IMemberValuePair pair:pairs) {
					if ("value".equals(pair.getMemberName()) && "Arquillian".equals(pair.getValue())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static void createDeploymentMethod(ICompilationUnit icu, IType type, ImportsManager imports, 
			boolean isAddComments, String delimiter,
			NewArquillianJUnitTestCaseDeploymentPage deploymentPage,
			IJavaElement sibling, boolean force) throws CoreException {
		String content= null;
		ImportRewrite importsRewrite = null;
		if (icu != null) {
			importsRewrite = StubUtility.createImportRewrite(icu, true);
		}
		String annotation= '@' + addImport(imports, importsRewrite, "org.jboss.arquillian.container.test.api.Deployment");
		String methodName = "createDeployment";
		addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.ShrinkWrap");
		addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.Archive");
		GenStubSettings settings= JUnitStubUtility.getCodeGenerationSettings(type.getJavaProject());
		settings.createComments= isAddComments;

		StringBuffer buffer = new StringBuffer();
		if (settings.createComments) {
			String retTypeSig = Signature.createTypeSignature(
					"org.jboss.shrinkwrap.api.Archive", true); //$NON-NLS-1$
			String comment = CodeGeneration.getMethodComment(
					type.getCompilationUnit(), type.getElementName(),
					methodName, new String[0], new String[0],
					retTypeSig, null, delimiter);
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
			for (ProjectResource resource:allResources) {
				if (ArquillianUIActivator.WAR.equals(archiveType) && resource.isDeployAsWebInfResource()) {
					webInfResources.add(resource.getPath().toString());
				} else {
					resources.add(resource.getPath().toString());
				}
			}
		}
		
		buffer.append(annotation);
		if ( (deploymentName != null && !deploymentName.isEmpty())
				|| (deploymentOrder != null && !deploymentOrder.isEmpty())
				) {
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
			addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.spec.JavaArchive");
			buffer.append("JavaArchive archive = ShrinkWrap.create(JavaArchive.class");
		}
		if (ArquillianUIActivator.WAR.equals(archiveType)) {
			addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.spec.WebArchive");
			buffer.append("WebArchive archive = ShrinkWrap.create(WebArchive.class");
		}
		if (ArquillianUIActivator.EAR.equals(archiveType)) {
			addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.spec.EnterpriseArchive");
			buffer.append("EnterpriseArchive archive = ShrinkWrap.create(EnterpriseArchive.class");
		}
		if (archiveName != null && !archiveName.isEmpty()) {
			if (archiveName.indexOf(".") == -1) {
				archiveName = archiveName + "." + archiveType;
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
			for (IType t:types) {
				if (!first) {
					buffer.append(" , ");
				} else {
					first = false;
				}
				String typeName = t.getFullyQualifiedName();
				int lastPeriod = typeName.lastIndexOf(".");
				String className = typeName;
				if (lastPeriod >= 0 && lastPeriod < typeName.length()) {
					className = typeName.substring(lastPeriod + 1, typeName.length());
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
			addImport(imports, importsRewrite, "org.jboss.shrinkwrap.api.asset.EmptyAsset");
			buffer.append(delimiter);
			buffer.append(".addAsManifestResource(EmptyAsset.INSTANCE, \"beans.xml\")");
		}
		
		buffer.append(";").append(delimiter);
        buffer.append("// System.out.println(archive.toString();").append(delimiter);
        buffer.append("return archive;").append(delimiter);
		buffer.append("}"); //$NON-NLS-1$
		buffer.append(delimiter);
		content = buffer.toString();

		IMethod createdMethod = type.createMethod(content, sibling, force, null);

		if (icu != null) {
			TextEdit edit= importsRewrite.rewriteImports(null);
			JavaModelUtil.applyEdit(importsRewrite.getCompilationUnit(), edit, false, null);
			ISourceRange range= createdMethod.getSourceRange();

			IBuffer buf= icu.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			int indent = StubUtility.getIndentUsed(type) + 1;
			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, originalContent, indent, delimiter, type.getJavaProject());
			formattedContent= Strings.trimLeadingTabsAndSpaces(formattedContent);
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

}
