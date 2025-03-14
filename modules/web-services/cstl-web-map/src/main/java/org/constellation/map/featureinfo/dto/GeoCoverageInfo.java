
package org.constellation.map.featureinfo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;

/**
 *
 * @author glegal
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoCoverageInfo implements LayerInfo {
    
    private final String layer;

    private final Map properties;
    
    private final GeoJSONGeometry geometry;
    
    public GeoCoverageInfo(String layer, Map properties, GeoJSONGeometry geometry) {
        this.layer = layer;
        this.properties = properties;
        this.geometry = geometry;
    }

    public String getLayer() {
        return layer;
    }

    public Map getProperties() {
        return properties;
    }

    public GeoJSONGeometry getGeometry() {
        return geometry;
    }

}
