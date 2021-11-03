package org.constellation.dto;

import java.io.Serializable;
import java.util.Objects;
import org.constellation.dto.service.Service;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class ServiceReference extends Identifiable implements Serializable {

    private static final long serialVersionUID = 7905763341389113756L;

    private String identifier;

    private String type;

    public ServiceReference() {

    }

    public ServiceReference(Integer id, String identifier, String type) {
        super(id);
        this.identifier = identifier;
        this.type = type;
    }

    public ServiceReference(Service service) {
        super(service);
        if (service != null) {
            this.identifier = service.getIdentifier();
            this.type = service.getType();
        }
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
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.identifier != null) {
            sb.append("identifier: ").append(identifier).append('\n');
        }
        if (this.type != null) {
            sb.append("type: ").append(type).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ServiceReference && super.equals(obj)) {
            ServiceReference that = (ServiceReference) obj;
            return Objects.equals(this.identifier, that.identifier) &&
                   Objects.equals(this.type,       that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + super.hashCode();
        hash = 97 * hash + Objects.hashCode(this.identifier);
        hash = 97 * hash + Objects.hashCode(this.type);
        return hash;
    }
}
