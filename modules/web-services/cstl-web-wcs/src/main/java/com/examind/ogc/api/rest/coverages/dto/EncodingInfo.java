package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quentin BIALOTA
 */
public class EncodingInfo {

    @JsonProperty("dataType")
    private String dataType;

    public EncodingInfo(String dataType) {
        this.dataType = dataType;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @JsonIgnore
    private static final Map<Class<?>, String> TYPE_TO_LINK_MAP = new HashMap<>();

    static {
        TYPE_TO_LINK_MAP.put(Byte.class, "https://www.opengis.net/def/data-type/ogc/0/unsigned-byte");
        TYPE_TO_LINK_MAP.put(Short.class, "https://www.opengis.net/def/data-type/ogc/0/unsigned-short");
        TYPE_TO_LINK_MAP.put(Integer.class, "https://www.opengis.net/def/data-type/ogc/0/unsigned-int");
        TYPE_TO_LINK_MAP.put(Long.class, "https://www.opengis.net/def/data-type/ogc/0/unsigned-long");
        TYPE_TO_LINK_MAP.put(Float.class, "https://www.opengis.net/def/data-type/ogc/0/float32");
        TYPE_TO_LINK_MAP.put(Double.class, "https://www.opengis.net/def/data-type/ogc/0/double");
        // Add other types here
    }

    public static String getOpenGisLink(Class<?> type) {
        String link = TYPE_TO_LINK_MAP.get(type);
        if (link == null) {
            return "empty";
        } else {
            return link;
        }
    }
}
