package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class ToolInputs {
    @JsonProperty("properties")
    private Map<String, Object> properties;

    public ToolInputs() {
        this.properties = new HashMap<>();
    }

    public void put(String key, Object value) {
        properties.put(key, value);
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public void remove(String key) {
        properties.remove(key);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
