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
import com.examind.database.api.jooq.tables.records.MapcontextStyledLayerRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row15;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.mapcontext_styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MapcontextStyledLayer extends TableImpl<MapcontextStyledLayerRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.mapcontext_styled_layer</code>
     */
    public static final MapcontextStyledLayer MAPCONTEXT_STYLED_LAYER = new MapcontextStyledLayer();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MapcontextStyledLayerRecord> getRecordType() {
        return MapcontextStyledLayerRecord.class;
    }

    /**
     * The column <code>admin.mapcontext_styled_layer.id</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.mapcontext_id</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> MAPCONTEXT_ID = createField(DSL.name("mapcontext_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.layer_id</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> LAYER_ID = createField(DSL.name("layer_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.style_id</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> STYLE_ID = createField(DSL.name("style_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.layer_order</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> LAYER_ORDER = createField(DSL.name("layer_order"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("1", SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.layer_opacity</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> LAYER_OPACITY = createField(DSL.name("layer_opacity"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("100", SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.layer_visible</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Boolean> LAYER_VISIBLE = createField(DSL.name("layer_visible"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("true", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.external_layer</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> EXTERNAL_LAYER = createField(DSL.name("external_layer"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> EXTERNAL_LAYER_EXTENT = createField(DSL.name("external_layer_extent"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> EXTERNAL_SERVICE_URL = createField(DSL.name("external_service_url"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> EXTERNAL_SERVICE_VERSION = createField(DSL.name("external_service_version"), SQLDataType.VARCHAR(32), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.external_style</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> EXTERNAL_STYLE = createField(DSL.name("external_style"), SQLDataType.VARCHAR(128), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.iswms</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Boolean> ISWMS = createField(DSL.name("iswms"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("true", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.data_id</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.mapcontext_styled_layer.query</code>.
     */
    public final TableField<MapcontextStyledLayerRecord, String> QUERY = createField(DSL.name("query"), SQLDataType.VARCHAR(10485760), this, "");

    private MapcontextStyledLayer(Name alias, Table<MapcontextStyledLayerRecord> aliased) {
        this(alias, aliased, null);
    }

    private MapcontextStyledLayer(Name alias, Table<MapcontextStyledLayerRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.mapcontext_styled_layer</code> table reference
     */
    public MapcontextStyledLayer(String alias) {
        this(DSL.name(alias), MAPCONTEXT_STYLED_LAYER);
    }

    /**
     * Create an aliased <code>admin.mapcontext_styled_layer</code> table reference
     */
    public MapcontextStyledLayer(Name alias) {
        this(alias, MAPCONTEXT_STYLED_LAYER);
    }

    /**
     * Create a <code>admin.mapcontext_styled_layer</code> table reference
     */
    public MapcontextStyledLayer() {
        this(DSL.name("mapcontext_styled_layer"), null);
    }

    public <O extends Record> MapcontextStyledLayer(Table<O> child, ForeignKey<O, MapcontextStyledLayerRecord> key) {
        super(child, key, MAPCONTEXT_STYLED_LAYER);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public Identity<MapcontextStyledLayerRecord, Integer> getIdentity() {
        return (Identity<MapcontextStyledLayerRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<MapcontextStyledLayerRecord> getPrimaryKey() {
        return Keys.MAPCONTEXT_STYLED_LAYER_PK;
    }

    @Override
    public List<UniqueKey<MapcontextStyledLayerRecord>> getKeys() {
        return Arrays.<UniqueKey<MapcontextStyledLayerRecord>>asList(Keys.MAPCONTEXT_STYLED_LAYER_PK);
    }

    @Override
    public List<ForeignKey<MapcontextStyledLayerRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<MapcontextStyledLayerRecord, ?>>asList(Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_MAPCONTEXT_ID_FK, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_LAYER_ID_FK, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_STYLE_ID_FK, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_DATA_ID_FK);
    }

    private transient Mapcontext _mapcontext;
    private transient Layer _layer;
    private transient Style _style;
    private transient Data _data;

    public Mapcontext mapcontext() {
        if (_mapcontext == null)
            _mapcontext = new Mapcontext(this, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_MAPCONTEXT_ID_FK);

        return _mapcontext;
    }

    public Layer layer() {
        if (_layer == null)
            _layer = new Layer(this, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_LAYER_ID_FK);

        return _layer;
    }

    public Style style() {
        if (_style == null)
            _style = new Style(this, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_STYLE_ID_FK);

        return _style;
    }

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.MAPCONTEXT_STYLED_LAYER__MAPCONTEXT_STYLED_LAYER_DATA_ID_FK);

        return _data;
    }

    @Override
    public MapcontextStyledLayer as(String alias) {
        return new MapcontextStyledLayer(DSL.name(alias), this);
    }

    @Override
    public MapcontextStyledLayer as(Name alias) {
        return new MapcontextStyledLayer(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MapcontextStyledLayer rename(String name) {
        return new MapcontextStyledLayer(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MapcontextStyledLayer rename(Name name) {
        return new MapcontextStyledLayer(name, null);
    }

    // -------------------------------------------------------------------------
    // Row15 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row15<Integer, Integer, Integer, Integer, Integer, Integer, Boolean, String, String, String, String, String, Boolean, Integer, String> fieldsRow() {
        return (Row15) super.fieldsRow();
    }
}
