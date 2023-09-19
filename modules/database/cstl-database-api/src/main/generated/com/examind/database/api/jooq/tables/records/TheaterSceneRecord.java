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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.TheaterScene;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.theater_scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TheaterSceneRecord extends UpdatableRecordImpl<TheaterSceneRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.theater_scene.theater_id</code>.
     */
    public TheaterSceneRecord setTheaterId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater_scene.theater_id</code>.
     */
    @NotNull
    public Integer getTheaterId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.theater_scene.scene_id</code>.
     */
    public TheaterSceneRecord setSceneId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater_scene.scene_id</code>.
     */
    @NotNull
    public Integer getSceneId() {
        return (Integer) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, Integer> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return TheaterScene.THEATER_SCENE.THEATER_ID;
    }

    @Override
    public Field<Integer> field2() {
        return TheaterScene.THEATER_SCENE.SCENE_ID;
    }

    @Override
    public Integer component1() {
        return getTheaterId();
    }

    @Override
    public Integer component2() {
        return getSceneId();
    }

    @Override
    public Integer value1() {
        return getTheaterId();
    }

    @Override
    public Integer value2() {
        return getSceneId();
    }

    @Override
    public TheaterSceneRecord value1(Integer value) {
        setTheaterId(value);
        return this;
    }

    @Override
    public TheaterSceneRecord value2(Integer value) {
        setSceneId(value);
        return this;
    }

    @Override
    public TheaterSceneRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TheaterSceneRecord
     */
    public TheaterSceneRecord() {
        super(TheaterScene.THEATER_SCENE);
    }

    /**
     * Create a detached, initialised TheaterSceneRecord
     */
    public TheaterSceneRecord(Integer theaterId, Integer sceneId) {
        super(TheaterScene.THEATER_SCENE);

        setTheaterId(theaterId);
        setSceneId(sceneId);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised TheaterSceneRecord
     */
    public TheaterSceneRecord(com.examind.database.api.jooq.tables.pojos.TheaterScene value) {
        super(TheaterScene.THEATER_SCENE);

        if (value != null) {
            setTheaterId(value.getTheaterId());
            setSceneId(value.getSceneId());
            resetChangedOnNotNull();
        }
    }
}
