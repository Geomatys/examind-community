package com.examind.ogc.api.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * @author Quentin BIALOTA
 */
@XmlRootElement(name = "Spatial")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpatialCRS {

    @JsonProperty("bbox")
    @XmlTransient
    private double[][] bbox;

    public double[][] getBbox() {
        return bbox;
    }

    public void setBbox(double[][] bbox) {
        this.bbox = bbox;
    }

    @JsonIgnore
    @XmlTransient
    private String lowerCorner;

    @JsonIgnore
    @XmlTransient
    private String upperCorner;

    @XmlElement(name = "LowerCorner")
    public String getLowerCorner() {
        if (bbox != null && bbox.length > 0 && bbox[0].length > 1) {
            if (bbox[0].length == 4) {
                return bbox[0][0] + " " + bbox[0][1];
            } else if (bbox[0].length == 6) {
                return bbox[0][0] + " " + bbox[0][1] + " " + bbox[0][2];
            }
        }
        return null;
    }

    @XmlElement(name = "UpperCorner")
    public String getUpperCorner() {
        if (bbox != null && bbox.length > 0 && bbox[0].length > 1) {
            if (bbox[0].length == 4) {
                return bbox[0][2] + " " + bbox[0][3];
            } else if (bbox[0].length == 6) {
                return bbox[0][3] + " " + bbox[0][4] + " " + bbox[0][5];
            }
        }
        return null;
    }
}
