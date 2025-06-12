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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class DataSource extends Identifiable implements Serializable {

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
    private Map<String, String> properties;

    public DataSource() {
    }

    public DataSource(Integer id, String type, String url, String username,
            String pwd, String storeId, Boolean readFromRemote, Long dateCreation,
            String analysisState, String format, Boolean permanent, Map<String, String> properties) {
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
        this.properties = properties;
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

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the pwd
     */
    public String getPwd() {
        return pwd;
    }

    /**
     * @param pwd the pwd to set
     */
    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    /**
     * @return the storeId
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * @param storeId the storeId to set
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    /**
     * @return the readFromRemote
     */
    public Boolean getReadFromRemote() {
        return readFromRemote;
    }

    /**
     * @param readFromRemote the readFromRemote to set
     */
    public void setReadFromRemote(Boolean readFromRemote) {
        this.readFromRemote = readFromRemote;
    }

    /**
     * @return the dateCreation
     */
    public Long getDateCreation() {
        return dateCreation;
    }

    /**
     * @param dateCreation the dateCreation to set
     */
    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }

    /**
     * @return the analysisState
     */
    public String getAnalysisState() {
        return analysisState;
    }

    /**
     * @param analysisState the analysisState to set
     */
    public void setAnalysisState(String analysisState) {
        this.analysisState = analysisState;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the permanent
     */
    public Boolean getPermanent() {
        if (permanent == null) {
           permanent = false;
        }
        return permanent;
    }

    /**
     * @param permanent the permanent to set
     */
    public void setPermanent(Boolean permanent) {
        this.permanent = permanent;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void hideSensibleField() {
        if (pwd != null) {
            pwd = "******";
        }
    }
}
