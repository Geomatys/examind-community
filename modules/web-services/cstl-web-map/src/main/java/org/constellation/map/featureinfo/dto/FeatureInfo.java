package org.constellation.map.featureinfo.dto;

import org.opengis.feature.Feature;

public class FeatureInfo implements LayerInfo {
    private final String layer;

    private final Feature feature;

    public FeatureInfo(String layer, Feature feature) {
        this.layer = layer;
        this.feature = feature;
    }

    public String getLayer() {
        return layer;
    }

    public Feature getFeature() {
        return feature;
    }
}
