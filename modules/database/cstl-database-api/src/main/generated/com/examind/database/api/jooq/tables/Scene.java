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
import com.examind.database.api.jooq.tables.records.SceneRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row20;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Scene extends TableImpl<SceneRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.scene</code>
     */
    public static final Scene SCENE = new Scene();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SceneRecord> getRecordType() {
        return SceneRecord.class;
    }

    /**
     * The column <code>admin.scene.id</code>.
     */
    public final TableField<SceneRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.scene.name</code>.
     */
    public final TableField<SceneRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(10000), this, "");

    /**
     * The column <code>admin.scene.map_context_id</code>.
     */
    public final TableField<SceneRecord, Integer> MAP_CONTEXT_ID = createField(DSL.name("map_context_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.scene.data_id</code>.
     */
    public final TableField<SceneRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.scene.layer_id</code>.
     */
    public final TableField<SceneRecord, Integer> LAYER_ID = createField(DSL.name("layer_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.scene.type</code>.
     */
    public final TableField<SceneRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(100), this, "");

    /**
     * The column <code>admin.scene.surface</code>.
     */
    public final TableField<SceneRecord, Integer[]> SURFACE = createField(DSL.name("surface"), SQLDataType.INTEGER.getArrayDataType(), this, "");

    /**
     * The column <code>admin.scene.surface_parameters</code>.
     */
    public final TableField<SceneRecord, String> SURFACE_PARAMETERS = createField(DSL.name("surface_parameters"), SQLDataType.VARCHAR(10000), this, "");

    /**
     * The column <code>admin.scene.surface_factor</code>.
     */
    public final TableField<SceneRecord, Double> SURFACE_FACTOR = createField(DSL.name("surface_factor"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.scene.status</code>.
     */
    public final TableField<SceneRecord, String> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR(100).nullable(false), this, "");

    /**
     * The column <code>admin.scene.creation_date</code>.
     */
    public final TableField<SceneRecord, Long> CREATION_DATE = createField(DSL.name("creation_date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.scene.min_lod</code>.
     */
    public final TableField<SceneRecord, Integer> MIN_LOD = createField(DSL.name("min_lod"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.scene.max_lod</code>.
     */
    public final TableField<SceneRecord, Integer> MAX_LOD = createField(DSL.name("max_lod"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.scene.bbox_minx</code>.
     */
    public final TableField<SceneRecord, Double> BBOX_MINX = createField(DSL.name("bbox_minx"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.scene.bbox_miny</code>.
     */
    public final TableField<SceneRecord, Double> BBOX_MINY = createField(DSL.name("bbox_miny"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.scene.bbox_maxx</code>.
     */
    public final TableField<SceneRecord, Double> BBOX_MAXX = createField(DSL.name("bbox_maxx"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.scene.bbox_maxy</code>.
     */
    public final TableField<SceneRecord, Double> BBOX_MAXY = createField(DSL.name("bbox_maxy"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.scene.time</code>.
     */
    public final TableField<SceneRecord, String> TIME = createField(DSL.name("time"), SQLDataType.VARCHAR(10000), this, "");

    /**
     * The column <code>admin.scene.extras</code>.
     */
    public final TableField<SceneRecord, String> EXTRAS = createField(DSL.name("extras"), SQLDataType.VARCHAR(10000), this, "");

    /**
     * The column <code>admin.scene.vector_simplify_factor</code>.
     */
    public final TableField<SceneRecord, Double> VECTOR_SIMPLIFY_FACTOR = createField(DSL.name("vector_simplify_factor"), SQLDataType.DOUBLE, this, "");

    private Scene(Name alias, Table<SceneRecord> aliased) {
        this(alias, aliased, null);
    }

    private Scene(Name alias, Table<SceneRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.scene</code> table reference
     */
    public Scene(String alias) {
        this(DSL.name(alias), SCENE);
    }

    /**
     * Create an aliased <code>admin.scene</code> table reference
     */
    public Scene(Name alias) {
        this(alias, SCENE);
    }

    /**
     * Create a <code>admin.scene</code> table reference
     */
    public Scene() {
        this(DSL.name("scene"), null);
    }

    public <O extends Record> Scene(Table<O> child, ForeignKey<O, SceneRecord> key) {
        super(child, key, SCENE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<SceneRecord> getPrimaryKey() {
        return Keys.SCENE_PK;
    }

    @Override
    public List<UniqueKey<SceneRecord>> getKeys() {
        return Arrays.<UniqueKey<SceneRecord>>asList(Keys.SCENE_PK, Keys.SCENE_NAME_UNIQUE_KEY);
    }

    @Override
    public List<ForeignKey<SceneRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<SceneRecord, ?>>asList(Keys.SCENE__SCENE_LAYER_FK, Keys.SCENE__SCENE_DATA_FK);
    }

    private transient Data _data;
    private transient Layer _layer;

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.SCENE__SCENE_LAYER_FK);

        return _data;
    }

    public Layer layer() {
        if (_layer == null)
            _layer = new Layer(this, Keys.SCENE__SCENE_DATA_FK);

        return _layer;
    }

    @Override
    public Scene as(String alias) {
        return new Scene(DSL.name(alias), this);
    }

    @Override
    public Scene as(Name alias) {
        return new Scene(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Scene rename(String name) {
        return new Scene(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Scene rename(Name name) {
        return new Scene(name, null);
    }

    // -------------------------------------------------------------------------
    // Row20 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row20<Integer, String, Integer, Integer, Integer, String, Integer[], String, Double, String, Long, Integer, Integer, Double, Double, Double, Double, String, String, Double> fieldsRow() {
        return (Row20) super.fieldsRow();
    }
}
