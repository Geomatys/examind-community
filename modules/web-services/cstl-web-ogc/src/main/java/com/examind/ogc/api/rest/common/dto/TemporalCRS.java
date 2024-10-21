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
@XmlRootElement(name = "Temporal")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemporalCRS {

    @XmlTransient
    @JsonProperty("interval")
    private String[][] interval;

    public String[][] getInterval() {
        return interval;
    }

    public void setInterval(String[][] interval) {
        this.interval = interval;
    }

    @JsonIgnore
    @XmlTransient
    private String begin;

    @JsonIgnore
    @XmlTransient
    private String end;

    @XmlElement(name = "begin")
    public String getBegin() {
        if (interval[0].length == 2) {
            return interval[0][0];
        }
        return null;
    }

    @XmlElement(name = "end")
    public String getEnd() {
        if (interval[0].length == 2) {
            return interval[0][1];
        }
        return null;
    }
}
