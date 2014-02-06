package org.jboss.forge.arquillian.container;

import java.util.List;

/**
 * @Author Paul Bakker - paul.bakker.nl@gmail.com
 */
public class Container {
    public static final String ARQUILLIAN_CONTAINER_NAME_START = "Arquillian Container ";
	private String group_id;
    private String artifact_id;
    private String name;
    private ContainerType container_type;
    private List<Dependency> dependencies;
    private Dependency download;
    private List<Configuration> configurations;
    private boolean activate = false;

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public String getArtifact_id() {
        return artifact_id;
    }

    public void setArtifact_id(String artifact_id) {
        this.artifact_id = artifact_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContainerType getContainer_type() {
        return container_type;
    }

    public void setContainer_type(ContainerType container_type) {
        this.container_type = container_type;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Dependency getDownload() {
        return download;
    }

    public void setDownload(Dependency download) {
        this.download = download;
    }

    public List<Configuration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<Configuration> configurations) {
        this.configurations = configurations;
    }

    public String getId() {
        return name.replace(ARQUILLIAN_CONTAINER_NAME_START, "").replaceAll(" ", "_").toUpperCase();  //$NON-NLS-1$//$NON-NLS-4$
    }

    @Override
    public String toString() {
        return getId();
    }

	public boolean isActivate() {
		return activate;
	}

	public void setActivate(boolean activate) {
		this.activate = activate;
	}
}
