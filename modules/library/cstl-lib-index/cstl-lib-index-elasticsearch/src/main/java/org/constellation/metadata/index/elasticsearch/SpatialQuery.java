/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

package org.constellation.metadata.index.elasticsearch;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.geotoolkit.index.LogicalFilterType;
import org.opengis.filter.Filter;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SpatialQuery implements org.geotoolkit.index.SpatialQuery {
    
    private String query;
    
    private XContentBuilder filter;
    
    private Sort sort;
    
    @Deprecated
    public SpatialQuery(String query, XContentBuilder filter, LogicalFilterType logical) {
        this.query  = query;
        this.filter = filter;
    }
    
    public SpatialQuery(String query, XContentBuilder filter) {
        this.query  = query;
        this.filter = filter;
    }
    
    public SpatialQuery(XContentBuilder filter) {
        this.filter = filter;
    }
    
    @Override
    public String getTextQuery() {
        return query;
    }
    
    @Override
    public Object getQuery() {
        return filter;
    }
    
    @Override
    public Sort getSort() {
        return sort;
    }
    
    public void setSort(String fieldName, String order) {
        this.sort = new Sort(fieldName, order);
    }

    @Override
    public void setSort(String fieldName, boolean desc, Character fieldType) {
        final String order = desc ? "DESC" : "ASC";
        this.sort = new Sort(fieldName, order);
    }
    
    
}
