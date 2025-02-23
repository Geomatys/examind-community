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
import com.examind.database.api.jooq.tables.records.MapcontextRecord;

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
 * Generated DAO object for table admin.mapcontext
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Mapcontext extends TableImpl<MapcontextRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.mapcontext</code>
     */
    public static final Mapcontext MAPCONTEXT = new Mapcontext();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MapcontextRecord> getRecordType() {
        return MapcontextRecord.class;
    }

    /**
     * The column <code>admin.mapcontext.id</code>.
     */
    public final TableField<MapcontextRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.mapcontext.name</code>.
     */
    public final TableField<MapcontextRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.mapcontext.owner</code>.
     */
    public final TableField<MapcontextRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.mapcontext.description</code>.
     */
    public final TableField<MapcontextRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.mapcontext.crs</code>.
     */
    public final TableField<MapcontextRecord, String> CRS = createField(DSL.name("crs"), SQLDataType.VARCHAR(32), this, "");

    /**
     * The column <code>admin.mapcontext.west</code>.
     */
    public final TableField<MapcontextRecord, Double> WEST = createField(DSL.name("west"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.mapcontext.north</code>.
     */
    public final TableField<MapcontextRecord, Double> NORTH = createField(DSL.name("north"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.mapcontext.east</code>.
     */
    public final TableField<MapcontextRecord, Double> EAST = createField(DSL.name("east"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.mapcontext.south</code>.
     */
    public final TableField<MapcontextRecord, Double> SOUTH = createField(DSL.name("south"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.mapcontext.keywords</code>.
     */
    public final TableField<MapcontextRecord, String> KEYWORDS = createField(DSL.name("keywords"), SQLDataType.VARCHAR(256), this, "");

    private Mapcontext(Name alias, Table<MapcontextRecord> aliased) {
        this(alias, aliased, null);
    }

    private Mapcontext(Name alias, Table<MapcontextRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.mapcontext</code> table reference
     */
    public Mapcontext(String alias) {
        this(DSL.name(alias), MAPCONTEXT);
    }

    /**
     * Create an aliased <code>admin.mapcontext</code> table reference
     */
    public Mapcontext(Name alias) {
        this(alias, MAPCONTEXT);
    }

    /**
     * Create a <code>admin.mapcontext</code> table reference
     */
    public Mapcontext() {
        this(DSL.name("mapcontext"), null);
    }

    public <O extends Record> Mapcontext(Table<O> child, ForeignKey<O, MapcontextRecord> key) {
        super(child, key, MAPCONTEXT);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.MAPCONTEXT_OWNER_IDX);
    }

    @Override
    public Identity<MapcontextRecord, Integer> getIdentity() {
        return (Identity<MapcontextRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<MapcontextRecord> getPrimaryKey() {
        return Keys.MAPCONTEXT_PK;
    }

    @Override
    public List<ForeignKey<MapcontextRecord, ?>> getReferences() {
        return Arrays.asList(Keys.MAPCONTEXT__MAPCONTEXT_OWNER_FK);
    }

    private transient CstlUser _cstlUser;

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.MAPCONTEXT__MAPCONTEXT_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Mapcontext as(String alias) {
        return new Mapcontext(DSL.name(alias), this);
    }

    @Override
    public Mapcontext as(Name alias) {
        return new Mapcontext(alias, this);
    }

    @Override
    public Mapcontext as(Table<?> alias) {
        return new Mapcontext(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapcontext rename(String name) {
        return new Mapcontext(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapcontext rename(Name name) {
        return new Mapcontext(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Mapcontext rename(Table<?> name) {
        return new Mapcontext(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, String, Integer, String, String, Double, Double, Double, Double, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function10<? super Integer, ? super String, ? super Integer, ? super String, ? super String, ? super Double, ? super Double, ? super Double, ? super Double, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function10<? super Integer, ? super String, ? super Integer, ? super String, ? super String, ? super Double, ? super Double, ? super Double, ? super Double, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
