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

import java.util.logging.Level;
import org.apache.sis.cql.CQLException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.Resource;

import static org.constellation.provider.AbstractData.LOGGER;
import org.geotoolkit.cql.CQL;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class DefaultGeoData<T extends Resource> extends AbstractData<T> implements GeoData<T> {

    public DefaultGeoData(GenericName name, T origin) {
        super(name, origin);
    }

    protected Filter buildCQLFilter(final String cql, final Filter filter) {
        final FilterFactory2 factory = (FilterFactory2) DefaultFactories.forBuildin(FilterFactory.class);
        try {
            final Filter cqlfilter = CQL.parseFilter(cql);
            if (filter != null) {
                return factory.and(cqlfilter, filter);
            } else {
                return cqlfilter;
            }
        } catch (CQLException ex) {
            LOGGER.log(Level.INFO,  ex.getMessage(),ex);
        }
        return filter;
    }

    protected Filter buildDimFilter(final String dimName, final String dimValue, final Filter filter) {
        final FilterFactory2 factory = (FilterFactory2) DefaultFactories.forBuildin(FilterFactory.class);
        Object value = dimValue;
        try {
            value = Double.parseDouble(dimValue);
        } catch (NumberFormatException ex) {
            // not a number
        }
        final Filter extraDimFilter = factory.equals(factory.property(dimName), factory.literal(value));
        if (filter != null) {
            return factory.and(extraDimFilter, filter);
        } else {
            return extraDimFilter;
        }
    }

}
