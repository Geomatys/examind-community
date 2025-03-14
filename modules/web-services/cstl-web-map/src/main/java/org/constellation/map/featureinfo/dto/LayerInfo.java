package org.constellation.map.featureinfo.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CoverageInfo.class, name = "coverage"),
        @JsonSubTypes.Type(value = FeatureInfo.class, name = "feature"),
        @JsonSubTypes.Type(value = GeoFeatureInfo.class, name = "feature"),
        @JsonSubTypes.Type(value = GeoCoverageInfo.class, name = "feature"),
        @JsonSubTypes.Type(value = FeatureCollectionInfo.class, name = "FeatureCollection"),
        @JsonSubTypes.Type(value = LayerError.class, name = "error")
})
public interface LayerInfo {}
