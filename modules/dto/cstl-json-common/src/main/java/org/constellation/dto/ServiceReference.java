package org.constellation.dto;

import java.io.Serializable;
import java.util.Objects;
import org.constellation.dto.service.Service;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class ServiceReference implements Serializable {

    private static final long serialVersionUID = 7905763341389113756L;


    protected Integer id;

    protected String identifier;

    protected String type;

    public ServiceReference() {

    }

    public ServiceReference(Integer id, String identifier, String type) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
    }

    public ServiceReference(Service service) {
        if (service != null) {
            this.id = service.getId();
            this.identifier = service.getIdentifier();
            this.type = service.getType();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "{id=" + id + " identifier=" + identifier + " type=" + type + "}";
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ServiceReference) {
            ServiceReference that = (ServiceReference) obj;
            return Objects.equals(this.id,         that.id) &&
                   Objects.equals(this.identifier, that.identifier) &&
                   Objects.equals(this.type,       that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.identifier);
        hash = 97 * hash + Objects.hashCode(this.type);
        return hash;
    }
}
