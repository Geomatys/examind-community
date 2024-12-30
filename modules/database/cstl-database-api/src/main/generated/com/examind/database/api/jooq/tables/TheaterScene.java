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
import com.examind.database.api.jooq.tables.records.TheaterSceneRecord;

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
 * Generated DAO object for table admin.theater_scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class TheaterScene extends TableImpl<TheaterSceneRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.theater_scene</code>
     */
    public static final TheaterScene THEATER_SCENE = new TheaterScene();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TheaterSceneRecord> getRecordType() {
        return TheaterSceneRecord.class;
    }

    /**
     * The column <code>admin.theater_scene.theater_id</code>.
     */
    public final TableField<TheaterSceneRecord, Integer> THEATER_ID = createField(DSL.name("theater_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.theater_scene.scene_id</code>.
     */
    public final TableField<TheaterSceneRecord, Integer> SCENE_ID = createField(DSL.name("scene_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private TheaterScene(Name alias, Table<TheaterSceneRecord> aliased) {
        this(alias, aliased, null);
    }

    private TheaterScene(Name alias, Table<TheaterSceneRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.theater_scene</code> table reference
     */
    public TheaterScene(String alias) {
        this(DSL.name(alias), THEATER_SCENE);
    }

    /**
     * Create an aliased <code>admin.theater_scene</code> table reference
     */
    public TheaterScene(Name alias) {
        this(alias, THEATER_SCENE);
    }

    /**
     * Create a <code>admin.theater_scene</code> table reference
     */
    public TheaterScene() {
        this(DSL.name("theater_scene"), null);
    }

    public <O extends Record> TheaterScene(Table<O> child, ForeignKey<O, TheaterSceneRecord> key) {
        super(child, key, THEATER_SCENE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<TheaterSceneRecord> getPrimaryKey() {
        return Keys.THEATER_SCENE_PK;
    }

    @Override
    public List<ForeignKey<TheaterSceneRecord, ?>> getReferences() {
        return Arrays.asList(Keys.THEATER_SCENE__THEATER_SCENE_THEATHER_FK, Keys.THEATER_SCENE__THEATER_SCENE_SCENE_FK);
    }

    private transient Theater _theater;
    private transient Scene _scene;

    /**
     * Get the implicit join path to the <code>admin.theater</code> table.
     */
    public Theater theater() {
        if (_theater == null)
            _theater = new Theater(this, Keys.THEATER_SCENE__THEATER_SCENE_THEATHER_FK);

        return _theater;
    }

    /**
     * Get the implicit join path to the <code>admin.scene</code> table.
     */
    public Scene scene() {
        if (_scene == null)
            _scene = new Scene(this, Keys.THEATER_SCENE__THEATER_SCENE_SCENE_FK);

        return _scene;
    }

    @Override
    public TheaterScene as(String alias) {
        return new TheaterScene(DSL.name(alias), this);
    }

    @Override
    public TheaterScene as(Name alias) {
        return new TheaterScene(alias, this);
    }

    @Override
    public TheaterScene as(Table<?> alias) {
        return new TheaterScene(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public TheaterScene rename(String name) {
        return new TheaterScene(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public TheaterScene rename(Name name) {
        return new TheaterScene(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public TheaterScene rename(Table<?> name) {
        return new TheaterScene(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
