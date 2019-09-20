/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2018, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.provider.sensorstore;

import java.util.Collections;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.storage.DataStore;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.SensorData;
import org.geotoolkit.sensor.AbstractSensorStore;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLUtilities;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultSensorData extends AbstractData implements SensorData {

    private final AbstractSensorStore store;

    private final AbstractSensorML metadata;

    public DefaultSensorData(GenericName name, AbstractSensorStore store, final AbstractSensorML metadata) {
        super(name, Collections.EMPTY_LIST);
        this.store = store;
        this.metadata = metadata;
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        return null; // TODO extract from SML metadata
    }

    @Override
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
    }

    @Override
    public DataType getDataType() {
        return DataType.SENSOR;
    }

    @Override
    public DataStore getStore() {
        return store;
    }

    @Override
    public Object getSensorMetadata() {
        return metadata;
    }

    @Override
    public String getSensorMLType() {
        return SensorMLUtilities.getSensorMLType(metadata);
    }

    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        return null; // TODO extract from SML metadata
    }

}
