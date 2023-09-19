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
import com.examind.database.api.jooq.tables.records.DataTimesRecord;

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
 * Generated DAO object for table admin.data_times
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataTimes extends TableImpl<DataTimesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.data_times</code>
     */
    public static final DataTimes DATA_TIMES = new DataTimes();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DataTimesRecord> getRecordType() {
        return DataTimesRecord.class;
    }

    /**
     * The column <code>admin.data_times.data_id</code>.
     */
    public final TableField<DataTimesRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_times.date</code>.
     */
    public final TableField<DataTimesRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT.nullable(false), this, "");

    private DataTimes(Name alias, Table<DataTimesRecord> aliased) {
        this(alias, aliased, null);
    }

    private DataTimes(Name alias, Table<DataTimesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.data_times</code> table reference
     */
    public DataTimes(String alias) {
        this(DSL.name(alias), DATA_TIMES);
    }

    /**
     * Create an aliased <code>admin.data_times</code> table reference
     */
    public DataTimes(Name alias) {
        this(alias, DATA_TIMES);
    }

    /**
     * Create a <code>admin.data_times</code> table reference
     */
    public DataTimes() {
        this(DSL.name("data_times"), null);
    }

    public <O extends Record> DataTimes(Table<O> child, ForeignKey<O, DataTimesRecord> key) {
        super(child, key, DATA_TIMES);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DataTimesRecord> getPrimaryKey() {
        return Keys.DATA_TIMES_PK;
    }

    @Override
    public List<ForeignKey<DataTimesRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATA_TIMES__DATA_TIMES_DATA_FK);
    }

    private transient Data _data;

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.DATA_TIMES__DATA_TIMES_DATA_FK);

        return _data;
    }

    @Override
    public DataTimes as(String alias) {
        return new DataTimes(DSL.name(alias), this);
    }

    @Override
    public DataTimes as(Name alias) {
        return new DataTimes(alias, this);
    }

    @Override
    public DataTimes as(Table<?> alias) {
        return new DataTimes(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DataTimes rename(String name) {
        return new DataTimes(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataTimes rename(Name name) {
        return new DataTimes(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataTimes rename(Table<?> name) {
        return new DataTimes(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Long> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Long, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Long, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
