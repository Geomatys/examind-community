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


import com.examind.database.api.jooq.tables.Mapcontext;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.mapcontext
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class MapcontextRecord extends UpdatableRecordImpl<MapcontextRecord> implements Record10<Integer, String, Integer, String, String, Double, Double, Double, Double, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.mapcontext.id</code>.
     */
    public MapcontextRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.mapcontext.name</code>.
     */
    public MapcontextRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.mapcontext.owner</code>.
     */
    public MapcontextRecord setOwner(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.mapcontext.description</code>.
     */
    public MapcontextRecord setDescription(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.description</code>.
     */
    @Size(max = 512)
    public String getDescription() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.mapcontext.crs</code>.
     */
    public MapcontextRecord setCrs(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.crs</code>.
     */
    @Size(max = 32)
    public String getCrs() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.mapcontext.west</code>.
     */
    public MapcontextRecord setWest(Double value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.west</code>.
     */
    public Double getWest() {
        return (Double) get(5);
    }

    /**
     * Setter for <code>admin.mapcontext.north</code>.
     */
    public MapcontextRecord setNorth(Double value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.north</code>.
     */
    public Double getNorth() {
        return (Double) get(6);
    }

    /**
     * Setter for <code>admin.mapcontext.east</code>.
     */
    public MapcontextRecord setEast(Double value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.east</code>.
     */
    public Double getEast() {
        return (Double) get(7);
    }

    /**
     * Setter for <code>admin.mapcontext.south</code>.
     */
    public MapcontextRecord setSouth(Double value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.south</code>.
     */
    public Double getSouth() {
        return (Double) get(8);
    }

    /**
     * Setter for <code>admin.mapcontext.keywords</code>.
     */
    public MapcontextRecord setKeywords(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.keywords</code>.
     */
    @Size(max = 256)
    public String getKeywords() {
        return (String) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, String, Integer, String, String, Double, Double, Double, Double, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, String, Integer, String, String, Double, Double, Double, Double, String> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Mapcontext.MAPCONTEXT.ID;
    }

    @Override
    public Field<String> field2() {
        return Mapcontext.MAPCONTEXT.NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Mapcontext.MAPCONTEXT.OWNER;
    }

    @Override
    public Field<String> field4() {
        return Mapcontext.MAPCONTEXT.DESCRIPTION;
    }

    @Override
    public Field<String> field5() {
        return Mapcontext.MAPCONTEXT.CRS;
    }

    @Override
    public Field<Double> field6() {
        return Mapcontext.MAPCONTEXT.WEST;
    }

    @Override
    public Field<Double> field7() {
        return Mapcontext.MAPCONTEXT.NORTH;
    }

    @Override
    public Field<Double> field8() {
        return Mapcontext.MAPCONTEXT.EAST;
    }

    @Override
    public Field<Double> field9() {
        return Mapcontext.MAPCONTEXT.SOUTH;
    }

    @Override
    public Field<String> field10() {
        return Mapcontext.MAPCONTEXT.KEYWORDS;
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
        return getOwner();
    }

    @Override
    public String component4() {
        return getDescription();
    }

    @Override
    public String component5() {
        return getCrs();
    }

    @Override
    public Double component6() {
        return getWest();
    }

    @Override
    public Double component7() {
        return getNorth();
    }

    @Override
    public Double component8() {
        return getEast();
    }

    @Override
    public Double component9() {
        return getSouth();
    }

    @Override
    public String component10() {
        return getKeywords();
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
        return getOwner();
    }

    @Override
    public String value4() {
        return getDescription();
    }

    @Override
    public String value5() {
        return getCrs();
    }

    @Override
    public Double value6() {
        return getWest();
    }

    @Override
    public Double value7() {
        return getNorth();
    }

    @Override
    public Double value8() {
        return getEast();
    }

    @Override
    public Double value9() {
        return getSouth();
    }

    @Override
    public String value10() {
        return getKeywords();
    }

    @Override
    public MapcontextRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public MapcontextRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public MapcontextRecord value3(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public MapcontextRecord value4(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public MapcontextRecord value5(String value) {
        setCrs(value);
        return this;
    }

    @Override
    public MapcontextRecord value6(Double value) {
        setWest(value);
        return this;
    }

    @Override
    public MapcontextRecord value7(Double value) {
        setNorth(value);
        return this;
    }

    @Override
    public MapcontextRecord value8(Double value) {
        setEast(value);
        return this;
    }

    @Override
    public MapcontextRecord value9(Double value) {
        setSouth(value);
        return this;
    }

    @Override
    public MapcontextRecord value10(String value) {
        setKeywords(value);
        return this;
    }

    @Override
    public MapcontextRecord values(Integer value1, String value2, Integer value3, String value4, String value5, Double value6, Double value7, Double value8, Double value9, String value10) {
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
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MapcontextRecord
     */
    public MapcontextRecord() {
        super(Mapcontext.MAPCONTEXT);
    }

    /**
     * Create a detached, initialised MapcontextRecord
     */
    public MapcontextRecord(Integer id, String name, Integer owner, String description, String crs, Double west, Double north, Double east, Double south, String keywords) {
        super(Mapcontext.MAPCONTEXT);

        setId(id);
        setName(name);
        setOwner(owner);
        setDescription(description);
        setCrs(crs);
        setWest(west);
        setNorth(north);
        setEast(east);
        setSouth(south);
        setKeywords(keywords);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised MapcontextRecord
     */
    public MapcontextRecord(com.examind.database.api.jooq.tables.pojos.Mapcontext value) {
        super(Mapcontext.MAPCONTEXT);

        if (value != null) {
            setId(value.getId());
            setName(value.getName());
            setOwner(value.getOwner());
            setDescription(value.getDescription());
            setCrs(value.getCrs());
            setWest(value.getWest());
            setNorth(value.getNorth());
            setEast(value.getEast());
            setSouth(value.getSouth());
            setKeywords(value.getKeywords());
            resetChangedOnNotNull();
        }
    }
}
