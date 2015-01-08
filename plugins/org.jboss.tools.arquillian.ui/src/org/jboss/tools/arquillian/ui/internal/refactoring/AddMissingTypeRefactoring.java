/*************************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.arquillian.ui.internal.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.text.edits.MultiTextEdit;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianSearchEngine;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.jboss.tools.arquillian.ui.internal.markers.RefactoringUtil;

/**
 *  A refactoring to add missing type to deployment
 *  
 * @author snjeza
 *
 */
public class AddMissingTypeRefactoring extends Refactoring {

	private static final String ARCHIVE_VARIABLE = "archive";
	private static final String CANNOT_FIND_A_DEPLOYMENT_METHOD = "Cannot find a deployment method";
	private IMarker marker;
	private String[] deploymentMethods;
	private List<MethodDeclaration> deploymentMethodDeclarations;
	private CompilationUnit astRoot;
	private String className;
	private ICompilationUnit cUnit;
	private IFile file;
	private IExpressionFragment selectedExpression;
	private ASTRewrite rewrite;
	private AST ast;
	private String tempName;
	private ImportRewrite importRewrite;
	private String[] excludedVariableNames;
	private String message;
	private boolean addAllDependentClasses = false;
	private List<IMethodBinding> abstractMethodBindings;
	private String deploymentMethodName;
	private ListRewrite listRewriter;
	
