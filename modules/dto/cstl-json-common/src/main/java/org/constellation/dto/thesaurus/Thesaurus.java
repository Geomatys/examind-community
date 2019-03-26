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
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A simple pojo for thesaurus transfer to UI
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class Thesaurus implements Serializable {

    private Integer id;

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

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }
}
