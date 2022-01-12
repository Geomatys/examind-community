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
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.CrsRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
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
 * Generated DAO object for table admin.crs
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Crs extends TableImpl<CrsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.crs</code>
     */
    public static final Crs CRS = new Crs();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<CrsRecord> getRecordType() {
        return CrsRecord.class;
    }

    /**
     * The column <code>admin.crs.dataid</code>.
     */
    public final TableField<CrsRecord, Integer> DATAID = createField(DSL.name("dataid"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.crs.crscode</code>.
     */
    public final TableField<CrsRecord, String> CRSCODE = createField(DSL.name("crscode"), SQLDataType.VARCHAR(64).nullable(false), this, "");

    private Crs(Name alias, Table<CrsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Crs(Name alias, Table<CrsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.crs</code> table reference
     */
    public Crs(String alias) {
        this(DSL.name(alias), CRS);
    }

    /**
     * Create an aliased <code>admin.crs</code> table reference
     */
    public Crs(Name alias) {
        this(alias, CRS);
    }

    /**
     * Create a <code>admin.crs</code> table reference
     */
    public Crs() {
        this(DSL.name("crs"), null);
    }

    public <O extends Record> Crs(Table<O> child, ForeignKey<O, CrsRecord> key) {
        super(child, key, CRS);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.CRS_DATAID_IDX);
    }

    @Override
    public UniqueKey<CrsRecord> getPrimaryKey() {
        return Keys.CRS_PK;
    }

    @Override
    public List<UniqueKey<CrsRecord>> getKeys() {
        return Arrays.<UniqueKey<CrsRecord>>asList(Keys.CRS_PK);
    }

    @Override
    public List<ForeignKey<CrsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<CrsRecord, ?>>asList(Keys.CRS__CRS_DATAID_FK);
    }

    private transient Data _data;

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.CRS__CRS_DATAID_FK);

        return _data;
    }

    @Override
    public Crs as(String alias) {
        return new Crs(DSL.name(alias), this);
    }

    @Override
    public Crs as(Name alias) {
        return new Crs(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Crs rename(String name) {
        return new Crs(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Crs rename(Name name) {
        return new Crs(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
