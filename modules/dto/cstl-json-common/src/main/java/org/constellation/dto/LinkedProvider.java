/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LinkedProvider {

    private Integer id;

    private boolean allEntry;

    public LinkedProvider() {
        
    }
    
    public LinkedProvider(Integer id, boolean allEntry) {
        this.id = id;
        this.allEntry = allEntry;
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

    /**
     * @return the allEntry
     */
    public boolean isAllEntry() {
        return allEntry;
    }

    /**
     * @param allEntry the allEntry to set
     */
    public void setAllEntry(boolean allEntry) {
        this.allEntry = allEntry;
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
            LinkedProvider that = (LinkedProvider) obj;
            return Objects.equals(this.allEntry, that.allEntry) &&
                   Objects.equals(this.id,       that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + (this.allEntry ? 1 : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "[LinkedProvider]\nid=" + id + "\nallEntry=" + allEntry;
    }
}
