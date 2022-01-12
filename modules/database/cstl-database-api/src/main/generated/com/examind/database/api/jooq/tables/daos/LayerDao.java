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
package com.examind.database.api.jooq.tables.daos;


import com.examind.database.api.jooq.tables.Layer;
import com.examind.database.api.jooq.tables.records.LayerRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class LayerDao extends DAOImpl<LayerRecord, com.examind.database.api.jooq.tables.pojos.Layer, Integer> {

    /**
     * Create a new LayerDao without any configuration
     */
    public LayerDao() {
        super(Layer.LAYER, com.examind.database.api.jooq.tables.pojos.Layer.class);
    }

    /**
     * Create a new LayerDao with an attached configuration
     */
    public LayerDao(Configuration configuration) {
        super(Layer.LAYER, com.examind.database.api.jooq.tables.pojos.Layer.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Layer object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Layer.LAYER.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchById(Integer... values) {
        return fetch(Layer.LAYER.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Layer fetchOneById(Integer value) {
        return fetchOne(Layer.LAYER.ID, value);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Layer.LAYER.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByName(String... values) {
        return fetch(Layer.LAYER.NAME, values);
    }

    /**
     * Fetch records that have <code>namespace BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfNamespace(String lowerInclusive, String upperInclusive) {
        return fetchRange(Layer.LAYER.NAMESPACE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>namespace IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByNamespace(String... values) {
        return fetch(Layer.LAYER.NAMESPACE, values);
    }

    /**
     * Fetch records that have <code>alias BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfAlias(String lowerInclusive, String upperInclusive) {
        return fetchRange(Layer.LAYER.ALIAS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>alias IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByAlias(String... values) {
        return fetch(Layer.LAYER.ALIAS, values);
    }

    /**
     * Fetch records that have <code>service BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfService(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Layer.LAYER.SERVICE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>service IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByService(Integer... values) {
        return fetch(Layer.LAYER.SERVICE, values);
    }

    /**
     * Fetch records that have <code>data BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfData(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Layer.LAYER.DATA, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>data IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByData(Integer... values) {
        return fetch(Layer.LAYER.DATA, values);
    }

    /**
     * Fetch records that have <code>date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Layer.LAYER.DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByDate(Long... values) {
        return fetch(Layer.LAYER.DATE, values);
    }

    /**
     * Fetch records that have <code>config BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfConfig(String lowerInclusive, String upperInclusive) {
        return fetchRange(Layer.LAYER.CONFIG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>config IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByConfig(String... values) {
        return fetch(Layer.LAYER.CONFIG, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Layer.LAYER.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByOwner(Integer... values) {
        return fetch(Layer.LAYER.OWNER, values);
    }

    /**
     * Fetch records that have <code>title BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchRangeOfTitle(String lowerInclusive, String upperInclusive) {
        return fetchRange(Layer.LAYER.TITLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>title IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Layer> fetchByTitle(String... values) {
        return fetch(Layer.LAYER.TITLE, values);
    }
}
