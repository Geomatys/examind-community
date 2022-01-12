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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.DatasourcePath;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathRecord extends UpdatableRecordImpl<DatasourcePathRecord> implements Record6<Integer, String, String, Boolean, String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource_path.datasource_id</code>.
     */
    public DatasourcePathRecord setDatasourceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource_path.path</code>.
     */
    public DatasourcePathRecord setPath(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.path</code>.
     */
    @NotNull
    public String getPath() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource_path.name</code>.
     */
    public DatasourcePathRecord setName(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.name</code>.
     */
    @NotNull
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.datasource_path.folder</code>.
     */
    public DatasourcePathRecord setFolder(Boolean value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.folder</code>.
     */
    @NotNull
    public Boolean getFolder() {
        return (Boolean) get(3);
    }

    /**
     * Setter for <code>admin.datasource_path.parent_path</code>.
     */
    public DatasourcePathRecord setParentPath(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.parent_path</code>.
     */
    public String getParentPath() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.datasource_path.size</code>.
     */
    public DatasourcePathRecord setSize(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.size</code>.
     */
    @NotNull
    public Integer getSize() {
        return (Integer) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, String, Boolean, String, Integer> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, String, String, Boolean, String, Integer> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DatasourcePath.DATASOURCE_PATH.DATASOURCE_ID;
    }

    @Override
    public Field<String> field2() {
        return DatasourcePath.DATASOURCE_PATH.PATH;
    }

    @Override
    public Field<String> field3() {
        return DatasourcePath.DATASOURCE_PATH.NAME;
    }

    @Override
    public Field<Boolean> field4() {
        return DatasourcePath.DATASOURCE_PATH.FOLDER;
    }

    @Override
    public Field<String> field5() {
        return DatasourcePath.DATASOURCE_PATH.PARENT_PATH;
    }

    @Override
    public Field<Integer> field6() {
        return DatasourcePath.DATASOURCE_PATH.SIZE;
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
        return getName();
    }

    @Override
    public Boolean component4() {
        return getFolder();
    }

    @Override
    public String component5() {
        return getParentPath();
    }

    @Override
    public Integer component6() {
        return getSize();
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
        return getName();
    }

    @Override
    public Boolean value4() {
        return getFolder();
    }

    @Override
    public String value5() {
        return getParentPath();
    }

    @Override
    public Integer value6() {
        return getSize();
    }

    @Override
    public DatasourcePathRecord value1(Integer value) {
        setDatasourceId(value);
        return this;
    }

    @Override
    public DatasourcePathRecord value2(String value) {
        setPath(value);
        return this;
    }

    @Override
    public DatasourcePathRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public DatasourcePathRecord value4(Boolean value) {
        setFolder(value);
        return this;
    }

    @Override
    public DatasourcePathRecord value5(String value) {
        setParentPath(value);
        return this;
    }

    @Override
    public DatasourcePathRecord value6(Integer value) {
        setSize(value);
        return this;
    }

    @Override
    public DatasourcePathRecord values(Integer value1, String value2, String value3, Boolean value4, String value5, Integer value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasourcePathRecord
     */
    public DatasourcePathRecord() {
        super(DatasourcePath.DATASOURCE_PATH);
    }

    /**
     * Create a detached, initialised DatasourcePathRecord
     */
    public DatasourcePathRecord(Integer datasourceId, String path, String name, Boolean folder, String parentPath, Integer size) {
        super(DatasourcePath.DATASOURCE_PATH);

        setDatasourceId(datasourceId);
        setPath(path);
        setName(name);
        setFolder(folder);
        setParentPath(parentPath);
        setSize(size);
    }
}
