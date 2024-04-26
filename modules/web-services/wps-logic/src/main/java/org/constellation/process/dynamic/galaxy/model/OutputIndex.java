package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class OutputIndex {
    @JsonProperty("src")
    private String src;
    @JsonProperty("id")
    private String id;
    @JsonProperty("uuid")
    private String uuid;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
