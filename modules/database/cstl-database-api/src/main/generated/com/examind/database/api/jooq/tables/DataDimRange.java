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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.DataDimRangeRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.data_dim_range
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataDimRange extends TableImpl<DataDimRangeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.data_dim_range</code>
     */
    public static final DataDimRange DATA_DIM_RANGE = new DataDimRange();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DataDimRangeRecord> getRecordType() {
        return DataDimRangeRecord.class;
    }

    /**
     * The column <code>admin.data_dim_range.data_id</code>.
     */
    public final TableField<DataDimRangeRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_dim_range.dimension</code>.
     */
    public final TableField<DataDimRangeRecord, Integer> DIMENSION = createField(DSL.name("dimension"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_dim_range.min</code>.
     */
    public final TableField<DataDimRangeRecord, Double> MIN = createField(DSL.name("min"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.data_dim_range.max</code>.
     */
    public final TableField<DataDimRangeRecord, Double> MAX = createField(DSL.name("max"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.data_dim_range.unit</code>.
     */
    public final TableField<DataDimRangeRecord, String> UNIT = createField(DSL.name("unit"), SQLDataType.VARCHAR(1000), this, "");

    /**
     * The column <code>admin.data_dim_range.unit_symbol</code>.
     */
    public final TableField<DataDimRangeRecord, String> UNIT_SYMBOL = createField(DSL.name("unit_symbol"), SQLDataType.VARCHAR(1000), this, "");

    private DataDimRange(Name alias, Table<DataDimRangeRecord> aliased) {
        this(alias, aliased, null);
    }

    private DataDimRange(Name alias, Table<DataDimRangeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.data_dim_range</code> table reference
     */
    public DataDimRange(String alias) {
        this(DSL.name(alias), DATA_DIM_RANGE);
    }

    /**
     * Create an aliased <code>admin.data_dim_range</code> table reference
     */
    public DataDimRange(Name alias) {
        this(alias, DATA_DIM_RANGE);
    }

    /**
     * Create a <code>admin.data_dim_range</code> table reference
     */
    public DataDimRange() {
        this(DSL.name("data_dim_range"), null);
    }

    public <O extends Record> DataDimRange(Table<O> child, ForeignKey<O, DataDimRangeRecord> key) {
        super(child, key, DATA_DIM_RANGE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<DataDimRangeRecord> getPrimaryKey() {
        return Keys.DATA_DIM_RANGE_PK;
    }

    @Override
    public List<UniqueKey<DataDimRangeRecord>> getKeys() {
        return Arrays.<UniqueKey<DataDimRangeRecord>>asList(Keys.DATA_DIM_RANGE_PK);
    }

    @Override
    public List<ForeignKey<DataDimRangeRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<DataDimRangeRecord, ?>>asList(Keys.DATA_DIM_RANGE__DATA_DIM_RANGE_DATA_FK);
    }

    private transient Data _data;

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.DATA_DIM_RANGE__DATA_DIM_RANGE_DATA_FK);

        return _data;
    }

    @Override
    public DataDimRange as(String alias) {
        return new DataDimRange(DSL.name(alias), this);
    }

    @Override
    public DataDimRange as(Name alias) {
        return new DataDimRange(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public DataDimRange rename(String name) {
        return new DataDimRange(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataDimRange rename(Name name) {
        return new DataDimRange(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Double, Double, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
