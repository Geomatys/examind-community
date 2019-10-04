package org.constellation.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class StyleReference implements Serializable {

    private static final long serialVersionUID = -3049058751205624982L;

    protected Integer id;

    protected String name;

    protected Integer providerId;

    protected String providerIdentifier;

    public StyleReference() {

    }

    public StyleReference(Integer id, String name, Integer providerId, String providerIdentifier) {
        this.id = id;
        this.name = name;
        this.providerId = providerId;
        this.providerIdentifier = providerIdentifier;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getProviderIdentifier() {
        return providerIdentifier;
    }

    public void setProviderIdentifier(String providerIdentifier) {
        this.providerIdentifier = providerIdentifier;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof StyleReference) {
            final StyleReference that = (StyleReference) obj;
            return Objects.equals(this.id, that.id) &&
                   Objects.equals(this.providerId, that.providerId) &&
                   Objects.equals(this.providerIdentifier, that.providerIdentifier) &&
                   Objects.equals(this.name, that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 53 * hash + (this.providerId != null ? this.providerId.hashCode() : 0);
        hash = 53 * hash + (this.providerIdentifier != null ? this.providerIdentifier.hashCode() : 0);
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[StyleReference]");
        if (id != null) {
            sb.append("id=").append(id).append('\n');
        }
        if (name != null) {
            sb.append("name=").append(name).append('\n');
        }
        if (providerId != null) {
            sb.append("providerId=").append(providerId).append('\n');
        }
        if (providerIdentifier != null) {
            sb.append("providerIdentifier=").append(providerIdentifier).append('\n');
        }
        return sb.toString();
    }
}
