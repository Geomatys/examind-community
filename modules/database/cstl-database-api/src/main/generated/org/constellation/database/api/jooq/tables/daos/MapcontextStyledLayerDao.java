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

import org.constellation.database.api.jooq.tables.MapcontextStyledLayer;
import org.constellation.database.api.jooq.tables.records.MapcontextStyledLayerRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.mapcontext_styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MapcontextStyledLayerDao extends DAOImpl<MapcontextStyledLayerRecord, org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer, Integer> {

    /**
     * Create a new MapcontextStyledLayerDao without any configuration
     */
    public MapcontextStyledLayerDao() {
        super(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER, org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer.class);
    }

    /**
     * Create a new MapcontextStyledLayerDao with an attached configuration
     */
    public MapcontextStyledLayerDao(Configuration configuration) {
        super(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER, org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchById(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer fetchOneById(Integer value) {
        return fetchOne(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ID, value);
    }

    /**
     * Fetch records that have <code>mapcontext_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfMapcontextId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>mapcontext_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByMapcontextId(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID, values);
    }

    /**
     * Fetch records that have <code>layer_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfLayerId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>layer_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByLayerId(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ID, values);
    }

    /**
     * Fetch records that have <code>style_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfStyleId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.STYLE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>style_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByStyleId(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.STYLE_ID, values);
    }

    /**
     * Fetch records that have <code>layer_order BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfLayerOrder(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ORDER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>layer_order IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByLayerOrder(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ORDER, values);
    }

    /**
     * Fetch records that have <code>layer_opacity BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfLayerOpacity(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_OPACITY, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>layer_opacity IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByLayerOpacity(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_OPACITY, values);
    }

    /**
     * Fetch records that have <code>layer_visible BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfLayerVisible(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_VISIBLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>layer_visible IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByLayerVisible(Boolean... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_VISIBLE, values);
    }

    /**
     * Fetch records that have <code>external_layer BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfExternalLayer(String lowerInclusive, String upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>external_layer IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByExternalLayer(String... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER, values);
    }

    /**
     * Fetch records that have <code>external_layer_extent BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfExternalLayerExtent(String lowerInclusive, String upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER_EXTENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>external_layer_extent IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByExternalLayerExtent(String... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER_EXTENT, values);
    }

    /**
     * Fetch records that have <code>external_service_url BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfExternalServiceUrl(String lowerInclusive, String upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>external_service_url IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByExternalServiceUrl(String... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL, values);
    }

    /**
     * Fetch records that have <code>external_service_version BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfExternalServiceVersion(String lowerInclusive, String upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_VERSION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>external_service_version IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByExternalServiceVersion(String... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_VERSION, values);
    }

    /**
     * Fetch records that have <code>external_style BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfExternalStyle(String lowerInclusive, String upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>external_style IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByExternalStyle(String... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE, values);
    }

    /**
     * Fetch records that have <code>iswms BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfIswms(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ISWMS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>iswms IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByIswms(Boolean... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ISWMS, values);
    }

    /**
     * Fetch records that have <code>data_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchRangeOfDataId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.DATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>data_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.MapcontextStyledLayer> fetchByDataId(Integer... values) {
        return fetch(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.DATA_ID, values);
    }
}
