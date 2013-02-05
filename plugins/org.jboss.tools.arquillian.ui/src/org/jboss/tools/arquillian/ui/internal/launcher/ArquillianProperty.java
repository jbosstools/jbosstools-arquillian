package org.jboss.tools.arquillian.ui.internal.launcher;

public class ArquillianProperty implements Comparable<ArquillianProperty> {
	private String name;
	private String value;
	private String source;
	private boolean defaultValue;
	private boolean changed;

	public ArquillianProperty(String name, String value, String source,
			boolean defaultValue) {
		super();
		this.name = name;
		this.value = value;
		this.source = source;
		this.defaultValue = defaultValue;
		this.changed = false;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (defaultValue ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArquillianProperty other = (ArquillianProperty) obj;
		if (defaultValue != other.defaultValue)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ArquillianProperty [name=" + name + ", value=" + value
				+ ", source=" + source + ", defaultValue=" + defaultValue + "]";
	}

	@Override
	public int compareTo(ArquillianProperty o) {
		if (o == null) {
			return 1;
		}
		if (name == null && o.getName() == null) {
			return 0;
		}
		if (name == null) {
			return -1;
		}
		if (o.getName() == null) {
			return 1;
		}
		return name.compareTo(o.getName());
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}
}
