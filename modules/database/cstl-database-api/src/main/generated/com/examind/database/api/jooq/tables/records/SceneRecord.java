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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.Scene;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record20;
import org.jooq.Row20;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.scene
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SceneRecord extends UpdatableRecordImpl<SceneRecord> implements Record20<Integer, String, Integer, Integer, Integer, String, Integer[], String, Double, String, Long, Integer, Integer, Double, Double, Double, Double, String, String, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.scene.id</code>.
     */
    public SceneRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.id</code>.
     */
    @NotNull
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.scene.name</code>.
     */
    public SceneRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.name</code>.
     */
    @NotNull
    @Size(max = 10000)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.scene.map_context_id</code>.
     */
    public SceneRecord setMapContextId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.map_context_id</code>.
     */
    @NotNull
    public Integer getMapContextId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.scene.data_id</code>.
     */
    public SceneRecord setDataId(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.data_id</code>.
     */
    public Integer getDataId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>admin.scene.layer_id</code>.
     */
    public SceneRecord setLayerId(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.layer_id</code>.
     */
    public Integer getLayerId() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>admin.scene.type</code>.
     */
    public SceneRecord setType(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.type</code>.
     */
    @Size(max = 100)
    public String getType() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.scene.surface</code>.
     */
    public SceneRecord setSurface(Integer[] value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface</code>.
     */
    public Integer[] getSurface() {
        return (Integer[]) get(6);
    }

    /**
     * Setter for <code>admin.scene.surface_parameters</code>.
     */
    public SceneRecord setSurfaceParameters(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface_parameters</code>.
     */
    @Size(max = 10000)
    public String getSurfaceParameters() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.scene.surface_factor</code>.
     */
    public SceneRecord setSurfaceFactor(Double value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.surface_factor</code>.
     */
    public Double getSurfaceFactor() {
        return (Double) get(8);
    }

    /**
     * Setter for <code>admin.scene.status</code>.
     */
    public SceneRecord setStatus(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.status</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getStatus() {
        return (String) get(9);
    }

    /**
     * Setter for <code>admin.scene.creation_date</code>.
     */
    public SceneRecord setCreationDate(Long value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.creation_date</code>.
     */
    @NotNull
    public Long getCreationDate() {
        return (Long) get(10);
    }

    /**
     * Setter for <code>admin.scene.min_lod</code>.
     */
    public SceneRecord setMinLod(Integer value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.min_lod</code>.
     */
    public Integer getMinLod() {
        return (Integer) get(11);
    }

    /**
     * Setter for <code>admin.scene.max_lod</code>.
     */
    public SceneRecord setMaxLod(Integer value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.max_lod</code>.
     */
    public Integer getMaxLod() {
        return (Integer) get(12);
    }

    /**
     * Setter for <code>admin.scene.bbox_minx</code>.
     */
    public SceneRecord setBboxMinx(Double value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_minx</code>.
     */
    public Double getBboxMinx() {
        return (Double) get(13);
    }

    /**
     * Setter for <code>admin.scene.bbox_miny</code>.
     */
    public SceneRecord setBboxMiny(Double value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_miny</code>.
     */
    public Double getBboxMiny() {
        return (Double) get(14);
    }

    /**
     * Setter for <code>admin.scene.bbox_maxx</code>.
     */
    public SceneRecord setBboxMaxx(Double value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_maxx</code>.
     */
    public Double getBboxMaxx() {
        return (Double) get(15);
    }

    /**
     * Setter for <code>admin.scene.bbox_maxy</code>.
     */
    public SceneRecord setBboxMaxy(Double value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.bbox_maxy</code>.
     */
    public Double getBboxMaxy() {
        return (Double) get(16);
    }

    /**
     * Setter for <code>admin.scene.time</code>.
     */
    public SceneRecord setTime(String value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.time</code>.
     */
    @Size(max = 10000)
    public String getTime() {
        return (String) get(17);
    }

    /**
     * Setter for <code>admin.scene.extras</code>.
     */
    public SceneRecord setExtras(String value) {
        set(18, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.extras</code>.
     */
    @Size(max = 10000)
    public String getExtras() {
        return (String) get(18);
    }

    /**
     * Setter for <code>admin.scene.vector_simplify_factor</code>.
     */
    public SceneRecord setVectorSimplifyFactor(Double value) {
        set(19, value);
        return this;
    }

    /**
     * Getter for <code>admin.scene.vector_simplify_factor</code>.
     */
    public Double getVectorSimplifyFactor() {
        return (Double) get(19);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record20 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row20<Integer, String, Integer, Integer, Integer, String, Integer[], String, Double, String, Long, Integer, Integer, Double, Double, Double, Double, String, String, Double> fieldsRow() {
        return (Row20) super.fieldsRow();
    }

    @Override
    public Row20<Integer, String, Integer, Integer, Integer, String, Integer[], String, Double, String, Long, Integer, Integer, Double, Double, Double, Double, String, String, Double> valuesRow() {
        return (Row20) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Scene.SCENE.ID;
    }

    @Override
    public Field<String> field2() {
        return Scene.SCENE.NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Scene.SCENE.MAP_CONTEXT_ID;
    }

    @Override
    public Field<Integer> field4() {
        return Scene.SCENE.DATA_ID;
    }

    @Override
    public Field<Integer> field5() {
        return Scene.SCENE.LAYER_ID;
    }

    @Override
    public Field<String> field6() {
        return Scene.SCENE.TYPE;
    }

    @Override
    public Field<Integer[]> field7() {
        return Scene.SCENE.SURFACE;
    }

    @Override
    public Field<String> field8() {
        return Scene.SCENE.SURFACE_PARAMETERS;
    }

    @Override
    public Field<Double> field9() {
        return Scene.SCENE.SURFACE_FACTOR;
    }

    @Override
    public Field<String> field10() {
        return Scene.SCENE.STATUS;
    }

    @Override
    public Field<Long> field11() {
        return Scene.SCENE.CREATION_DATE;
    }

    @Override
    public Field<Integer> field12() {
        return Scene.SCENE.MIN_LOD;
    }

    @Override
    public Field<Integer> field13() {
        return Scene.SCENE.MAX_LOD;
    }

    @Override
    public Field<Double> field14() {
        return Scene.SCENE.BBOX_MINX;
    }

    @Override
    public Field<Double> field15() {
        return Scene.SCENE.BBOX_MINY;
    }

    @Override
    public Field<Double> field16() {
        return Scene.SCENE.BBOX_MAXX;
    }

    @Override
    public Field<Double> field17() {
        return Scene.SCENE.BBOX_MAXY;
    }

    @Override
    public Field<String> field18() {
        return Scene.SCENE.TIME;
    }

    @Override
    public Field<String> field19() {
        return Scene.SCENE.EXTRAS;
    }

    @Override
    public Field<Double> field20() {
        return Scene.SCENE.VECTOR_SIMPLIFY_FACTOR;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public Integer component3() {
        return getMapContextId();
    }

    @Override
    public Integer component4() {
        return getDataId();
    }

    @Override
    public Integer component5() {
        return getLayerId();
    }

    @Override
    public String component6() {
        return getType();
    }

    @Override
    public Integer[] component7() {
        return getSurface();
    }

    @Override
    public String component8() {
        return getSurfaceParameters();
    }

    @Override
    public Double component9() {
        return getSurfaceFactor();
    }

    @Override
    public String component10() {
        return getStatus();
    }

    @Override
    public Long component11() {
        return getCreationDate();
    }

    @Override
    public Integer component12() {
        return getMinLod();
    }

    @Override
    public Integer component13() {
        return getMaxLod();
    }

    @Override
    public Double component14() {
        return getBboxMinx();
    }

    @Override
    public Double component15() {
        return getBboxMiny();
    }

    @Override
    public Double component16() {
        return getBboxMaxx();
    }

    @Override
    public Double component17() {
        return getBboxMaxy();
    }

    @Override
    public String component18() {
        return getTime();
    }

    @Override
    public String component19() {
        return getExtras();
    }

    @Override
    public Double component20() {
        return getVectorSimplifyFactor();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public Integer value3() {
        return getMapContextId();
    }

    @Override
    public Integer value4() {
        return getDataId();
    }

    @Override
    public Integer value5() {
        return getLayerId();
    }

    @Override
    public String value6() {
        return getType();
    }

    @Override
    public Integer[] value7() {
        return getSurface();
    }

    @Override
    public String value8() {
        return getSurfaceParameters();
    }

    @Override
    public Double value9() {
        return getSurfaceFactor();
    }

    @Override
    public String value10() {
        return getStatus();
    }

    @Override
    public Long value11() {
        return getCreationDate();
    }

    @Override
    public Integer value12() {
        return getMinLod();
    }

    @Override
    public Integer value13() {
        return getMaxLod();
    }

    @Override
    public Double value14() {
        return getBboxMinx();
    }

    @Override
    public Double value15() {
        return getBboxMiny();
    }

    @Override
    public Double value16() {
        return getBboxMaxx();
    }

    @Override
    public Double value17() {
        return getBboxMaxy();
    }

    @Override
    public String value18() {
        return getTime();
    }

    @Override
    public String value19() {
        return getExtras();
    }

    @Override
    public Double value20() {
        return getVectorSimplifyFactor();
    }

    @Override
    public SceneRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public SceneRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public SceneRecord value3(Integer value) {
        setMapContextId(value);
        return this;
    }

    @Override
    public SceneRecord value4(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public SceneRecord value5(Integer value) {
        setLayerId(value);
        return this;
    }

    @Override
    public SceneRecord value6(String value) {
        setType(value);
        return this;
    }

    @Override
    public SceneRecord value7(Integer[] value) {
        setSurface(value);
        return this;
    }

    @Override
    public SceneRecord value8(String value) {
        setSurfaceParameters(value);
        return this;
    }

    @Override
    public SceneRecord value9(Double value) {
        setSurfaceFactor(value);
        return this;
    }

    @Override
    public SceneRecord value10(String value) {
        setStatus(value);
        return this;
    }

    @Override
    public SceneRecord value11(Long value) {
        setCreationDate(value);
        return this;
    }

    @Override
    public SceneRecord value12(Integer value) {
        setMinLod(value);
        return this;
    }

    @Override
    public SceneRecord value13(Integer value) {
        setMaxLod(value);
        return this;
    }

    @Override
    public SceneRecord value14(Double value) {
        setBboxMinx(value);
        return this;
    }

    @Override
    public SceneRecord value15(Double value) {
        setBboxMiny(value);
        return this;
    }

    @Override
    public SceneRecord value16(Double value) {
        setBboxMaxx(value);
        return this;
    }

    @Override
    public SceneRecord value17(Double value) {
        setBboxMaxy(value);
        return this;
    }

    @Override
    public SceneRecord value18(String value) {
        setTime(value);
        return this;
    }

    @Override
    public SceneRecord value19(String value) {
        setExtras(value);
        return this;
    }

    @Override
    public SceneRecord value20(Double value) {
        setVectorSimplifyFactor(value);
        return this;
    }

    @Override
    public SceneRecord values(Integer value1, String value2, Integer value3, Integer value4, Integer value5, String value6, Integer[] value7, String value8, Double value9, String value10, Long value11, Integer value12, Integer value13, Double value14, Double value15, Double value16, Double value17, String value18, String value19, Double value20) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        value19(value19);
        value20(value20);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SceneRecord
     */
    public SceneRecord() {
        super(Scene.SCENE);
    }

    /**
     * Create a detached, initialised SceneRecord
     */
    public SceneRecord(Integer id, String name, Integer mapContextId, Integer dataId, Integer layerId, String type, Integer[] surface, String surfaceParameters, Double surfaceFactor, String status, Long creationDate, Integer minLod, Integer maxLod, Double bboxMinx, Double bboxMiny, Double bboxMaxx, Double bboxMaxy, String time, String extras, Double vectorSimplifyFactor) {
        super(Scene.SCENE);

        setId(id);
        setName(name);
        setMapContextId(mapContextId);
        setDataId(dataId);
        setLayerId(layerId);
        setType(type);
        setSurface(surface);
        setSurfaceParameters(surfaceParameters);
        setSurfaceFactor(surfaceFactor);
        setStatus(status);
        setCreationDate(creationDate);
        setMinLod(minLod);
        setMaxLod(maxLod);
        setBboxMinx(bboxMinx);
        setBboxMiny(bboxMiny);
        setBboxMaxx(bboxMaxx);
        setBboxMaxy(bboxMaxy);
        setTime(time);
        setExtras(extras);
        setVectorSimplifyFactor(vectorSimplifyFactor);
    }
}
