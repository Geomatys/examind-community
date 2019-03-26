/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.dto.service.config.wxs;

import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.Filter;
import org.constellation.dto.service.config.wxs.DimensionDefinition;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FilterAndDimension {

    private Filter filter;

    private List<DimensionDefinition> dimensions;

    public FilterAndDimension() {

    }

    public FilterAndDimension(Filter filter, List<DimensionDefinition> dimensions) {
        this.dimensions = dimensions;
        this.filter = filter;
    }

    /**
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * @return the dimensions
     */
    public List<DimensionDefinition> getDimensions() {
        if (dimensions == null) {
            dimensions = new ArrayList<>();
        }
        return dimensions;
    }

    /**
     * @param dimensions the dimensions to set
     */
    public void setDimensions(List<DimensionDefinition> dimensions) {
        this.dimensions = dimensions;
    }
}
