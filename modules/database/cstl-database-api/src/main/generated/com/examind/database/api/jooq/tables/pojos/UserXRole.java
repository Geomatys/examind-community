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
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.user_x_role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UserXRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String  role;

    public UserXRole() {}

    public UserXRole(UserXRole value) {
        this.userId = value.userId;
        this.role = value.role;
    }

    public UserXRole(
        Integer userId,
        String  role
    ) {
        this.userId = userId;
        this.role = role;
    }

    /**
     * Getter for <code>admin.user_x_role.user_id</code>.
     */
    @NotNull
    public Integer getUserId() {
        return this.userId;
    }

    /**
     * Setter for <code>admin.user_x_role.user_id</code>.
     */
    public UserXRole setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Getter for <code>admin.user_x_role.role</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getRole() {
        return this.role;
    }

    /**
     * Setter for <code>admin.user_x_role.role</code>.
     */
    public UserXRole setRole(String role) {
        this.role = role;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("UserXRole (");

        sb.append(userId);
        sb.append(", ").append(role);

        sb.append(")");
        return sb.toString();
    }
}
