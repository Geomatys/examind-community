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
 * Generated DAO object for table admin.datasource
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Datasource implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  type;
    private String  url;
    private String  username;
    private String  pwd;
    private String  storeId;
    private Boolean readFromRemote;
    private Long    dateCreation;
    private String  analysisState;
    private String  format;
    private Boolean permanent;

    public Datasource() {}

    public Datasource(Datasource value) {
        this.id = value.id;
        this.type = value.type;
        this.url = value.url;
        this.username = value.username;
        this.pwd = value.pwd;
        this.storeId = value.storeId;
        this.readFromRemote = value.readFromRemote;
        this.dateCreation = value.dateCreation;
        this.analysisState = value.analysisState;
        this.format = value.format;
        this.permanent = value.permanent;
    }

    public Datasource(
        Integer id,
        String  type,
        String  url,
        String  username,
        String  pwd,
        String  storeId,
        Boolean readFromRemote,
        Long    dateCreation,
        String  analysisState,
        String  format,
        Boolean permanent
    ) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.username = username;
        this.pwd = pwd;
        this.storeId = storeId;
        this.readFromRemote = readFromRemote;
        this.dateCreation = dateCreation;
        this.analysisState = analysisState;
        this.format = format;
        this.permanent = permanent;
    }

    /**
     * Getter for <code>admin.datasource.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.datasource.id</code>.
     */
    public Datasource setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.type</code>.
     */
    @NotNull
    @Size(max = 50)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.datasource.type</code>.
     */
    public Datasource setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.url</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getUrl() {
        return this.url;
    }

    /**
     * Setter for <code>admin.datasource.url</code>.
     */
    public Datasource setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.username</code>.
     */
    @Size(max = 100)
    public String getUsername() {
        return this.username;
    }

    /**
     * Setter for <code>admin.datasource.username</code>.
     */
    public Datasource setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.pwd</code>.
     */
    @Size(max = 500)
    public String getPwd() {
        return this.pwd;
    }

    /**
     * Setter for <code>admin.datasource.pwd</code>.
     */
    public Datasource setPwd(String pwd) {
        this.pwd = pwd;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.store_id</code>.
     */
    @Size(max = 100)
    public String getStoreId() {
        return this.storeId;
    }

    /**
     * Setter for <code>admin.datasource.store_id</code>.
     */
    public Datasource setStoreId(String storeId) {
        this.storeId = storeId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.read_from_remote</code>.
     */
    public Boolean getReadFromRemote() {
        return this.readFromRemote;
    }

    /**
     * Setter for <code>admin.datasource.read_from_remote</code>.
     */
    public Datasource setReadFromRemote(Boolean readFromRemote) {
        this.readFromRemote = readFromRemote;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.date_creation</code>.
     */
    public Long getDateCreation() {
        return this.dateCreation;
    }

    /**
     * Setter for <code>admin.datasource.date_creation</code>.
     */
    public Datasource setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.analysis_state</code>.
     */
    @Size(max = 50)
    public String getAnalysisState() {
        return this.analysisState;
    }

    /**
     * Setter for <code>admin.datasource.analysis_state</code>.
     */
    public Datasource setAnalysisState(String analysisState) {
        this.analysisState = analysisState;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.format</code>.
     */
    public String getFormat() {
        return this.format;
    }

    /**
     * Setter for <code>admin.datasource.format</code>.
     */
    public Datasource setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Getter for <code>admin.datasource.permanent</code>.
     */
    public Boolean getPermanent() {
        return this.permanent;
    }

    /**
     * Setter for <code>admin.datasource.permanent</code>.
     */
    public Datasource setPermanent(Boolean permanent) {
        this.permanent = permanent;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Datasource (");

        sb.append(id);
        sb.append(", ").append(type);
        sb.append(", ").append(url);
        sb.append(", ").append(username);
        sb.append(", ").append(pwd);
        sb.append(", ").append(storeId);
        sb.append(", ").append(readFromRemote);
        sb.append(", ").append(dateCreation);
        sb.append(", ").append(analysisState);
        sb.append(", ").append(format);
        sb.append(", ").append(permanent);

        sb.append(")");
        return sb.toString();
    }
}
