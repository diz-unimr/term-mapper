package de.unimarburg.diz.termmapper.model;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MappingUpdate implements Serializable {

    private String version;
    private String oldVersion;
    private Collection<String> updates;
    private boolean manual;

    public MappingUpdate() {
    }

    public MappingUpdate(String actualVersion, String oldVersion,
                         Collection<String> updates) {
        this(actualVersion, oldVersion, updates, false);
    }

    public MappingUpdate(String actualVersion, String oldVersion,
                         Collection<String> updates, boolean manual) {
        this.version = actualVersion;
        this.oldVersion = oldVersion;
        this.updates = updates;
        this.manual = manual;
    }

    public String getVersion() {
        return version;
    }

    @JsonSetter("version")
    public void setVersion(String version) {
        this.version = version;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    @JsonSetter("oldVersion")
    public void setOldVersion(String oldVersion) {
        this.oldVersion = oldVersion;
    }

    public Collection<String> getUpdates() {
        return updates;
    }

    @JsonSetter("updates")
    public void setUpdates(List<String> updates) {
        this.updates = updates;
    }

    public boolean isManual() {
        return manual;
    }

    @JsonSetter("manual")
    public void setManual(boolean manual) {
        this.manual = manual;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MappingUpdate that = (MappingUpdate) o;
        return Objects.equals(version, that.version) && Objects.equals(
            oldVersion, that.oldVersion) && Objects.equals(updates,
            that.updates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, oldVersion, updates);
    }
}
