/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.ws;

import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.constellation.api.DataType;
import org.constellation.api.ServiceDef;
import org.constellation.dto.DimensionRange;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.StyleReference;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LayerCache {
    
    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws");

    private final NameInProvider nip;
    private final GenericName name;
    private final List<StyleReference> styles;
    private final Data data;
    private final LayerConfig configuration;

    public LayerCache(final NameInProvider nip, GenericName name, Data d, List<StyleReference> styles, final LayerConfig configuration) {
        this.nip = nip;
        this.data = d;
        this.name = name;
        this.styles = styles;
        this.configuration = configuration;
    }

    public Integer getId() {
        return nip.layerId;
    }

    public GenericName getName() {
        return name;
    }

    public List<StyleReference> getStyles() {
        return styles;
    }

    public Data getData() {
        return data;
    }

    public LayerConfig getConfiguration() {
        return configuration;
    }

    public Envelope getEnvelope() throws ConstellationStoreException {
        return data.getEnvelope();
    }
    
    public GeographicBoundingBox getGeographicBoundingBox() throws ConstellationStoreException {
        try {
            final Envelope env = getEnvelope();
            if (env != null) {
                final DefaultGeographicBoundingBox result = new DefaultGeographicBoundingBox();
                result.setBounds(env);
                return result;
            } else {
                LOGGER.warning("Null boundingBox for Layer:" + name + ". Returning World BBOX.");
                return new DefaultGeographicBoundingBox(-180, 180, -90, 90);
            }
        } catch (TransformException ex) {
            throw new ConstellationStoreException(ex);
        }
    }
    
    public CoordinateReferenceSystem getCoordinateReferenceSystem() throws ConstellationStoreException {
        return data.getEnvelope().getCoordinateReferenceSystem();
    }

    public Number getFirstElevation() throws ConstellationStoreException {
        final SortedSet<Number> elevations = data.getAvailableElevations();
        if (!elevations.isEmpty()) {
            return elevations.first();
        }
        return null;
    }

    public boolean isQueryable(ServiceDef.Query query) {
        return data.isQueryable(query);
    }
    
    public DataType getDataType() {
        return data.getDataType();
    }
    
    public SortedSet<Number> getAvailableElevations() throws ConstellationStoreException {
        return data.getAvailableElevations();
    }
    
    public SortedSet<Date> getAvailableTimes() throws ConstellationStoreException {
        return data.getAvailableTimes();
    }
    
    public SortedSet<Date> getDateRange() throws ConstellationStoreException {
        return data.getDateRange();
    }
    
    public SortedSet<DimensionRange> getSampleValueRanges() throws ConstellationStoreException {
        return data.getSampleValueRanges();
    }
}
