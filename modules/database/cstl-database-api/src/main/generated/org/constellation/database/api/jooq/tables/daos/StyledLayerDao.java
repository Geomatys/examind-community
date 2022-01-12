/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.database.api.jooq.tables.daos;


import java.util.List;

import org.constellation.database.api.jooq.tables.StyledLayer;
import org.constellation.database.api.jooq.tables.records.StyledLayerRecord;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledLayerDao extends DAOImpl<StyledLayerRecord, org.constellation.database.api.jooq.tables.pojos.StyledLayer, Record2<Integer, Integer>> {

    /**
     * Create a new StyledLayerDao without any configuration
     */
    public StyledLayerDao() {
        super(StyledLayer.STYLED_LAYER, org.constellation.database.api.jooq.tables.pojos.StyledLayer.class);
    }

    /**
     * Create a new StyledLayerDao with an attached configuration
     */
    public StyledLayerDao(Configuration configuration) {
        super(StyledLayer.STYLED_LAYER, org.constellation.database.api.jooq.tables.pojos.StyledLayer.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(org.constellation.database.api.jooq.tables.pojos.StyledLayer object) {
        return compositeKeyRecord(object.getStyle(), object.getLayer());
    }

    /**
     * Fetch records that have <code>style BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchRangeOfStyle(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(StyledLayer.STYLED_LAYER.STYLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>style IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchByStyle(Integer... values) {
        return fetch(StyledLayer.STYLED_LAYER.STYLE, values);
    }

    /**
     * Fetch records that have <code>layer BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchRangeOfLayer(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(StyledLayer.STYLED_LAYER.LAYER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>layer IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchByLayer(Integer... values) {
        return fetch(StyledLayer.STYLED_LAYER.LAYER, values);
    }

    /**
     * Fetch records that have <code>is_default BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchRangeOfIsDefault(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(StyledLayer.STYLED_LAYER.IS_DEFAULT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>is_default IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.StyledLayer> fetchByIsDefault(Boolean... values) {
        return fetch(StyledLayer.STYLED_LAYER.IS_DEFAULT, values);
    }
}
