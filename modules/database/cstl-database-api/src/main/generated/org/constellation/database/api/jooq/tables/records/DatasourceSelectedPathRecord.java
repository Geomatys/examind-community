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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.DatasourceSelectedPath;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource_selected_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceSelectedPathRecord extends UpdatableRecordImpl<DatasourceSelectedPathRecord> implements Record4<Integer, String, String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource_selected_path.datasource_id</code>.
     */
    public DatasourceSelectedPathRecord setDatasourceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource_selected_path.path</code>.
     */
    public DatasourceSelectedPathRecord setPath(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.path</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getPath() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource_selected_path.status</code>.
     */
    public DatasourceSelectedPathRecord setStatus(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.status</code>.
     */
    @Size(max = 50)
    public String getStatus() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.datasource_selected_path.provider_id</code>.
     */
    public DatasourceSelectedPathRecord setProviderId(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.provider_id</code>.
     */
    public Integer getProviderId() {
        return (Integer) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, Integer> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, String, Integer> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.DATASOURCE_ID;
    }

    @Override
    public Field<String> field2() {
        return DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PATH;
    }

    @Override
    public Field<String> field3() {
        return DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.STATUS;
    }

    @Override
    public Field<Integer> field4() {
        return DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PROVIDER_ID;
    }

    @Override
    public Integer component1() {
        return getDatasourceId();
    }

    @Override
    public String component2() {
        return getPath();
    }

    @Override
    public String component3() {
        return getStatus();
    }

    @Override
    public Integer component4() {
        return getProviderId();
    }

    @Override
    public Integer value1() {
        return getDatasourceId();
    }

    @Override
    public String value2() {
        return getPath();
    }

    @Override
    public String value3() {
        return getStatus();
    }

    @Override
    public Integer value4() {
        return getProviderId();
    }

    @Override
    public DatasourceSelectedPathRecord value1(Integer value) {
        setDatasourceId(value);
        return this;
    }

    @Override
    public DatasourceSelectedPathRecord value2(String value) {
        setPath(value);
        return this;
    }

    @Override
    public DatasourceSelectedPathRecord value3(String value) {
        setStatus(value);
        return this;
    }

    @Override
    public DatasourceSelectedPathRecord value4(Integer value) {
        setProviderId(value);
        return this;
    }

    @Override
    public DatasourceSelectedPathRecord values(Integer value1, String value2, String value3, Integer value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasourceSelectedPathRecord
     */
    public DatasourceSelectedPathRecord() {
        super(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH);
    }

    /**
     * Create a detached, initialised DatasourceSelectedPathRecord
     */
    public DatasourceSelectedPathRecord(Integer datasourceId, String path, String status, Integer providerId) {
        super(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH);

        setDatasourceId(datasourceId);
        setPath(path);
        setStatus(status);
        setProviderId(providerId);
    }
}
