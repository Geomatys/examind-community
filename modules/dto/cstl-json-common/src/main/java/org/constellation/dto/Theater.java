/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.dto;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Theater extends Identifiable {

    private String name;

    private Integer dataId;

    private Integer layerId;

    private String type;

    public Theater() {

    }

    public Theater(Theater that) {
        super(that);
        if (that != null) {
            this.dataId = that.dataId;
            this.layerId = that.layerId;
            this.name = that.name;
            this.type = that.type;
        }
    }

    public Theater(Integer id, String name, Integer dataId, Integer layerId, String type) {
        super(id);
        this.name = name;
        this.dataId = dataId;
        this.layerId = layerId;
        this.type = type;
    }

    /**
     * @return the dataId
     */
    public Integer getDataId() {
        return dataId;
    }

    /**
     * @param dataId the dataId to set
     */
    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    /**
     * @return the LayerId
     */
    public Integer getLayerId() {
        return layerId;
    }

    /**
     * @param LayerId the LayerId to set
     */
    public void setLayerId(Integer LayerId) {
        this.layerId = LayerId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
}
