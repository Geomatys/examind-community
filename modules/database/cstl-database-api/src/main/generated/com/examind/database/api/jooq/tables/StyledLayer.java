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
import com.examind.database.api.jooq.tables.records.StyledLayerRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledLayer extends TableImpl<StyledLayerRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.styled_layer</code>
     */
    public static final StyledLayer STYLED_LAYER = new StyledLayer();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StyledLayerRecord> getRecordType() {
        return StyledLayerRecord.class;
    }

    /**
     * The column <code>admin.styled_layer.style</code>.
     */
    public final TableField<StyledLayerRecord, Integer> STYLE = createField(DSL.name("style"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.styled_layer.layer</code>.
     */
    public final TableField<StyledLayerRecord, Integer> LAYER = createField(DSL.name("layer"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.styled_layer.is_default</code>.
     */
    public final TableField<StyledLayerRecord, Boolean> IS_DEFAULT = createField(DSL.name("is_default"), SQLDataType.BOOLEAN, this, "");

    private StyledLayer(Name alias, Table<StyledLayerRecord> aliased) {
        this(alias, aliased, null);
    }

    private StyledLayer(Name alias, Table<StyledLayerRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.styled_layer</code> table reference
     */
    public StyledLayer(String alias) {
        this(DSL.name(alias), STYLED_LAYER);
    }

    /**
     * Create an aliased <code>admin.styled_layer</code> table reference
     */
    public StyledLayer(Name alias) {
        this(alias, STYLED_LAYER);
    }

    /**
     * Create a <code>admin.styled_layer</code> table reference
     */
    public StyledLayer() {
        this(DSL.name("styled_layer"), null);
    }

    public <O extends Record> StyledLayer(Table<O> child, ForeignKey<O, StyledLayerRecord> key) {
        super(child, key, STYLED_LAYER);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.STYLED_LAYER_LAYER_IDX, Indexes.STYLED_LAYER_STYLE_IDX);
    }

    @Override
    public UniqueKey<StyledLayerRecord> getPrimaryKey() {
        return Keys.STYLED_LAYER_PK;
    }

    @Override
    public List<UniqueKey<StyledLayerRecord>> getKeys() {
        return Arrays.<UniqueKey<StyledLayerRecord>>asList(Keys.STYLED_LAYER_PK);
    }

    @Override
    public List<ForeignKey<StyledLayerRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<StyledLayerRecord, ?>>asList(Keys.STYLED_LAYER__STYLED_LAYER_STYLE_FK, Keys.STYLED_LAYER__STYLED_LAYER_LAYER_FK);
    }

    private transient Style _style;
    private transient Layer _layer;

    public Style style() {
        if (_style == null)
            _style = new Style(this, Keys.STYLED_LAYER__STYLED_LAYER_STYLE_FK);

        return _style;
    }

    public Layer layer() {
        if (_layer == null)
            _layer = new Layer(this, Keys.STYLED_LAYER__STYLED_LAYER_LAYER_FK);

        return _layer;
    }

    @Override
    public StyledLayer as(String alias) {
        return new StyledLayer(DSL.name(alias), this);
    }

    @Override
    public StyledLayer as(Name alias) {
        return new StyledLayer(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public StyledLayer rename(String name) {
        return new StyledLayer(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public StyledLayer rename(Name name) {
        return new StyledLayer(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Integer, Boolean> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
