package org.constellation.dto.wms;

import java.io.Serializable;

/**
 * Simple DTO that expose style name and title.
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class StyleDTO implements Serializable {

    private String name;

    private String title;

    public StyleDTO() {
    }

    public StyleDTO(String name, String title) {
        this.name = name;
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
