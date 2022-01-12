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
package org.constellation.database.api.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.constellation.database.api.jooq.Admin;
import org.constellation.database.api.jooq.Keys;
import org.constellation.database.api.jooq.tables.records.DatasourcePathStoreRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.datasource_path_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathStore extends TableImpl<DatasourcePathStoreRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource_path_store</code>
     */
    public static final DatasourcePathStore DATASOURCE_PATH_STORE = new DatasourcePathStore();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourcePathStoreRecord> getRecordType() {
        return DatasourcePathStoreRecord.class;
    }

    /**
     * The column <code>admin.datasource_path_store.datasource_id</code>.
     */
    public final TableField<DatasourcePathStoreRecord, Integer> DATASOURCE_ID = createField(DSL.name("datasource_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path_store.path</code>.
     */
    public final TableField<DatasourcePathStoreRecord, String> PATH = createField(DSL.name("path"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path_store.store</code>.
     */
    public final TableField<DatasourcePathStoreRecord, String> STORE = createField(DSL.name("store"), SQLDataType.VARCHAR(500).nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path_store.type</code>.
     */
    public final TableField<DatasourcePathStoreRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR.nullable(false), this, "");

    private DatasourcePathStore(Name alias, Table<DatasourcePathStoreRecord> aliased) {
        this(alias, aliased, null);
    }

    private DatasourcePathStore(Name alias, Table<DatasourcePathStoreRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource_path_store</code> table reference
     */
    public DatasourcePathStore(String alias) {
        this(DSL.name(alias), DATASOURCE_PATH_STORE);
    }

    /**
     * Create an aliased <code>admin.datasource_path_store</code> table reference
     */
    public DatasourcePathStore(Name alias) {
        this(alias, DATASOURCE_PATH_STORE);
    }

    /**
     * Create a <code>admin.datasource_path_store</code> table reference
     */
    public DatasourcePathStore() {
        this(DSL.name("datasource_path_store"), null);
    }

    public <O extends Record> DatasourcePathStore(Table<O> child, ForeignKey<O, DatasourcePathStoreRecord> key) {
        super(child, key, DATASOURCE_PATH_STORE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<DatasourcePathStoreRecord> getPrimaryKey() {
        return Keys.DATASOURCE_PATH_STORE_PK;
    }

    @Override
    public List<UniqueKey<DatasourcePathStoreRecord>> getKeys() {
        return Arrays.<UniqueKey<DatasourcePathStoreRecord>>asList(Keys.DATASOURCE_PATH_STORE_PK);
    }

    @Override
    public List<ForeignKey<DatasourcePathStoreRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<DatasourcePathStoreRecord, ?>>asList(Keys.DATASOURCE_PATH_STORE__DATASOURCE_PATH_STORE_PATH_FK);
    }

    private transient DatasourcePath _datasourcePath;

    public DatasourcePath datasourcePath() {
        if (_datasourcePath == null)
            _datasourcePath = new DatasourcePath(this, Keys.DATASOURCE_PATH_STORE__DATASOURCE_PATH_STORE_PATH_FK);

        return _datasourcePath;
    }

    @Override
    public DatasourcePathStore as(String alias) {
        return new DatasourcePathStore(DSL.name(alias), this);
    }

    @Override
    public DatasourcePathStore as(Name alias) {
        return new DatasourcePathStore(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourcePathStore rename(String name) {
        return new DatasourcePathStore(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourcePathStore rename(Name name) {
        return new DatasourcePathStore(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
