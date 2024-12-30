/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.DatasourceStoreRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.datasource_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourceStore extends TableImpl<DatasourceStoreRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource_store</code>
     */
    public static final DatasourceStore DATASOURCE_STORE = new DatasourceStore();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourceStoreRecord> getRecordType() {
        return DatasourceStoreRecord.class;
    }

    /**
     * The column <code>admin.datasource_store.datasource_id</code>.
     */
    public final TableField<DatasourceStoreRecord, Integer> DATASOURCE_ID = createField(DSL.name("datasource_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_store.store</code>.
     */
    public final TableField<DatasourceStoreRecord, String> STORE = createField(DSL.name("store"), SQLDataType.VARCHAR(500).nullable(false), this, "");

    /**
     * The column <code>admin.datasource_store.type</code>.
     */
    public final TableField<DatasourceStoreRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR.nullable(false), this, "");

    private DatasourceStore(Name alias, Table<DatasourceStoreRecord> aliased) {
        this(alias, aliased, null);
    }

    private DatasourceStore(Name alias, Table<DatasourceStoreRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource_store</code> table reference
     */
    public DatasourceStore(String alias) {
        this(DSL.name(alias), DATASOURCE_STORE);
    }

    /**
     * Create an aliased <code>admin.datasource_store</code> table reference
     */
    public DatasourceStore(Name alias) {
        this(alias, DATASOURCE_STORE);
    }

    /**
     * Create a <code>admin.datasource_store</code> table reference
     */
    public DatasourceStore() {
        this(DSL.name("datasource_store"), null);
    }

    public <O extends Record> DatasourceStore(Table<O> child, ForeignKey<O, DatasourceStoreRecord> key) {
        super(child, key, DATASOURCE_STORE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DatasourceStoreRecord> getPrimaryKey() {
        return Keys.DATASOURCE_STORE_PK;
    }

    @Override
    public List<ForeignKey<DatasourceStoreRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATASOURCE_STORE__DATASOURCE_STORE_DATASOURCE_ID_FK);
    }

    private transient Datasource _datasource;

    /**
     * Get the implicit join path to the <code>admin.datasource</code> table.
     */
    public Datasource datasource() {
        if (_datasource == null)
            _datasource = new Datasource(this, Keys.DATASOURCE_STORE__DATASOURCE_STORE_DATASOURCE_ID_FK);

        return _datasource;
    }

    @Override
    public DatasourceStore as(String alias) {
        return new DatasourceStore(DSL.name(alias), this);
    }

    @Override
    public DatasourceStore as(Name alias) {
        return new DatasourceStore(alias, this);
    }

    @Override
    public DatasourceStore as(Table<?> alias) {
        return new DatasourceStore(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceStore rename(String name) {
        return new DatasourceStore(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceStore rename(Name name) {
        return new DatasourceStore(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceStore rename(Table<?> name) {
        return new DatasourceStore(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
