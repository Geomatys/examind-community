/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

import java.io.Serializable;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class DataSet extends Identifiable implements Serializable {

    private String identifier;
    private Integer ownerId;
    private Long date;
    private String type;

    public DataSet() {
    }

    public DataSet(Integer id, String identifier, Integer ownerId,
            Long date, String type) {
        this.id = id;
        this.identifier = identifier;
        this.ownerId = ownerId;
        this.date = date;
        this.type = type;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the owner
     */
    public Integer getOwnerId() {
        return ownerId;
    }

    /**
     * @param ownerId the owner Identifier to set
     */
    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * @return the date
     */
    public Long getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Long date) {
        this.date = date;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            DataSet that = (DataSet) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.identifier, that.identifier)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.ownerId, that.ownerId)
                    && Objects.equals(this.date, that.date);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.identifier);
        hash = 59 * hash + Objects.hashCode(this.ownerId);
        hash = 59 * hash + Objects.hashCode(this.date);
        hash = 59 * hash + Objects.hashCode(this.type);
        return hash;
    }

}
