package com.examind.openeo.api.rest.data.discovery.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import java.util.List;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/EO-Data-Discovery">OpenEO Doc</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collections {

    @JsonProperty("links")
    private List<Link> links;
    @JsonProperty("collections")
    private List<Collection> collections;
    
    public Collections() {
        
    }
    
    public Collections(List<Collection> collections, List<Link> links) {
        this.collections = collections;
        this.links = links;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }
}
