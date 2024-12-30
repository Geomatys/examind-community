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
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.LayerRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function10;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row10;
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
 * Generated DAO object for table admin.layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Layer extends TableImpl<LayerRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.layer</code>
     */
    public static final Layer LAYER = new Layer();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<LayerRecord> getRecordType() {
        return LayerRecord.class;
    }

    /**
     * The column <code>admin.layer.id</code>.
     */
    public final TableField<LayerRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.layer.name</code>.
     */
    public final TableField<LayerRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.layer.namespace</code>.
     */
    public final TableField<LayerRecord, String> NAMESPACE = createField(DSL.name("namespace"), SQLDataType.VARCHAR(256), this, "");

    /**
     * The column <code>admin.layer.alias</code>.
     */
    public final TableField<LayerRecord, String> ALIAS = createField(DSL.name("alias"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.layer.service</code>.
     */
    public final TableField<LayerRecord, Integer> SERVICE = createField(DSL.name("service"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.layer.data</code>.
     */
    public final TableField<LayerRecord, Integer> DATA = createField(DSL.name("data"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.layer.date</code>.
     */
    public final TableField<LayerRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.layer.config</code>.
     */
    public final TableField<LayerRecord, String> CONFIG = createField(DSL.name("config"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.layer.owner</code>.
     */
    public final TableField<LayerRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.layer.title</code>.
     */
    public final TableField<LayerRecord, String> TITLE = createField(DSL.name("title"), SQLDataType.CLOB, this, "");

    private Layer(Name alias, Table<LayerRecord> aliased) {
        this(alias, aliased, null);
    }

    private Layer(Name alias, Table<LayerRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.layer</code> table reference
     */
    public Layer(String alias) {
        this(DSL.name(alias), LAYER);
    }

    /**
     * Create an aliased <code>admin.layer</code> table reference
     */
    public Layer(Name alias) {
        this(alias, LAYER);
    }

    /**
     * Create a <code>admin.layer</code> table reference
     */
    public Layer() {
        this(DSL.name("layer"), null);
    }

    public <O extends Record> Layer(Table<O> child, ForeignKey<O, LayerRecord> key) {
        super(child, key, LAYER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.LAYER_ALIAS_SERVICE_IDX, Indexes.LAYER_DATA_IDX, Indexes.LAYER_NAME_SERVICE_IDX, Indexes.LAYER_OWNER_IDX, Indexes.LAYER_SERVICE_IDX);
    }

    @Override
    public Identity<LayerRecord, Integer> getIdentity() {
        return (Identity<LayerRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<LayerRecord> getPrimaryKey() {
        return Keys.LAYER_PK;
    }

    @Override
    public List<UniqueKey<LayerRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.LAYER_NAME_UQ, Keys.LAYER_UNIQUE_ALIAS);
    }

    @Override
    public List<ForeignKey<LayerRecord, ?>> getReferences() {
        return Arrays.asList(Keys.LAYER__LAYER_SERVICE_FK, Keys.LAYER__LAYER_DATA_FK, Keys.LAYER__LAYER_OWNER_FK);
    }

    private transient Service _service;
    private transient Data _data;
    private transient CstlUser _cstlUser;

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.LAYER__LAYER_SERVICE_FK);

        return _service;
    }

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.LAYER__LAYER_DATA_FK);

        return _data;
    }

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.LAYER__LAYER_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Layer as(String alias) {
        return new Layer(DSL.name(alias), this);
    }

    @Override
    public Layer as(Name alias) {
        return new Layer(alias, this);
    }

    @Override
    public Layer as(Table<?> alias) {
        return new Layer(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Layer rename(String name) {
        return new Layer(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Layer rename(Name name) {
        return new Layer(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Layer rename(Table<?> name) {
        return new Layer(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, String, String, String, Integer, Integer, Long, String, Integer, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function10<? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super Integer, ? super Long, ? super String, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function10<? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super Integer, ? super Long, ? super String, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
