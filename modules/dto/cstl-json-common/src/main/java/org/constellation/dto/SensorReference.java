package org.constellation.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class SensorReference implements Serializable {

    private static final long serialVersionUID = 7557342300322707217L;


    protected Integer id;

    protected String identifier;


    public SensorReference() {

    }

    public SensorReference(Integer id, String identifier) {
        this.id = id;
        this.identifier = identifier;
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

    @Override
    public String toString() {
        return "{id=" + id + " identifier=" + identifier + "}";
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SensorReference) {
            SensorReference that = (SensorReference) obj;
            return Objects.equals(this.id,         that.id) &&
                   Objects.equals(this.identifier, that.identifier);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.identifier);
        return hash;
    }
}
