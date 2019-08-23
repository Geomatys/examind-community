/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.dto.service.config.sos;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ObservationFilter {

    private String sensorID;

    private List<String> observedProperty;

    private List<String> foi;

    private Date start;

    private Date end;

    private int width = 840;


    public ObservationFilter() {

    }

    public ObservationFilter(String sensorID, List<String> observedProperty, List<String> foi, Date start, Date end, int width) {
        this.sensorID = sensorID;
        this.observedProperty = observedProperty;
        this.foi = foi;
        this.start = start;
        this.end = end;
        this.width = width;
    }

    /**
     * @return the sensorID
     */
    public String getSensorID() {
        return sensorID;
    }

    /**
     * @param sensorID the sensorID to set
     */
    public void setSensorID(String sensorID) {
        this.sensorID = sensorID;
    }

    /**
     * @return the start
     */
    public Date getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * @return the observedProperty
     */
    public List<String> getObservedProperty() {
        if (observedProperty == null) {
            observedProperty = new ArrayList<>();
        }
        return observedProperty;
    }

    /**
     * @param observedProperty the observedProperty to set
     */
    public void setObservedProperty(List<String> observedProperty) {
        this.observedProperty = observedProperty;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the foi
     */
    public List<String> getFoi() {
        if (foi == null) {
            foi = new ArrayList<>();
        }
        return foi;
    }

    /**
     * @param foi the foi to set
     */
    public void setFoi(List<String> foi) {
        this.foi = foi;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[Observation Filter]\n");
        sb.append("sensorID:").append(sensorID).append("\n");
        sb.append("observedProperties:\n");
        if (observedProperty != null) {
            for (String op : observedProperty) {
                sb.append(op).append("\n");
            }
        }
        sb.append("foi:\n");
        if (foi != null) {
            for (String op : foi) {
                sb.append(op).append("\n");
            }
        }
        if (start != null) {
            sb.append("start:").append(start).append("\n");
        }
        if (end != null) {
            sb.append("end:").append(end).append("\n");
        }
        sb.append("width:").append(width).append("\n");
        return sb.toString();
    }

}
