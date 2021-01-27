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
package org.constellation.provider;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.sis.cql.CQLException;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;

import static org.constellation.provider.AbstractData.LOGGER;

import org.apache.sis.util.collection.BackingStoreException;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.cql.CQL;
import org.apache.sis.portrayal.MapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.style.MutableStyle;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.Style;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class DefaultGeoData<T extends Resource> extends AbstractData<T> implements GeoData<T> {

    public DefaultGeoData(GenericName name, T origin) {
        super(name, origin);
    }

    protected Filter buildDimFilter(final String dimName, final String dimValue) {
        final FilterFactory2 factory = (FilterFactory2) DefaultFactories.forBuildin(FilterFactory.class);
        Object value = dimValue;
        try {
            value = Double.parseDouble(dimValue);
        } catch (NumberFormatException ex) {
            // not a number
            LOGGER.log(Level.FINER, "Received dimension value is not a number", ex);
        }

        return factory.equals(factory.property(dimName), factory.literal(value));
    }

    private Filter toFilter(Map.Entry param) {
        final Object rawKey = param.getKey();
        if (!(rawKey instanceof String)) return null;
        final String key = (String) rawKey;
        if (key.equalsIgnoreCase("cql_filter")) {
            final String cqlFilter = extractStringValue(param.getValue());
            if (cqlFilter != null) {
                try {
                    return CQL.parseFilter(cqlFilter);
                } catch (CQLException e) {
                    throw new BackingStoreException(e);
                }
            }
        } else if (key.startsWith("dim_") || key.startsWith("DIM_")) {
            final String dimValue = extractStringValue(param.getValue());
            final String dimName = key.substring(4);
            return buildDimFilter(dimName, dimValue);
        }
        return null;
    }

    private String extractStringValue(Object valueOrContainer) {
        if (valueOrContainer instanceof Iterable) {
            final Iterator it = ((Iterable) valueOrContainer).iterator();
            if (it.hasNext()) valueOrContainer = it.next();
        }

        if (valueOrContainer instanceof String) return (String) valueOrContainer;
        else return null;
    }

    protected Optional<Query> resolveQuery(final Map portrayParameters) {
        if (portrayParameters == null) return Optional.empty();
        final Object rawValues = portrayParameters.get(KEY_EXTRA_PARAMETERS);
        if (rawValues == null) return Optional.empty();
        if (!(rawValues instanceof Map)) throw new IllegalArgumentException(KEY_EXTRA_PARAMETERS+" parameter must be a Map");
        final Map<?, ?> extras = (Map) rawValues;
        return extras.entrySet().stream()
                .map(this::toFilter)
                .filter(Objects::nonNull)
                .reduce(new DefaultFilterFactory()::and)
                .map(filter-> {
                    final SimpleQuery query = new SimpleQuery();
                    query.setFilter(filter);
                    return query;
                });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final MapLayer getMapLayer(Style styleI, final Map<String, Object> params) throws ConstellationStoreException {
        MutableStyle style;
        if (styleI == null) {
            style = getDefaultStyle();
        } else if (styleI instanceof MutableStyle) {
            style = (MutableStyle) styleI;
        } else {
            throw new IllegalArgumentException("Only MutableStyle implementation is acepted");
        }
        final MapLayer layer = MapBuilder.createLayer(origin);
        if (style != null) layer.setStyle(style);
        
        final String title = getName().tip().toString();
        layer.setIdentifier(title);
        layer.setTitle(title);
        
        resolveQuery(params).ifPresent(query -> layer.setQuery(query));

        return layer;
    }
    
    protected abstract MutableStyle getDefaultStyle() throws ConstellationStoreException;
}
