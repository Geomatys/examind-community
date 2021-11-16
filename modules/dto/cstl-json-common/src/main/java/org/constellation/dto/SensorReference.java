package org.constellation.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class SensorReference extends Identifiable implements Serializable {

    private static final long serialVersionUID = 7557342300322707217L;

    protected String identifier;

    public SensorReference() {

    }

    public SensorReference(Integer id, String identifier) {
        super(id);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.identifier != null) {
            sb.append("identifier: ").append(identifier).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            SensorReference that = (SensorReference) obj;
            return Objects.equals(this.identifier, that.identifier);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + super.hashCode();
        hash = 71 * hash + Objects.hashCode(this.identifier);
        return hash;
    }
}
