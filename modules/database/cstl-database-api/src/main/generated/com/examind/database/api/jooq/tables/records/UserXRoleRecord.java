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


import com.examind.database.api.jooq.tables.UserXRole;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.user_x_role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UserXRoleRecord extends UpdatableRecordImpl<UserXRoleRecord> implements Record2<Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.user_x_role.user_id</code>.
     */
    public UserXRoleRecord setUserId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.user_x_role.user_id</code>.
     */
    @NotNull
    public Integer getUserId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.user_x_role.role</code>.
     */
    public UserXRoleRecord setRole(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.user_x_role.role</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getRole() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return UserXRole.USER_X_ROLE.USER_ID;
    }

    @Override
    public Field<String> field2() {
        return UserXRole.USER_X_ROLE.ROLE;
    }

    @Override
    public Integer component1() {
        return getUserId();
    }

    @Override
    public String component2() {
        return getRole();
    }

    @Override
    public Integer value1() {
        return getUserId();
    }

    @Override
    public String value2() {
        return getRole();
    }

    @Override
    public UserXRoleRecord value1(Integer value) {
        setUserId(value);
        return this;
    }

    @Override
    public UserXRoleRecord value2(String value) {
        setRole(value);
        return this;
    }

    @Override
    public UserXRoleRecord values(Integer value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached UserXRoleRecord
     */
    public UserXRoleRecord() {
        super(UserXRole.USER_X_ROLE);
    }

    /**
     * Create a detached, initialised UserXRoleRecord
     */
    public UserXRoleRecord(Integer userId, String role) {
        super(UserXRole.USER_X_ROLE);

        setUserId(userId);
        setRole(role);
    }
}
