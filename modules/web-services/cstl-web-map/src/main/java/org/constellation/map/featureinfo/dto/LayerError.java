package org.constellation.map.featureinfo.dto;

public class LayerError implements LayerInfo {

    private String layer;
    private Exception error;

    public String getLayer() {
        return layer;
    }

    public LayerError setLayer(String layer) {
        this.layer = layer;
        return this;
    }

    public Exception getError() {
        return error;
    }

    public LayerError setError(Exception error) {
        this.error = error;
        return this;
    }
}
