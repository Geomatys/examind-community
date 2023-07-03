/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.dto.thesaurus;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.constellation.dto.Identifiable;

/**
 * A simple pojo for thesaurus transfer to UI
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class Thesaurus extends Identifiable implements Serializable {

    private String uri;

    private String name;

    private String description;

    private Date creationDate;

    private boolean state;

    private List<String> langs;

    private String defaultLang;

    private String version;

    transient String schemaName;

    public Thesaurus() {

    }

    public Thesaurus(final Integer id, final String uri, final String name, final Date creationDate, final String description, final List<String> languages, final String defaultLanguage,
            final String schemaName, final boolean state, final String version) {
        this.id           = id;
        this.defaultLang  = defaultLanguage;
        this.description  = description;
        this.creationDate = creationDate;
        this.langs        = languages;
        this.name         = name;
        this.state        = state;
        this.uri          = uri;
        this.version      = version;
        this.schemaName   = schemaName;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
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
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the description to set
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the state
     */
    public boolean isState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(boolean state) {
        this.state = state;
    }

    /**
     * @return the langs
     */
    public List<String> getLangs() {
        return langs;
    }

    /**
     * @param langs the langs to set
     */
    public void setLangs(List<String> langs) {
        this.langs = langs;
    }

    /**
     * @return the defaultLang
     */
    public String getDefaultLang() {
        return defaultLang;
    }

    /**
     * @param defaultLang the defaultLang to set
     */
    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final Thesaurus that = (Thesaurus) obj;
            return Objects.equals(this.creationDate, that.creationDate) &&
                   Objects.equals(this.defaultLang, that.defaultLang) &&
                   Objects.equals(this.description, that.description) &&
                   Objects.equals(this.id, that.id) &&
                   Objects.equals(this.langs, that.langs) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.state, that.state) &&
                   Objects.equals(this.schemaName, that.schemaName) &&
                   Objects.equals(this.uri, that.uri) &&
                   Objects.equals(this.version, that.version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.uri);
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.description);
        hash = 97 * hash + Objects.hashCode(this.creationDate);
        hash = 97 * hash + (this.state ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.langs);
        hash = 97 * hash + Objects.hashCode(this.defaultLang);
        hash = 97 * hash + Objects.hashCode(this.version);
        hash = 97 * hash + Objects.hashCode(this.schemaName);
        return hash;
    }

    

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Thesaurus{");
        sb.append("uri=").append(uri);
        sb.append(", id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", creationDate='").append(creationDate).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", langs='").append(langs).append('\'');
        sb.append(", defaultLang='").append(defaultLang).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", schemaName='").append(schemaName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
