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
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.user_x_role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UserXRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String role;

    public UserXRole() {}

    public UserXRole(UserXRole value) {
        this.userId = value.userId;
        this.role = value.role;
    }

    public UserXRole(
        Integer userId,
        String role
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final UserXRole other = (UserXRole) obj;
        if (this.userId == null) {
            if (other.userId != null)
                return false;
        }
        else if (!this.userId.equals(other.userId))
            return false;
        if (this.role == null) {
            if (other.role != null)
                return false;
        }
        else if (!this.role.equals(other.role))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.userId == null) ? 0 : this.userId.hashCode());
        result = prime * result + ((this.role == null) ? 0 : this.role.hashCode());
        return result;
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
