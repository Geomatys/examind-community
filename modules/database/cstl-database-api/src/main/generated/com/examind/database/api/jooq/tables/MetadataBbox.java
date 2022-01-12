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
import com.examind.database.api.jooq.tables.records.MetadataBboxRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.metadata_bbox
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataBbox extends TableImpl<MetadataBboxRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.metadata_bbox</code>
     */
    public static final MetadataBbox METADATA_BBOX = new MetadataBbox();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetadataBboxRecord> getRecordType() {
        return MetadataBboxRecord.class;
    }

    /**
     * The column <code>admin.metadata_bbox.metadata_id</code>.
     */
    public final TableField<MetadataBboxRecord, Integer> METADATA_ID = createField(DSL.name("metadata_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_bbox.east</code>.
     */
    public final TableField<MetadataBboxRecord, Double> EAST = createField(DSL.name("east"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_bbox.west</code>.
     */
    public final TableField<MetadataBboxRecord, Double> WEST = createField(DSL.name("west"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_bbox.north</code>.
     */
    public final TableField<MetadataBboxRecord, Double> NORTH = createField(DSL.name("north"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_bbox.south</code>.
     */
    public final TableField<MetadataBboxRecord, Double> SOUTH = createField(DSL.name("south"), SQLDataType.DOUBLE.nullable(false), this, "");

    private MetadataBbox(Name alias, Table<MetadataBboxRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetadataBbox(Name alias, Table<MetadataBboxRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.metadata_bbox</code> table reference
     */
    public MetadataBbox(String alias) {
        this(DSL.name(alias), METADATA_BBOX);
    }

    /**
     * Create an aliased <code>admin.metadata_bbox</code> table reference
     */
    public MetadataBbox(Name alias) {
        this(alias, METADATA_BBOX);
    }

    /**
     * Create a <code>admin.metadata_bbox</code> table reference
     */
    public MetadataBbox() {
        this(DSL.name("metadata_bbox"), null);
    }

    public <O extends Record> MetadataBbox(Table<O> child, ForeignKey<O, MetadataBboxRecord> key) {
        super(child, key, METADATA_BBOX);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<MetadataBboxRecord> getPrimaryKey() {
        return Keys.METADATA_BBOX_PK;
    }

    @Override
    public List<UniqueKey<MetadataBboxRecord>> getKeys() {
        return Arrays.<UniqueKey<MetadataBboxRecord>>asList(Keys.METADATA_BBOX_PK);
    }

    @Override
    public List<ForeignKey<MetadataBboxRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<MetadataBboxRecord, ?>>asList(Keys.METADATA_BBOX__BBOX_METADATA_FK);
    }

    private transient Metadata _metadata;

    public Metadata metadata() {
        if (_metadata == null)
            _metadata = new Metadata(this, Keys.METADATA_BBOX__BBOX_METADATA_FK);

        return _metadata;
    }

    @Override
    public MetadataBbox as(String alias) {
        return new MetadataBbox(DSL.name(alias), this);
    }

    @Override
    public MetadataBbox as(Name alias) {
        return new MetadataBbox(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataBbox rename(String name) {
        return new MetadataBbox(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataBbox rename(Name name) {
        return new MetadataBbox(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Double, Double, Double, Double> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
