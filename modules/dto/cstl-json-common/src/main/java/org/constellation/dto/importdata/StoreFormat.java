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
package org.constellation.dto.importdata;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StoreFormat {
    
    private String store;
    private String format;

    public StoreFormat() {
        
    }
    
    public StoreFormat(String store, String format) {
        this.format = format;
        this.store = store;
        
    }
    /**
     * @return the store
     */
    public String getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(String store) {
        this.store = store;
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
}
