/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dto.fs;

import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Datasource {
    
    private String userName;
    private String password;
    private String location;
    
    private Map<String, String> advancedParameters;

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the advancedParameters
     */
    public Map<String, String> getAdvancedParameters() {
        if (advancedParameters == null) advancedParameters = Map.of();
        return advancedParameters;
    }

    /**
     * @param advancedParameters the advancedParameters to set
     */
    public void setAdvancedParameters(Map<String, String> advancedParameters) {
        this.advancedParameters = advancedParameters;
    }
    
}
