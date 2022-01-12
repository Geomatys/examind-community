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
import com.examind.database.api.jooq.tables.records.DataXDataRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.data_x_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataXData extends TableImpl<DataXDataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.data_x_data</code>
     */
    public static final DataXData DATA_X_DATA = new DataXData();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DataXDataRecord> getRecordType() {
        return DataXDataRecord.class;
    }

    /**
     * The column <code>admin.data_x_data.data_id</code>.
     */
    public final TableField<DataXDataRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_x_data.child_id</code>.
     */
    public final TableField<DataXDataRecord, Integer> CHILD_ID = createField(DSL.name("child_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private DataXData(Name alias, Table<DataXDataRecord> aliased) {
        this(alias, aliased, null);
    }

    private DataXData(Name alias, Table<DataXDataRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.data_x_data</code> table reference
     */
    public DataXData(String alias) {
        this(DSL.name(alias), DATA_X_DATA);
    }

    /**
     * Create an aliased <code>admin.data_x_data</code> table reference
     */
    public DataXData(Name alias) {
        this(alias, DATA_X_DATA);
    }

    /**
     * Create a <code>admin.data_x_data</code> table reference
     */
    public DataXData() {
        this(DSL.name("data_x_data"), null);
    }

    public <O extends Record> DataXData(Table<O> child, ForeignKey<O, DataXDataRecord> key) {
        super(child, key, DATA_X_DATA);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<DataXDataRecord> getPrimaryKey() {
        return Keys.DATA_X_DATA_PK;
    }

    @Override
    public List<UniqueKey<DataXDataRecord>> getKeys() {
        return Arrays.<UniqueKey<DataXDataRecord>>asList(Keys.DATA_X_DATA_PK);
    }

    @Override
    public List<ForeignKey<DataXDataRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<DataXDataRecord, ?>>asList(Keys.DATA_X_DATA__DATA_X_DATA_CROSS_ID_FK, Keys.DATA_X_DATA__DATA_X_DATA_CROSS_ID_FK2);
    }

    private transient Data _dataXDataCrossIdFk;
    private transient Data _dataXDataCrossIdFk2;

    public Data dataXDataCrossIdFk() {
        if (_dataXDataCrossIdFk == null)
            _dataXDataCrossIdFk = new Data(this, Keys.DATA_X_DATA__DATA_X_DATA_CROSS_ID_FK);

        return _dataXDataCrossIdFk;
    }

    public Data dataXDataCrossIdFk2() {
        if (_dataXDataCrossIdFk2 == null)
            _dataXDataCrossIdFk2 = new Data(this, Keys.DATA_X_DATA__DATA_X_DATA_CROSS_ID_FK2);

        return _dataXDataCrossIdFk2;
    }

    @Override
    public DataXData as(String alias) {
        return new DataXData(DSL.name(alias), this);
    }

    @Override
    public DataXData as(Name alias) {
        return new DataXData(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public DataXData rename(String name) {
        return new DataXData(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataXData rename(Name name) {
        return new DataXData(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
