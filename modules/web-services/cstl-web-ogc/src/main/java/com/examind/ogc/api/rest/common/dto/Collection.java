package com.examind.ogc.api.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.geotoolkit.atom.xml.Link;

import java.util.List;

/**
 * @author Quentin BIALOTA
 * @author Guilhem LEGAL
 */
@XmlRootElement(name = "Collection")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collection extends CommonResponse {

    public Collection() {}

    public Collection(String id, List<Link> links, String title, String description, String name, Extent extent, List<String> crs, String storageCrs) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.name = name;
        this.extent = extent;
        this.crs = crs;
        this.storageCrs = storageCrs;
        this.links = links;
    }

    public Collection(Collection sourceCollection) {
        this.id = sourceCollection.getId();
        this.title = sourceCollection.getTitle();
        this.description = sourceCollection.getDescription();
        this.name = sourceCollection.getName();
        this.extent = sourceCollection.getExtent();
        this.crs = sourceCollection.getCrs();
        this.storageCrs = sourceCollection.getStorageCrs();
        this.links = sourceCollection.getLinks();
    }

    @XmlElement(
            name = "link",
            namespace = "http://www.w3.org/2005/Atom"
    )
    @JsonProperty("links")
    private List<Link> links;

    @XmlElement(
            name = "Id"
    )
    @JsonProperty("id")
    private String id;

    @XmlElement(
            name = "Name"
    )
    @JsonProperty("name")
    private String name;

    @XmlElement(
            name = "Title"
    )
    @JsonProperty("title")
    private String title;

    @XmlElement(
            name = "Description"
    )
    @JsonProperty("description")
    private String description;

    @XmlElement(
            name = "Extent"
    )
    @JsonProperty("extent")
    private Extent extent;

    @XmlElement(
            name = "Crs"
    )
    @JsonProperty("crs")
    private List<String> crs;

    @XmlElement(
            name = "StorageCrs"
    )
    @JsonProperty("storageCrs")
    private String storageCrs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public Extent getExtent() {
        return extent;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }

    public List<String> getCrs() {
        return crs;
    }

    public void setCrs(List<String> crs) {
        this.crs = crs;
    }

    public String getStorageCrs() {
        return storageCrs;
    }

    public void setStorageCrs(String storageCrs) {
        this.storageCrs = storageCrs;
    }
}
