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
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer style;
    private Integer layer;
    private Boolean isDefault;

    public StyledLayer() {}

    public StyledLayer(StyledLayer value) {
        this.style = value.style;
        this.layer = value.layer;
        this.isDefault = value.isDefault;
    }

    public StyledLayer(
        Integer style,
        Integer layer,
        Boolean isDefault
    ) {
        this.style = style;
        this.layer = layer;
        this.isDefault = isDefault;
    }

    /**
     * Getter for <code>admin.styled_layer.style</code>.
     */
    @NotNull
    public Integer getStyle() {
        return this.style;
    }

    /**
     * Setter for <code>admin.styled_layer.style</code>.
     */
    public StyledLayer setStyle(Integer style) {
        this.style = style;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.layer</code>.
     */
    @NotNull
    public Integer getLayer() {
        return this.layer;
    }

    /**
     * Setter for <code>admin.styled_layer.layer</code>.
     */
    public StyledLayer setLayer(Integer layer) {
        this.layer = layer;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.is_default</code>.
     */
    public Boolean getIsDefault() {
        return this.isDefault;
    }

    /**
     * Setter for <code>admin.styled_layer.is_default</code>.
     */
    public StyledLayer setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StyledLayer (");

        sb.append(style);
        sb.append(", ").append(layer);
        sb.append(", ").append(isDefault);

        sb.append(")");
        return sb.toString();
    }
}
