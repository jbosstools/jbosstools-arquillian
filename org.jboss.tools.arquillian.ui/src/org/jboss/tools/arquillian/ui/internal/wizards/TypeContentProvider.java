package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class TypeContentProvider implements IStructuredContentProvider {

	private IType[] types;
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		types = (IType[]) newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return types;
	}

}
