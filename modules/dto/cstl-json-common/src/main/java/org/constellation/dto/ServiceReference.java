package org.constellation.dto;

import java.io.Serializable;
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
}
