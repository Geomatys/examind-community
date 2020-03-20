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
package org.constellation.provider.metadatastore;

import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.storage.DataStore;

import org.geotoolkit.metadata.MetadataStore;

import org.constellation.api.DataType;
import org.constellation.dto.DataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.MetadataData;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultMetadataData extends AbstractData implements MetadataData {

    private final MetadataStore store;

    private final Node metadata;

    public DefaultMetadataData(GenericName name, MetadataStore store, final Node metadata) {
        super(name, null);
        this.store = store;
        this.metadata = metadata;
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        return null; // TODO extract from ISO metadata
    }

    @Override
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
    }

    @Override
    public DataStore getStore() {
        return store;
    }

    @Override
    public DataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        return null;
    }

    @Override
    public Node getMetadata() {
        return metadata;
    }

    @Override
    public DataType getDataType() {
        return DataType.METADATA;
    }

    public String getResourceCRSName() throws ConstellationStoreException {
        return null; // TODO extract from metadata object?
    }
}
