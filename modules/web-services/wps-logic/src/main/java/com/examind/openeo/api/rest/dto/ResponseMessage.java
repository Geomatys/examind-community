package com.examind.openeo.api.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class ResponseMessage {
    @JsonProperty("id")
    private String id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("links")
    private List<Link> links = new ArrayList<>();

    public ResponseMessage(String id, String code, String message, List<Link> links) {
        this.id = id;
        this.code = code;
        this.message = message;
        this.links = links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponseMessage that = (ResponseMessage) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code) && Objects.equals(message, that.message) && Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, message, links);
    }

    @Override
    public String toString() {
        return "ResponseMessage{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", links=" + links +
                '}';
    }
}
