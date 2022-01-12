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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.Style;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.style
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyleRecord extends UpdatableRecordImpl<StyleRecord> implements Record8<Integer, String, Integer, String, Long, String, Integer, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.style.id</code>.
     */
    public StyleRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.style.name</code>.
     */
    public StyleRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.style.provider</code>.
     */
    public StyleRecord setProvider(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.provider</code>.
     */
    @NotNull
    public Integer getProvider() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.style.type</code>.
     */
    public StyleRecord setType(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.style.date</code>.
     */
    public StyleRecord setDate(Long value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.date</code>.
     */
    @NotNull
    public Long getDate() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>admin.style.body</code>.
     */
    public StyleRecord setBody(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.body</code>.
     */
    @NotNull
    public String getBody() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.style.owner</code>.
     */
    public StyleRecord setOwner(Integer value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>admin.style.is_shared</code>.
     */
    public StyleRecord setIsShared(Boolean value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.style.is_shared</code>.
     */
    public Boolean getIsShared() {
        return (Boolean) get(7);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row8<Integer, String, Integer, String, Long, String, Integer, Boolean> fieldsRow() {
        return (Row8) super.fieldsRow();
    }

    @Override
    public Row8<Integer, String, Integer, String, Long, String, Integer, Boolean> valuesRow() {
        return (Row8) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Style.STYLE.ID;
    }

    @Override
    public Field<String> field2() {
        return Style.STYLE.NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Style.STYLE.PROVIDER;
    }

    @Override
    public Field<String> field4() {
        return Style.STYLE.TYPE;
    }

    @Override
    public Field<Long> field5() {
        return Style.STYLE.DATE;
    }

    @Override
    public Field<String> field6() {
        return Style.STYLE.BODY;
    }

    @Override
    public Field<Integer> field7() {
        return Style.STYLE.OWNER;
    }

    @Override
    public Field<Boolean> field8() {
        return Style.STYLE.IS_SHARED;
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
        return getProvider();
    }

    @Override
    public String component4() {
        return getType();
    }

    @Override
    public Long component5() {
        return getDate();
    }

    @Override
    public String component6() {
        return getBody();
    }

    @Override
    public Integer component7() {
        return getOwner();
    }

    @Override
    public Boolean component8() {
        return getIsShared();
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
        return getProvider();
    }

    @Override
    public String value4() {
        return getType();
    }

    @Override
    public Long value5() {
        return getDate();
    }

    @Override
    public String value6() {
        return getBody();
    }

    @Override
    public Integer value7() {
        return getOwner();
    }

    @Override
    public Boolean value8() {
        return getIsShared();
    }

    @Override
    public StyleRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public StyleRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public StyleRecord value3(Integer value) {
        setProvider(value);
        return this;
    }

    @Override
    public StyleRecord value4(String value) {
        setType(value);
        return this;
    }

    @Override
    public StyleRecord value5(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public StyleRecord value6(String value) {
        setBody(value);
        return this;
    }

    @Override
    public StyleRecord value7(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public StyleRecord value8(Boolean value) {
        setIsShared(value);
        return this;
    }

    @Override
    public StyleRecord values(Integer value1, String value2, Integer value3, String value4, Long value5, String value6, Integer value7, Boolean value8) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached StyleRecord
     */
    public StyleRecord() {
        super(Style.STYLE);
    }

    /**
     * Create a detached, initialised StyleRecord
     */
    public StyleRecord(Integer id, String name, Integer provider, String type, Long date, String body, Integer owner, Boolean isShared) {
        super(Style.STYLE);

        setId(id);
        setName(name);
        setProvider(provider);
        setType(type);
        setDate(date);
        setBody(body);
        setOwner(owner);
        setIsShared(isShared);
    }
}
