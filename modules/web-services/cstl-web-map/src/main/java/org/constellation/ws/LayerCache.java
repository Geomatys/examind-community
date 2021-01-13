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
import org.constellation.api.ServiceDef;
import org.constellation.dto.StyleReference;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.geotoolkit.util.DateRange;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LayerCache {

    private final Integer id;
    private final GenericName name;
    private final List<StyleReference> styles;
    private final Data data;
    private final Layer configuration;

    public LayerCache(final Integer id, GenericName name, Data d, List<StyleReference> styles, final Layer configuration) {
        this.id = id;
        this.data = d;
        this.name = name;
        this.styles = styles;
        this.configuration = configuration;
    }

    public Integer getId() {
        return id;
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

    public Layer getConfiguration() {
        return configuration;
    }

    public Date getFirstDate() throws ConstellationStoreException {
        final DateRange dates = data.getDateRange();
        if (dates != null) {
            return dates.getMinValue();
        }
        return null;
    }

    public Date getLastDate() throws ConstellationStoreException {
        final DateRange dates = data.getDateRange();
        if (dates != null) {
            return dates.getMaxValue();
        }
        return null;
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
}
