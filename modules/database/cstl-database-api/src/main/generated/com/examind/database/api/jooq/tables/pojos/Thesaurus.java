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
 * Generated DAO object for table admin.thesaurus
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Thesaurus implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String uri;
    private String name;
    private String description;
    private Long creationDate;
    private Boolean state;
    private String defaultlang;
    private String version;
    private String schemaname;

    public Thesaurus() {}

    public Thesaurus(Thesaurus value) {
        this.id = value.id;
        this.uri = value.uri;
        this.name = value.name;
        this.description = value.description;
        this.creationDate = value.creationDate;
        this.state = value.state;
        this.defaultlang = value.defaultlang;
        this.version = value.version;
        this.schemaname = value.schemaname;
    }

    public Thesaurus(
        Integer id,
        String uri,
        String name,
        String description,
        Long creationDate,
        Boolean state,
        String defaultlang,
        String version,
        String schemaname
    ) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.creationDate = creationDate;
        this.state = state;
        this.defaultlang = defaultlang;
        this.version = version;
        this.schemaname = schemaname;
    }

    /**
     * Getter for <code>admin.thesaurus.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.thesaurus.id</code>.
     */
    public Thesaurus setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.uri</code>.
     */
    @NotNull
    @Size(max = 200)
    public String getUri() {
        return this.uri;
    }

    /**
     * Setter for <code>admin.thesaurus.uri</code>.
     */
    public Thesaurus setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.name</code>.
     */
    @NotNull
    @Size(max = 200)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.thesaurus.name</code>.
     */
    public Thesaurus setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.description</code>.
     */
    @Size(max = 500)
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for <code>admin.thesaurus.description</code>.
     */
    public Thesaurus setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.creation_date</code>.
     */
    @NotNull
    public Long getCreationDate() {
        return this.creationDate;
    }

    /**
     * Setter for <code>admin.thesaurus.creation_date</code>.
     */
    public Thesaurus setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.state</code>.
     */
    public Boolean getState() {
        return this.state;
    }

    /**
     * Setter for <code>admin.thesaurus.state</code>.
     */
    public Thesaurus setState(Boolean state) {
        this.state = state;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.defaultlang</code>.
     */
    @Size(max = 3)
    public String getDefaultlang() {
        return this.defaultlang;
    }

    /**
     * Setter for <code>admin.thesaurus.defaultlang</code>.
     */
    public Thesaurus setDefaultlang(String defaultlang) {
        this.defaultlang = defaultlang;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.version</code>.
     */
    @Size(max = 20)
    public String getVersion() {
        return this.version;
    }

    /**
     * Setter for <code>admin.thesaurus.version</code>.
     */
    public Thesaurus setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.schemaname</code>.
     */
    @Size(max = 100)
    public String getSchemaname() {
        return this.schemaname;
    }

    /**
     * Setter for <code>admin.thesaurus.schemaname</code>.
     */
    public Thesaurus setSchemaname(String schemaname) {
        this.schemaname = schemaname;
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
        final Thesaurus other = (Thesaurus) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.uri == null) {
            if (other.uri != null)
                return false;
        }
        else if (!this.uri.equals(other.uri))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.description == null) {
            if (other.description != null)
                return false;
        }
        else if (!this.description.equals(other.description))
            return false;
        if (this.creationDate == null) {
            if (other.creationDate != null)
                return false;
        }
        else if (!this.creationDate.equals(other.creationDate))
            return false;
        if (this.state == null) {
            if (other.state != null)
                return false;
        }
        else if (!this.state.equals(other.state))
            return false;
        if (this.defaultlang == null) {
            if (other.defaultlang != null)
                return false;
        }
        else if (!this.defaultlang.equals(other.defaultlang))
            return false;
        if (this.version == null) {
            if (other.version != null)
                return false;
        }
        else if (!this.version.equals(other.version))
            return false;
        if (this.schemaname == null) {
            if (other.schemaname != null)
                return false;
        }
        else if (!this.schemaname.equals(other.schemaname))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.uri == null) ? 0 : this.uri.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.creationDate == null) ? 0 : this.creationDate.hashCode());
        result = prime * result + ((this.state == null) ? 0 : this.state.hashCode());
        result = prime * result + ((this.defaultlang == null) ? 0 : this.defaultlang.hashCode());
        result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
        result = prime * result + ((this.schemaname == null) ? 0 : this.schemaname.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Thesaurus (");

        sb.append(id);
        sb.append(", ").append(uri);
        sb.append(", ").append(name);
        sb.append(", ").append(description);
        sb.append(", ").append(creationDate);
        sb.append(", ").append(state);
        sb.append(", ").append(defaultlang);
        sb.append(", ").append(version);
        sb.append(", ").append(schemaname);

        sb.append(")");
        return sb.toString();
    }
}
