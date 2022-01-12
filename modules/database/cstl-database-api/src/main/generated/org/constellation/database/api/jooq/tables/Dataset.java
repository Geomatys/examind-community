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
import org.constellation.database.api.jooq.Indexes;
import org.constellation.database.api.jooq.Keys;
import org.constellation.database.api.jooq.tables.records.DatasetRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
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
 * Generated DAO object for table admin.dataset
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Dataset extends TableImpl<DatasetRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.dataset</code>
     */
    public static final Dataset DATASET = new Dataset();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasetRecord> getRecordType() {
        return DatasetRecord.class;
    }

    /**
     * The column <code>admin.dataset.id</code>.
     */
    public final TableField<DatasetRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.dataset.identifier</code>.
     */
    public final TableField<DatasetRecord, String> IDENTIFIER = createField(DSL.name("identifier"), SQLDataType.VARCHAR(100).nullable(false), this, "");

    /**
     * The column <code>admin.dataset.owner</code>.
     */
    public final TableField<DatasetRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.dataset.date</code>.
     */
    public final TableField<DatasetRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.dataset.feature_catalog</code>.
     */
    public final TableField<DatasetRecord, String> FEATURE_CATALOG = createField(DSL.name("feature_catalog"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.dataset.type</code>.
     */
    public final TableField<DatasetRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR, this, "");

    private Dataset(Name alias, Table<DatasetRecord> aliased) {
        this(alias, aliased, null);
    }

    private Dataset(Name alias, Table<DatasetRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.dataset</code> table reference
     */
    public Dataset(String alias) {
        this(DSL.name(alias), DATASET);
    }

    /**
     * Create an aliased <code>admin.dataset</code> table reference
     */
    public Dataset(Name alias) {
        this(alias, DATASET);
    }

    /**
     * Create a <code>admin.dataset</code> table reference
     */
    public Dataset() {
        this(DSL.name("dataset"), null);
    }

    public <O extends Record> Dataset(Table<O> child, ForeignKey<O, DatasetRecord> key) {
        super(child, key, DATASET);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.DATASET_OWNER_IDX);
    }

    @Override
    public Identity<DatasetRecord, Integer> getIdentity() {
        return (Identity<DatasetRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<DatasetRecord> getPrimaryKey() {
        return Keys.DATASET_PK;
    }

    @Override
    public List<UniqueKey<DatasetRecord>> getKeys() {
        return Arrays.<UniqueKey<DatasetRecord>>asList(Keys.DATASET_PK);
    }

    @Override
    public List<ForeignKey<DatasetRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<DatasetRecord, ?>>asList(Keys.DATASET__DATASET_OWNER_FK);
    }

    private transient CstlUser _cstlUser;

    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.DATASET__DATASET_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Dataset as(String alias) {
        return new Dataset(DSL.name(alias), this);
    }

    @Override
    public Dataset as(Name alias) {
        return new Dataset(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Dataset rename(String name) {
        return new Dataset(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Dataset rename(Name name) {
        return new Dataset(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, Integer, Long, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
