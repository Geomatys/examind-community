package org.constellation.dto;

import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerSummary;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MapContextStyledLayerDTO extends LayerSummary implements Comparable<MapContextStyledLayerDTO> {
    private Integer mapcontextId;
    private Integer layerId;
    private String serviceIdentifier;
    private String serviceVersions;
    private Integer styleId;
    private String styleName;
    private int order;
    private int opacity;
    private boolean visible;
    private boolean iswms;
    private String externalStyle;
    private String externalServiceUrl;
    private String externalServiceVersion;
    private String externalLayer;
    private String externalLayerExtent;

    /**
     * Default constructor needed by jackson when dealing with json.
     */
    public MapContextStyledLayerDTO(){
        super();
    }

    public MapContextStyledLayerDTO(Integer id,
		Integer mapcontextId,
		Integer layerId,
		Integer styleId,
		Integer layerOrder,
		Integer layerOpacity,
		Boolean layerVisible,
		String  externalLayer,
		String  externalLayerExtent,
		String  externalServiceUrl,
		String  externalServiceVersion,
		String  externalStyle,
		Boolean iswms,
		Integer dataId,
                String layerName,
                String layerNamespace,
                String layerAlias,
                Integer serviceID,
                Date date,
                String layerConfig,
                Integer ownerId,
                String layerTitle,
                String dataType,
                String dataSubType,
                String dataOwner,
                String dataProvider,
                Integer dataProviderID,
                final List<StyleBrief> targetStyles) {
            super(layerId, layerName, layerNamespace, layerAlias, serviceID, dataId, date, layerConfig, ownerId, dataOwner, dataType, dataSubType, dataOwner, dataProvider, dataProviderID, targetStyles);
            this.id = id;
            this.mapcontextId = mapcontextId;
            this.layerId = layerId;
            this.styleId = styleId;
            this.order = layerOrder;
            this.opacity = layerOpacity;
            this.visible = layerVisible;
            this.externalStyle = externalStyle;
            this.externalServiceUrl = externalServiceUrl;
            this.externalServiceVersion = externalServiceVersion;
            this.externalLayer = externalLayer;
            this.externalLayerExtent = externalLayerExtent;
            this.dataId = dataId;
            this.iswms = iswms;

            if (externalLayer != null) {
                super.setName(externalLayer);
                super.setAlias(externalLayer);
            }
            if (externalStyle != null) {
                final StyleBrief style = new StyleBrief();
                style.setName(externalStyle);
                style.setTitle(externalStyle);
                super.setTargetStyle(Collections.singletonList(style));
            }
    }

    public MapContextStyledLayerDTO(Integer id,
		Integer mapcontextId,
		Integer layerId,
		Integer styleId,
		Integer layerOrder,
		Integer layerOpacity,
		Boolean layerVisible,
		String  externalLayer,
		String  externalLayerExtent,
		String  externalServiceUrl,
		String  externalServiceVersion,
		String  externalStyle,
		Boolean iswms,
		Integer dataId,
                final Layer layer,
                final DataBrief db,
                final List<StyleBrief> layerStyles) {
        super(layer, db, layerStyles);
        this.id = id;
        this.mapcontextId = mapcontextId;
        this.layerId = layerId;
        this.styleId = styleId;
        this.order = layerOrder;
        this.opacity = layerOpacity;
        this.visible = layerVisible;
        this.externalStyle = externalStyle;
        this.externalServiceUrl = externalServiceUrl;
        this.externalServiceVersion = externalServiceVersion;
        this.externalLayer = externalLayer;
        this.externalLayerExtent = externalLayerExtent;
        this.dataId = dataId;
        this.iswms = iswms;

        if (externalLayer != null) {
                super.setName(externalLayer);
                super.setAlias(externalLayer);
        }
        if (externalStyle != null) {
            final StyleBrief style = new StyleBrief();
            style.setName(externalStyle);
            style.setTitle(externalStyle);
            super.setTargetStyle(Collections.singletonList(style));
        }
    }

    public Integer getMapcontextId() {
        return mapcontextId;
    }

    public void setMapcontextId(Integer mapcontextId) {
        this.mapcontextId = mapcontextId;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getExternalStyle() {
        return externalStyle;
    }

    public void setExternalStyle(String externalStyle) {
        this.externalStyle = externalStyle;
    }

    public String getExternalServiceUrl() {
        return externalServiceUrl;
    }

    public void setExternalServiceUrl(String externalServiceUrl) {
        this.externalServiceUrl = externalServiceUrl;
    }

    public String getExternalServiceVersion() {
        return externalServiceVersion;
    }

    public void setExternalServiceVersion(String externalServiceVersion) {
        this.externalServiceVersion = externalServiceVersion;
    }

    public String getExternalLayer() {
        return externalLayer;
    }

    public void setExternalLayer(String externalLayer) {
        this.externalLayer = externalLayer;
    }

    public String getExternalLayerExtent() {
        return externalLayerExtent;
    }

    public void setExternalLayerExtent(String externalLayerExtent) {
        this.externalLayerExtent = externalLayerExtent;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public String getServiceVersions() {
        return serviceVersions;
    }

    public void setServiceVersions(String serviceVersions) {
        this.serviceVersions = serviceVersions;
    }

    public boolean isIswms() {
        return iswms;
    }

    public void setIswms(boolean iswms) {
        this.iswms = iswms;
    }

    @Override
    public int compareTo(MapContextStyledLayerDTO o) {
        return getOrder() - o.getOrder();
    }
}