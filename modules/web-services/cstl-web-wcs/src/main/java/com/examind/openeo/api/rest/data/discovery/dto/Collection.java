package com.examind.openeo.api.rest.data.discovery.dto;

import com.examind.ogc.api.rest.common.dto.Extent;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Collection extends com.examind.ogc.api.rest.common.dto.Collection {
    public Collection() {}

    public Collection(String id, List<Link> links, String title, String description, String name, Extent extent, List<String> crs, String storageCrs, String stacVersion, Set<String> stacExtensions, List<String> keywords, Boolean deprecated, String license) {
        super(id, links, title, description, name, extent, crs, storageCrs);
    }

    public Collection(com.examind.ogc.api.rest.common.dto.Collection collection, String stacVersion, Set<String> stacExtensions, List<String> keywords, Boolean deprecated, String license) {
        super(collection);
        this.stacVersion = stacVersion;
        this.stacExtensions = stacExtensions;
        this.keywords = keywords;
        this.deprecated = deprecated;
        this.license = license;
    }

    @JsonProperty("stac_version")
    private String stacVersion;

    @JsonProperty("stac_extensions")
    @Valid
    private Set<String> stacExtensions = new HashSet<>();

    @JsonProperty("type")
    @Valid
    private String type = "Collection";

    @JsonProperty("keywords")
    @Valid
    private List<String> keywords = new ArrayList<>();

    @JsonProperty("deprecated")
    private Boolean deprecated = false;

    @JsonProperty("license")
    private String license;

    @JsonProperty("providers")
    @Valid
    private List<Provider> providers = null;

    public String getStacVersion() {
        return stacVersion;
    }

    public void setStacVersion(String stacVersion) {
        this.stacVersion = stacVersion;
    }

    public Set<String> getStacExtensions() {
        return stacExtensions;
    }

    public void setStacExtensions(Set<String> stacExtensions) {
        this.stacExtensions = stacExtensions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Collection that = (Collection) o;
        return Objects.equals(stacVersion, that.stacVersion) && Objects.equals(stacExtensions, that.stacExtensions) && Objects.equals(type, that.type) && Objects.equals(keywords, that.keywords) && Objects.equals(deprecated, that.deprecated) && Objects.equals(license, that.license) && Objects.equals(providers, that.providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stacVersion, stacExtensions, type, keywords, deprecated, license, providers);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "stacVersion='" + stacVersion + '\'' +
                ", stacExtensions=" + stacExtensions +
                ", type='" + type + '\'' +
                ", keywords=" + keywords +
                ", deprecated=" + deprecated +
                ", license='" + license + '\'' +
                ", providers=" + providers +
                '}';
    }
}
