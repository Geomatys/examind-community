package org.constellation.map.featureinfo.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CoverageInfo.class, name = "coverage"),
        @JsonSubTypes.Type(value = FeatureInfo.class, name = "feature")
})
public interface LayerInfo {}