	/**
	 * @param marker
	 */
	public AddMissingTypeRefactoring(IMarker marker) {
		super();
		this.marker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
	public String getName() {
		return RefactoringUtil.getQuickFixName(marker);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		className = RefactoringUtil.getMissingClassName(marker);
		String message = null;
		if (className == null) {
			message = "Invalid marker";
		} else {
			deploymentMethods = getDeploymentMethods();
			if (deploymentMethods == null || deploymentMethods.length <= 0) {
				abstractMethodBindings = getAbstractDeploymentMethodBindings();
				if (abstractMethodBindings == null) {
					message = CANNOT_FIND_A_DEPLOYMENT_METHOD;
				}
			} else {
				deploymentMethodName = deploymentMethods[0];
			}
		}
		if (message != null) {
			IStatus status = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, message);
			return RefactoringStatus.create(status);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	public String[] getAbstractDeploymentMethods() {
		
		if (cUnit != null) {
			List<IMethodBinding> methods;
			try {
				methods = ArquillianSearchEngine.getDeploymentMethods(cUnit.findPrimaryType());
			} catch (JavaModelException e) {
				return new String[0];
			}
			String[] names = new String[methods.size()];
			int i = 0;
			for (IMethodBinding method:methods) {
				names[i++] = method.getName();
			}
			return names;
		}
		return new String[0];
	}
	
	private List<IMethodBinding> getAbstractDeploymentMethodBindings() {
		if (cUnit != null) {
			List<IMethodBinding> methods;
			try {
				methods = ArquillianSearchEngine.getDeploymentMethods(cUnit.findPrimaryType());
			} catch (JavaModelException e) {
				return null;
			}
			if (methods.size() > 0) {
				IMethodBinding methodBinding = methods.get(0);
				deploymentMethodName = methodBinding.getName();
				return methods;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		if (message != null) {
			IStatus status = new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, message);
			return RefactoringStatus.create(status);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		message = null;
		if (astRoot == null || 
				(abstractMethodBindings == null && deploymentMethodName == null)) {
			message = CANNOT_FIND_A_DEPLOYMENT_METHOD;
			return null;
		}
		MethodDeclaration deploymentMethod = null;
		TextFileChange result = new TextFileChange( file.getName(), file );
	    MultiTextEdit rootEdit = new MultiTextEdit();
	    importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
		if (astRoot.getAST().hasResolvedBindings()) {
			importRewrite.setUseContextToFilterImplicitImports(true);
		}
		rewrite= ASTRewrite.create(astRoot.getAST());
		IType fType = cUnit.findPrimaryType();
		if (fType.isAnonymous()) {
			final ClassInstanceCreation creation= (ClassInstanceCreation) ASTNodes.getParent(NodeFinder.perform(astRoot, fType.getNameRange()), ClassInstanceCreation.class);
			if (creation != null) {
				final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
				if (declaration != null)
					listRewriter= rewrite.getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
			}
		} else {
			final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(astRoot, fType.getNameRange()), AbstractTypeDeclaration.class);
			if (declaration != null)
				listRewriter= rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
		}
		if (listRewriter == null) {
			throw new CoreException(new Status(IStatus.ERROR, ArquillianUIActivator.PLUGIN_ID, IStatus.ERROR, "Could not find the type element", null));
		}
		ast = astRoot.getAST();
		ReturnStatement returnStatement;
		if (abstractMethodBindings != null) {
			IMethodBinding abstractMethodBinding = null;
			for (IMethodBinding binding:abstractMethodBindings) {
				if (binding.getName().equals(deploymentMethodName)) {
					abstractMethodBinding = binding;
					break;
				}
			}
			if (abstractMethodBinding == null) {
				message = CANNOT_FIND_A_DEPLOYMENT_METHOD;
				return null;
			}
			deploymentMethod = ast.newMethodDeclaration();
			deploymentMethod.setName(ast.newSimpleName(deploymentMethodName));
			
			IJavaProject javaProject = abstractMethodBinding.getJavaElement().getJavaProject();
			String archiveTypeName = ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_JAVA_ARCHIVE;
			if (javaProject != null && javaProject.isOpen()) {
				IProject project = javaProject.getProject();
				IFile pomFile = project.getFile(IMavenConstants.POM_FILE_NAME);
				if (pomFile != null && pomFile.exists()) {
					try {
						IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, new NullProgressMonitor());
						MavenProject mavenProject = facade.getMavenProject(new NullProgressMonitor());
						Model model = mavenProject.getModel();
						String packaging = model.getPackaging();
						if (ArquillianConstants.WAR.equals(packaging)) {
							archiveTypeName = ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_WEB_ARCHIVE;
						}
						if (ArquillianConstants.EAR.equals(packaging)) {
							archiveTypeName = ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_ENTERPRISE_ARCHIVE;
						}
						if (ArquillianConstants.RAR.equals(packaging)) {
							archiveTypeName = ArquillianUtility.ORG_JBOSS_SHRINKWRAP_API_SPEC_RESOURCEADAPTER_ARCHIVE;
						}
					} catch (CoreException e) {
						ArquillianUIActivator.log(e);
					}
				}
			}
			int index = archiveTypeName.lastIndexOf("."); //$NON-NLS-1$
			String simpleArchiveName;
			if (index >= 0) {
				simpleArchiveName = archiveTypeName.substring(index + 1);
			} else {
				simpleArchiveName = archiveTypeName;
			}
			SimpleName type2 = ast.newSimpleName(simpleArchiveName);
			importRewrite.addImport(archiveTypeName);
			deploymentMethod.setReturnType2(ast.newSimpleType(type2));
			deploymentMethod.setConstructor(false);
			deploymentMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.PUBLIC|Modifier.STATIC));

			Annotation marker= rewrite.getAST().newMarkerAnnotation();
			importRewrite.addImport(ArquillianUtility.ORG_JBOSS_ARQUILLIAN_CONTAINER_TEST_API_DEPLOYMENT);
			marker.setTypeName(rewrite.getAST().newSimpleName("Deployment")); //$NON-NLS-1$
			rewrite.getListRewrite(deploymentMethod, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
			
			Block body= ast.newBlock();
			deploymentMethod.setBody(body);
			
			// XXXArchive archive = (XXXArchive) AbstractTest.createDeployment();
			String declaringClassQualifiedName = abstractMethodBinding.getDeclaringClass().getQualifiedName();
			importRewrite.addImport(declaringClassQualifiedName);
			String declaringClassName = abstractMethodBinding.getDeclaringClass().getName();
			MethodInvocation deploymentInvocation = ast.newMethodInvocation();
			deploymentInvocation.setExpression(ast.newName(declaringClassName));
			deploymentInvocation.setName(ast.newSimpleName(deploymentMethodName));
			VariableDeclarationFragment archiveDeclFragment= ast.newVariableDeclarationFragment();
			archiveDeclFragment.setName(ast.newSimpleName(ARCHIVE_VARIABLE));
			ITypeBinding returnType = abstractMethodBinding.getReturnType();
			if (!archiveTypeName.equals(
					returnType.getQualifiedName())) {
				CastExpression cast = ast.newCastExpression();
				cast.setType(ast.newSimpleType(ast.newName(simpleArchiveName)));
				cast.setExpression(deploymentInvocation);
				archiveDeclFragment.setInitializer(cast);
			} else {
				archiveDeclFragment.setInitializer(deploymentInvocation);
			}
			VariableDeclarationStatement vStatement= ast.newVariableDeclarationStatement(archiveDeclFragment);
			vStatement.setType(ast.newSimpleType(ast.newName(simpleArchiveName)));
			body.statements().add(vStatement);
			
			//return archive;
			returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(ast.newSimpleName(ARCHIVE_VARIABLE));
			//body.statements().add(returnStatement);
		} else {
			for (MethodDeclaration md : deploymentMethodDeclarations) {
				if (deploymentMethodName.equals(md.getName().getIdentifier())) {
					deploymentMethod = md;
					break;
				}
			}
			if (deploymentMethod == null) {
				message = CANNOT_FIND_A_DEPLOYMENT_METHOD;
				return null;
			}
			returnStatement = getReturnStatement(deploymentMethod);
			ast = deploymentMethod.getAST();
			rewrite = ASTRewrite.create(ast);
		}
		
		if (returnStatement == null) {
			message = "Cannot find a return statement";
			return null;
		}
		
	    Expression expression = returnStatement.getExpression();
		
		if (expression instanceof MethodInvocation) {
			int start = expression.getStartPosition();
			int length = expression.getLength();
			selectedExpression = getSelectedExpression(new SourceRange(start, length));
			tempName = ARCHIVE_VARIABLE;
			int i = 0;
			while (!checkTempName(tempName).isOK()) {
				tempName = tempName + i++;
			}
			createAndInsertTempDeclaration(start, length);
			addReplaceExpressionWithTemp();
			createStatements(tempName, className, deploymentMethod, returnStatement, rootEdit, pm);
		} 
		if (expression instanceof SimpleName) {
			String name = ((SimpleName) expression).getIdentifier();
			createStatements(name,className, deploymentMethod, returnStatement, rootEdit, pm);
		}
		
		result.setEdit(rootEdit); 
	    return result;
	}

	private void createStatements(String variableName, String cName, MethodDeclaration deploymentMethod,
			ReturnStatement returnStatement, MultiTextEdit rootEdit, 
			IProgressMonitor pm)
			throws JavaModelException, CoreException {
		createStatement(variableName, cName, deploymentMethod, returnStatement, rootEdit, pm);
		if (addAllDependentClasses) {
			Set<String> classNames = new HashSet<String>();
			IMarker[] markers = file.findMarkers(ArquillianConstants.MARKER_CLASS_ID, true, IResource.DEPTH_INFINITE);
			for (IMarker marker:markers) {
				if (RefactoringUtil.isMissingClassMarker(marker)) {
					String clazzName = RefactoringUtil.getMissingClassName(marker);
					if (clazzName != null && !clazzName.equals(className) && !classNames.contains(clazzName)) {
						classNames.add(clazzName);
					}
				}
			}
			for (String c:classNames) {
				createStatement(variableName, c, deploymentMethod, returnStatement, rootEdit, pm);
			}
		}
		if (abstractMethodBindings != null) {
			deploymentMethod.getBody().statements().add(returnStatement);
			listRewriter.insertLast(deploymentMethod, null);
		}
		rootEdit.addChild(rewrite.rewriteAST());
		rootEdit.addChild(importRewrite.rewriteImports(pm));
		ArquillianCoreActivator.getDefault().removeProjectLoader(file.getProject());
	}

	private RefactoringStatus checkTempName(String newName) {
		RefactoringStatus status= Checks.checkTempName(newName, cUnit);
		if (Arrays.asList(getExcludedVariableNames()).contains(newName))
			status.addWarning(Messages.format(RefactoringCoreMessages.ExtractTempRefactoring_another_variable, BasicElementLabels.getJavaElementName(newName)));
		return status;
	}
	
	private String[] getExcludedVariableNames() {
		if (excludedVariableNames == null) {
			IBinding[] bindings= new ScopeAnalyzer(astRoot).getDeclarationsInScope(selectedExpression.getStartPosition(), ScopeAnalyzer.VARIABLES
					| ScopeAnalyzer.CHECK_VISIBILITY);
			excludedVariableNames= new String[bindings.length];
			for (int i= 0; i < bindings.length; i++) {
				excludedVariableNames[i]= bindings[i].getName();
			}
		}
		return excludedVariableNames;
	}
	
	private void addReplaceExpressionWithTemp() throws JavaModelException {
		IASTFragment[] fragmentsToReplace= retainOnlyReplacableMatches(getMatchingFragments());
		//TODO: should not have to prune duplicates here...
		HashSet<IASTFragment> seen= new HashSet<IASTFragment>();
		for (int i= 0; i < fragmentsToReplace.length; i++) {
			IASTFragment fragment= fragmentsToReplace[i];
			if (! seen.add(fragment))
				continue;
			SimpleName simpleName= ast.newSimpleName(tempName);
			
			fragment.replace(rewrite, simpleName, null);
		}
	}
	
	private static IASTFragment[] retainOnlyReplacableMatches(IASTFragment[] allMatches) {
		List<IASTFragment> result= new ArrayList<IASTFragment>(allMatches.length);
		for (int i= 0; i < allMatches.length; i++) {
			if (canReplace(allMatches[i]))
				result.add(allMatches[i]);
		}
		return result.toArray(new IASTFragment[result.size()]);
	}
	
	private static boolean canReplace(IASTFragment fragment) {
		ASTNode node= fragment.getAssociatedNode();
		ASTNode parent= node.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) parent;
			if (node.equals(vdf.getName()))
				return false;
		}
		if (isMethodParameter(node))
			return false;
		if (isThrowableInCatchBlock(node))
			return false;
		if (parent instanceof ExpressionStatement)
			return false;
		if (isLeftValue(node))
			return false;
		if (isReferringToLocalVariableFromFor((Expression) node))
			return false;
		if (isUsedInForInitializerOrUpdater((Expression) node))
			return false;
		if (parent instanceof SwitchCase)
			return false;
		return true;
	}
	
	private static boolean isUsedInForInitializerOrUpdater(Expression expression) {
		ASTNode parent= expression.getParent();
		if (parent instanceof ForStatement) {
			ForStatement forStmt= (ForStatement) parent;
			return forStmt.initializers().contains(expression) || forStmt.updaters().contains(expression);
		}
		return false;
	}
	
	private static List<IVariableBinding> getForInitializedVariables(VariableDeclarationExpression variableDeclarations) {
		List<IVariableBinding> forInitializerVariables= new ArrayList<IVariableBinding>(1);
		for (Iterator<VariableDeclarationFragment> iter= variableDeclarations.fragments().iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= iter.next();
			IVariableBinding binding= fragment.resolveBinding();
			if (binding != null)
				forInitializerVariables.add(binding);
		}
		return forInitializerVariables;
	}
	
	private static boolean isReferringToLocalVariableFromFor(Expression expression) {
		ASTNode current= expression;
		ASTNode parent= current.getParent();
		while (parent != null && !(parent instanceof BodyDeclaration)) {
			if (parent instanceof ForStatement) {
				ForStatement forStmt= (ForStatement) parent;
				if (forStmt.initializers().contains(current) || forStmt.updaters().contains(current) || forStmt.getExpression() == current) {
					List<Expression> initializers= forStmt.initializers();
					if (initializers.size() == 1 && initializers.get(0) instanceof VariableDeclarationExpression) {
						List<IVariableBinding> forInitializerVariables= getForInitializedVariables((VariableDeclarationExpression) initializers.get(0));
						ForStatementChecker checker= new ForStatementChecker(forInitializerVariables);
						expression.accept(checker);
						if (checker.isReferringToForVariable())
							return true;
					}
				}
			}
			current= parent;
			parent= current.getParent();
		}
		return false;
	}
	private static boolean isThrowableInCatchBlock(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof CatchClause);
	}
	
	private static boolean isLeftValue(ASTNode node) {
		ASTNode parent= node.getParent();
		if (parent instanceof Assignment) {
			Assignment assignment= (Assignment) parent;
			if (assignment.getLeftHandSide() == node)
				return true;
		}
		if (parent instanceof PostfixExpression)
			return true;
		if (parent instanceof PrefixExpression) {
			PrefixExpression.Operator op= ((PrefixExpression) parent).getOperator();
			if (op.equals(PrefixExpression.Operator.DECREMENT))
				return true;
			if (op.equals(PrefixExpression.Operator.INCREMENT))
				return true;
			return false;
		}
		return false;
	}

	private static boolean isMethodParameter(ASTNode node) {
		return (node instanceof SimpleName) && (node.getParent() instanceof SingleVariableDeclaration) && (node.getParent().getParent() instanceof MethodDeclaration);
	}
	
	private IASTFragment[] getMatchingFragments() throws JavaModelException {
		return new IASTFragment[] { selectedExpression};
	}
	
	private void createAndInsertTempDeclaration(int start, int length) throws CoreException {
		SourceRange range = new SourceRange(start, length);
		IExpressionFragment selectionExpression = getSelectedExpression(range);
		Expression initializer= selectionExpression.createCopyTarget(rewrite, true);
		VariableDeclarationStatement vds= createTempDeclaration(initializer);

		insertAt(selectionExpression.getAssociatedNode(), vds);
	}
	
	private void insertAt(ASTNode target, Statement declaration) {
		//ASTRewrite rewrite= fCURewrite.getASTRewrite();
		//TextEditGroup groupDescription= fCURewrite.createGroupDescription(RefactoringCoreMessages.ExtractTempRefactoring_declare_local_variable);

		ASTNode parent= target.getParent();
		StructuralPropertyDescriptor locationInParent= target.getLocationInParent();
		while (locationInParent != Block.STATEMENTS_PROPERTY && locationInParent != SwitchStatement.STATEMENTS_PROPERTY) {
			if (locationInParent == IfStatement.THEN_STATEMENT_PROPERTY
					|| locationInParent == IfStatement.ELSE_STATEMENT_PROPERTY
					|| locationInParent == ForStatement.BODY_PROPERTY
					|| locationInParent == EnhancedForStatement.BODY_PROPERTY
					|| locationInParent == DoStatement.BODY_PROPERTY
					|| locationInParent == WhileStatement.BODY_PROPERTY) {
				// create intermediate block if target was the body property of a control statement:
				Block replacement= rewrite.getAST().newBlock();
				ListRewrite replacementRewrite= rewrite.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				replacementRewrite.insertFirst(declaration, null);
				replacementRewrite.insertLast(rewrite.createMoveTarget(target), null);
				rewrite.replace(target, replacement, null);
				return;
			}
			target= parent;
			parent= parent.getParent();
			locationInParent= target.getLocationInParent();
		}
		ListRewrite lw = rewrite.getListRewrite(parent, (ChildListPropertyDescriptor) locationInParent);
		lw.insertBefore(declaration, target, null);
	}
	private VariableDeclarationStatement createTempDeclaration(Expression initializer) throws CoreException {
		VariableDeclarationFragment vdf= ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(tempName));
		vdf.setInitializer(initializer);

		VariableDeclarationStatement vds= ast.newVariableDeclarationStatement(vdf);
		
		vds.setType(createTempType());

		return vds;
	}
	
	private Type createTempType() throws CoreException {
		Expression expression= selectedExpression.getAssociatedExpression();

		Type resultingType= null;
		ITypeBinding typeBinding= expression.resolveTypeBinding();

		AST ast= rewrite.getAST();

		if (expression instanceof ClassInstanceCreation && (typeBinding == null || typeBinding.getTypeArguments().length == 0)) {
			resultingType= (Type) rewrite.createCopyTarget(((ClassInstanceCreation) expression).getType());
		} else if (expression instanceof CastExpression) {
			resultingType= (Type) rewrite.createCopyTarget(((CastExpression) expression).getType());
		} else {
			if (typeBinding == null) {
				typeBinding= ASTResolving.guessBindingForReference(expression);
			}
			if (typeBinding != null) {
				typeBinding= Bindings.normalizeForDeclarationUse(typeBinding, ast);
				ImportRewriteContext context= new ContextSensitiveImportRewriteContext(expression, importRewrite);
				resultingType= importRewrite.addImport(typeBinding, ast, context);
			} else {
				resultingType= ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
			}
		}
		
		return resultingType;
	}

	private IExpressionFragment getSelectedExpression(SourceRange range) throws JavaModelException {
		if (selectedExpression != null)
			return selectedExpression;
		IASTFragment selectedFragment= ASTFragmentFactory.createFragmentForSourceRange(range, astRoot, cUnit);

		if (selectedFragment instanceof IExpressionFragment && !Checks.isInsideJavadoc(selectedFragment.getAssociatedNode())) {
			selectedExpression= (IExpressionFragment) selectedFragment;
		} else if (selectedFragment != null) {
			if (selectedFragment.getAssociatedNode() instanceof ExpressionStatement) {
				ExpressionStatement exprStatement= (ExpressionStatement) selectedFragment.getAssociatedNode();
				Expression expression= exprStatement.getExpression();
				selectedExpression= (IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(expression);
			} else if (selectedFragment.getAssociatedNode() instanceof Assignment) {
				Assignment assignment= (Assignment) selectedFragment.getAssociatedNode();
				selectedExpression= (IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(assignment);
			}
		}

		if (selectedExpression != null && Checks.isEnumCase(selectedExpression.getAssociatedExpression().getParent())) {
			selectedExpression= null;
		}

		return selectedExpression;
	}
	private void createStatement(String variableName, String cName,
			MethodDeclaration deploymentMethod,
			ReturnStatement returnStatement, MultiTextEdit rootEdit,
			IProgressMonitor pm) throws JavaModelException, CoreException {
		
		importRewrite.addImport(cName);
		MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(ast.newSimpleName(variableName));
		mi.setName(ast.newSimpleName("addClass")); //$NON-NLS-1$
		int index = cName.lastIndexOf("."); //$NON-NLS-1$
		String simpleClassName;
		if (index >= 0) {
			simpleClassName = cName.substring(index + 1);
		} else {
			simpleClassName = cName;
		}
		Name typeName = ast.newName(simpleClassName);
		org.eclipse.jdt.core.dom.Type type = ast.newSimpleType(typeName);
		TypeLiteral typeLiteral = ast.newTypeLiteral();
		typeLiteral.setType(type);
		mi.arguments().add(typeLiteral);
		Statement newStatement = ast.newExpressionStatement(mi);
		if (abstractMethodBindings != null) {
			deploymentMethod.getBody().statements().add(newStatement);
		} else {
			ListRewrite lw = rewrite.getListRewrite(deploymentMethod.getBody(), Block.STATEMENTS_PROPERTY);
			lw.insertBefore(newStatement, returnStatement, null);
		}
	}

	private ReturnStatement getReturnStatement(MethodDeclaration deploymentMethod) {
		final Set<ReturnStatement> returnStatements = new HashSet<ReturnStatement>();
		deploymentMethod.accept(new ASTVisitor() {
		
			@Override
			public boolean visit(ReturnStatement statement) {
				ITypeBinding binding = statement.getExpression().resolveTypeBinding();
				if (ArquillianSearchEngine.isArchiveType(binding)) {
						returnStatements.add(statement);
				}
				return true;
			}
		
		});
		if (returnStatements.size() != 1) {
			// FIXME
			return null;
		}
		return returnStatements.iterator().next();
	}

	public String[] getDeploymentMethods() {
		if (deploymentMethods == null) {
			deploymentMethodDeclarations = new LinkedList<MethodDeclaration>();
			final CompilationUnit root = getAST();
			if (root == null) {
				deploymentMethods = new String[0];
			} else {
				root.accept(new ASTVisitor() {

					@Override
					public boolean visit(MethodDeclaration node) {
						IMethodBinding binding = node.resolveBinding();
						if (ArquillianSearchEngine.isDeploymentMethod(binding)) {
							deploymentMethodDeclarations.add(node);
						}
						return false;
					}

				});
				deploymentMethods = new String[deploymentMethodDeclarations.size()];
				int i = 0;
				for (MethodDeclaration methodDeclaration:deploymentMethodDeclarations) {
					deploymentMethods[i++] = methodDeclaration.getName().getIdentifier();
				}
			}
		}
		return deploymentMethods;
	}

	private CompilationUnit getAST() {
		IResource resource= marker.getResource();
		if ( !(resource instanceof IFile)) {
			return null;
		}
		file = (IFile) resource;
		IJavaElement element = JavaCore.create(file);
		if (!(element instanceof ICompilationUnit)) {
			return null;
		}
		cUnit = (ICompilationUnit) element;
		ASTParser parser= ASTParser.newParser(AST.JLS8);
		parser.setSource(cUnit);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		astRoot = (CompilationUnit) parser.createAST(null);
		return astRoot;
	}

	public void setDeploymentMethod(String name) {
		this.deploymentMethodName = name;
	}

	private static final class ForStatementChecker extends ASTVisitor {

		private final Collection<IVariableBinding> fForInitializerVariables;

		private boolean fReferringToForVariable= false;

		public ForStatementChecker(Collection<IVariableBinding> forInitializerVariables) {
			Assert.isNotNull(forInitializerVariables);
			fForInitializerVariables= forInitializerVariables;
		}

		public boolean isReferringToForVariable() {
			return fReferringToForVariable;
		}

		@Override
		public boolean visit(SimpleName node) {
			IBinding binding= node.resolveBinding();
			if (binding != null && fForInitializerVariables.contains(binding)) {
				fReferringToForVariable= true;
			}
			return false;
		}
	}

	public boolean isAddAllDependentClasses() {
		return addAllDependentClasses;
	}

	public void setAddAllDependentClasses(boolean addAllDependentClasses) {
		this.addAllDependentClasses = addAllDependentClasses;
	}

	public String[] getDeploymentMethodsNames() {
		String[] names = getDeploymentMethods();
		if (names.length > 0) {
			return names;
		}
		return getAbstractDeploymentMethods();
	}
	
}
