package org.constellation.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataSummary implements Serializable{

    private Integer id;
    private String name;
    private Integer datasetId;
    private Date date;
    private String type;
    private String subtype;
    private boolean sensorable;
    private String owner;
    private List<String> targetSensor = new ArrayList<>(0);
    private DataDescription dataDescription;
    private String pyramidConformProviderId;
    private List<Dimension> dimensions;

    public List<Dimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<Dimension> dimensions) {
        this.dimensions = dimensions;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public boolean isSensorable() {
        return sensorable;
    }

    public void setSensorable(boolean sensorable) {
        this.sensorable = sensorable;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getTargetSensor() {
        return targetSensor;
    }

    public void setTargetSensor(List<String> targetSensor) {
        this.targetSensor = targetSensor;
    }

    public DataDescription getDataDescription() {
        return dataDescription;
    }

    public void setDataDescription(DataDescription dataDescription) {
        this.dataDescription = dataDescription;
    }

    public String getPyramidConformProviderId() {
        return pyramidConformProviderId;
    }

    public void setPyramidConformProviderId(String pyramidConformProviderId) {
        this.pyramidConformProviderId = pyramidConformProviderId;
    }
}
