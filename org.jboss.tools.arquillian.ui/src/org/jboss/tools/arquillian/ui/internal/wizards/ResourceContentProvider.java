package org.jboss.tools.arquillian.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResourceContentProvider implements IStructuredContentProvider {

	private ProjectResource[] resourceDeployments;

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		resourceDeployments = (ProjectResource[]) newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return resourceDeployments;
	}

}
