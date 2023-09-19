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
import com.examind.database.api.jooq.tables.records.DataElevationsRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
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
 * Generated DAO object for table admin.data_elevations
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataElevations extends TableImpl<DataElevationsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.data_elevations</code>
     */
    public static final DataElevations DATA_ELEVATIONS = new DataElevations();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DataElevationsRecord> getRecordType() {
        return DataElevationsRecord.class;
    }

    /**
     * The column <code>admin.data_elevations.data_id</code>.
     */
    public final TableField<DataElevationsRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_elevations.elevation</code>.
     */
    public final TableField<DataElevationsRecord, Double> ELEVATION = createField(DSL.name("elevation"), SQLDataType.DOUBLE.nullable(false), this, "");

    private DataElevations(Name alias, Table<DataElevationsRecord> aliased) {
        this(alias, aliased, null);
    }

    private DataElevations(Name alias, Table<DataElevationsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.data_elevations</code> table reference
     */
    public DataElevations(String alias) {
        this(DSL.name(alias), DATA_ELEVATIONS);
    }

    /**
     * Create an aliased <code>admin.data_elevations</code> table reference
     */
    public DataElevations(Name alias) {
        this(alias, DATA_ELEVATIONS);
    }

    /**
     * Create a <code>admin.data_elevations</code> table reference
     */
    public DataElevations() {
        this(DSL.name("data_elevations"), null);
    }

    public <O extends Record> DataElevations(Table<O> child, ForeignKey<O, DataElevationsRecord> key) {
        super(child, key, DATA_ELEVATIONS);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DataElevationsRecord> getPrimaryKey() {
        return Keys.DATA_ELEVATIONS_PK;
    }

    @Override
    public List<ForeignKey<DataElevationsRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATA_ELEVATIONS__DATA_ELEVATIONS_DATA_FK);
    }

    private transient Data _data;

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.DATA_ELEVATIONS__DATA_ELEVATIONS_DATA_FK);

        return _data;
    }

    @Override
    public DataElevations as(String alias) {
        return new DataElevations(DSL.name(alias), this);
    }

    @Override
    public DataElevations as(Name alias) {
        return new DataElevations(alias, this);
    }

    @Override
    public DataElevations as(Table<?> alias) {
        return new DataElevations(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DataElevations rename(String name) {
        return new DataElevations(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataElevations rename(Name name) {
        return new DataElevations(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataElevations rename(Table<?> name) {
        return new DataElevations(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Double> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Double, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Double, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
