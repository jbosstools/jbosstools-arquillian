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
package org.jboss.tools.arquillian.ui.internal.preferences;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.arquillian.core.ArquillianCoreActivator;
import org.jboss.tools.arquillian.core.internal.preferences.ArquillianConstants;
import org.jboss.tools.arquillian.core.internal.util.ArquillianUtility;
import org.jboss.tools.arquillian.ui.ArquillianUIActivator;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;

/**
 * 
 * @author snjeza
 *
 */
public class ArquillianPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.jboss.tools.arquillian.ui.preferences.arquillianPreferencePage"; //$NON-NLS-1$
	private Combo combo;

	private static final String[] defaultVersions = new String[] {ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT};
	
	private static final String COORDS = ArquillianUtility.ARQUILLIAN_GROUP_ID + ":" + ArquillianUtility.ARQUILLIAN_BOM_ARTIFACT_ID + ":[0,)";  //$NON-NLS-1$ //$NON-NLS-2$
	
	@Override
	public void init(IWorkbench workbench) {
		
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,false));
        composite.setLayout(new GridLayout(2, false));
        
        Label label = new Label(composite, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL,true,false);
        label.setLayoutData(gd);
        label.setText("Arquillian version:");
        combo = new Combo(composite, SWT.READ_ONLY);
        gd = new GridData(SWT.FILL, SWT.FILL,true,true);
        combo.setLayoutData(gd);
        combo.setItems(getArquillianVersions());
        String value = ArquillianUtility.getPreference(ArquillianConstants.ARQUILLIAN_VERSION);
        combo.setText(value);
		return composite;
	}

	@Override
    protected void performDefaults() {
        IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        combo.setText(ArquillianConstants.ARQUILLIAN_VERSION_DEFAULT);
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
    	IPreferenceStore prefs = ArquillianCoreActivator.getDefault().getPreferenceStore();
        prefs.setValue(ArquillianConstants.ARQUILLIAN_VERSION, combo.getText());
       
        return super.performOk();
    }
    
    private static String[] getArquillianVersions() {
		RepositorySystem system;
		try {
			system = new DefaultPlexusContainer().lookup(RepositorySystem.class);
		} catch (Exception e) {
			ArquillianUIActivator.log(e);
			return defaultVersions;
		}
		MavenRepositorySystemSession session = new MavenRepositorySystemSession();
		IMaven maven = MavenPlugin.getMaven();
		String localRepoHome = maven.getLocalRepositoryPath();
		LocalRepository localRepo = new LocalRepository(localRepoHome);
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));
		
		VersionRangeRequest rangeRequest = new VersionRangeRequest();
		rangeRequest.setArtifact( new DefaultArtifact(COORDS));
		
		List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
	    try {
			repos.addAll(maven.getArtifactRepositories(false));
		} catch (CoreException e) {
			ArquillianUIActivator.log(e);
			return defaultVersions;
		}
		for (ArtifactRepository repo : repos) {
			RemoteRepository remoteRepo = new RemoteRepository(repo.getId(), "default", repo.getUrl()); //$NON-NLS-1$
			rangeRequest.addRepository(remoteRepo);
		}
		try {
			VersionRangeResult result = system.resolveVersionRange(	session, rangeRequest);
			List<Version> versions = result.getVersions();
			if (versions == null || versions.size() <=  0) {
				return defaultVersions;
			}
			String[] versionStrings = new String[versions.size()];
			int i = 0;
			for (Version version:versions) {
				versionStrings[i++] = version.toString();
			}
			return versionStrings;
		} catch (VersionRangeResolutionException e) {
			ArquillianUIActivator.log(e);
		}
		return defaultVersions;
	}

}
