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

/**
 *  A Spatial query use to perform search request on elasticsearch datasource.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class SpatialQuery implements org.geotoolkit.index.SpatialQuery {

    private String query;

    private XContentBuilder filter;

    private Sort sort;

    public SpatialQuery(String query, XContentBuilder filter) {
        this.query  = query;
        this.filter = filter;
    }

    public SpatialQuery(XContentBuilder filter) {
        this.filter = filter;
    }

    /**
     * Return the elasticsearch text query (Using elasticsearch syntax).
     */
    @Override
    public String getTextQuery() {
        return query;
    }

    /**
     * Return a elasticSearchQuery {@link Query} object.
     * can be {@code null}.
     */
    @Override
    public XContentBuilder getQuery() {
        return filter;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Sort getSort() {
        return sort;
    }

    public void setSort(String fieldName, String order) {
        this.sort = new Sort(fieldName, order);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setSort(String fieldName, boolean desc, Character fieldType) {
        final String order = desc ? "DESC" : "ASC";
        this.sort = new Sort(fieldName, order);
    }


}
