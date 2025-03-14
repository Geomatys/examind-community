package org.constellation.map.featureinfo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Optional;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoFeatureInfo implements LayerInfo {
    private final String layer;

    private final Feature properties;
    
    private GeoJSONGeometry geometry;

    public GeoFeatureInfo(String layer, Feature feature) {
        this.layer = layer;
        this.properties = feature;
        final FeatureType type = feature.getType();
        Optional<PropertyType> defaultGeometryProp = FeatureExt.getDefaultGeometrySafe(type);
        if (defaultGeometryProp.isPresent()) {
            final Object value = feature.getPropertyValue(defaultGeometryProp.get().getName().toString());
            if (value instanceof Geometry geom) {
                geometry = GeoJSONGeometry.toGeoJSONGeometry(geom);
            }
        }
       
    }

    public String getLayer() {
        return layer;
    }

    public Feature getProperties() {
        return properties;
    }

    public GeoJSONGeometry getGeometry() {
        return geometry;
    }
}
