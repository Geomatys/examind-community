
package org.constellation.map.featureinfo.dto;

import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureCollectionInfo implements LayerInfo {
    private final List<LayerInfo> features;
        
    public FeatureCollectionInfo(List<LayerInfo> features) {
        this.features = features;
    }

    public List<LayerInfo> getFeatures() {
        return features;
    }
}
