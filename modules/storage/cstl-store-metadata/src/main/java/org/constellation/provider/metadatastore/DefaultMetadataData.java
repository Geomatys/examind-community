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

import org.opengis.util.GenericName;

import org.geotoolkit.metadata.MetadataStore;

import org.constellation.api.DataType;
import org.constellation.provider.AbstractData;
import org.constellation.provider.MetadataData;
import org.w3c.dom.Node;

/**
 * TODO extract envelope/crs from metadata object
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultMetadataData extends AbstractData implements MetadataData {

    private final Node metadata;

    public DefaultMetadataData(GenericName name, MetadataStore store, final Node metadata) {
        super(name, null, store);
        this.metadata = metadata;
    }

    @Override
    public Node getMetadata() {
        return metadata;
    }

    @Override
    public DataType getDataType() {
        return DataType.METADATA;
    }
}
