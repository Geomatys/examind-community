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


import com.examind.database.api.jooq.tables.MetadataBbox;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.metadata_bbox
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataBboxRecord extends UpdatableRecordImpl<MetadataBboxRecord> implements Record5<Integer, Double, Double, Double, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.metadata_bbox.metadata_id</code>.
     */
    public MetadataBboxRecord setMetadataId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.metadata_id</code>.
     */
    @NotNull
    public Integer getMetadataId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.metadata_bbox.east</code>.
     */
    public MetadataBboxRecord setEast(Double value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.east</code>.
     */
    @NotNull
    public Double getEast() {
        return (Double) get(1);
    }

    /**
     * Setter for <code>admin.metadata_bbox.west</code>.
     */
    public MetadataBboxRecord setWest(Double value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.west</code>.
     */
    @NotNull
    public Double getWest() {
        return (Double) get(2);
    }

    /**
     * Setter for <code>admin.metadata_bbox.north</code>.
     */
    public MetadataBboxRecord setNorth(Double value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.north</code>.
     */
    @NotNull
    public Double getNorth() {
        return (Double) get(3);
    }

    /**
     * Setter for <code>admin.metadata_bbox.south</code>.
     */
    public MetadataBboxRecord setSouth(Double value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.south</code>.
     */
    @NotNull
    public Double getSouth() {
        return (Double) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record5<Integer, Double, Double, Double, Double> key() {
        return (Record5) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, Double, Double, Double, Double> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, Double, Double, Double, Double> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return MetadataBbox.METADATA_BBOX.METADATA_ID;
    }

    @Override
    public Field<Double> field2() {
        return MetadataBbox.METADATA_BBOX.EAST;
    }

    @Override
    public Field<Double> field3() {
        return MetadataBbox.METADATA_BBOX.WEST;
    }

    @Override
    public Field<Double> field4() {
        return MetadataBbox.METADATA_BBOX.NORTH;
    }

    @Override
    public Field<Double> field5() {
        return MetadataBbox.METADATA_BBOX.SOUTH;
    }

    @Override
    public Integer component1() {
        return getMetadataId();
    }

    @Override
    public Double component2() {
        return getEast();
    }

    @Override
    public Double component3() {
        return getWest();
    }

    @Override
    public Double component4() {
        return getNorth();
    }

    @Override
    public Double component5() {
        return getSouth();
    }

    @Override
    public Integer value1() {
        return getMetadataId();
    }

    @Override
    public Double value2() {
        return getEast();
    }

    @Override
    public Double value3() {
        return getWest();
    }

    @Override
    public Double value4() {
        return getNorth();
    }

    @Override
    public Double value5() {
        return getSouth();
    }

    @Override
    public MetadataBboxRecord value1(Integer value) {
        setMetadataId(value);
        return this;
    }

    @Override
    public MetadataBboxRecord value2(Double value) {
        setEast(value);
        return this;
    }

    @Override
    public MetadataBboxRecord value3(Double value) {
        setWest(value);
        return this;
    }

    @Override
    public MetadataBboxRecord value4(Double value) {
        setNorth(value);
        return this;
    }

    @Override
    public MetadataBboxRecord value5(Double value) {
        setSouth(value);
        return this;
    }

    @Override
    public MetadataBboxRecord values(Integer value1, Double value2, Double value3, Double value4, Double value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetadataBboxRecord
     */
    public MetadataBboxRecord() {
        super(MetadataBbox.METADATA_BBOX);
    }

    /**
     * Create a detached, initialised MetadataBboxRecord
     */
    public MetadataBboxRecord(Integer metadataId, Double east, Double west, Double north, Double south) {
        super(MetadataBbox.METADATA_BBOX);

        setMetadataId(metadataId);
        setEast(east);
        setWest(west);
        setNorth(north);
        setSouth(south);
    }
}
