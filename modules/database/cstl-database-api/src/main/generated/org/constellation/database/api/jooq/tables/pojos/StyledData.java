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
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;


/**
 * Generated DAO object for table admin.styled_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer style;
    private Integer data;

    public StyledData() {}

    public StyledData(StyledData value) {
        this.style = value.style;
        this.data = value.data;
    }

    public StyledData(
        Integer style,
        Integer data
    ) {
        this.style = style;
        this.data = data;
    }

    /**
     * Getter for <code>admin.styled_data.style</code>.
     */
    @NotNull
    public Integer getStyle() {
        return this.style;
    }

    /**
     * Setter for <code>admin.styled_data.style</code>.
     */
    public StyledData setStyle(Integer style) {
        this.style = style;
        return this;
    }

    /**
     * Getter for <code>admin.styled_data.data</code>.
     */
    @NotNull
    public Integer getData() {
        return this.data;
    }

    /**
     * Setter for <code>admin.styled_data.data</code>.
     */
    public StyledData setData(Integer data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StyledData (");

        sb.append(style);
        sb.append(", ").append(data);

        sb.append(")");
        return sb.toString();
    }
}
