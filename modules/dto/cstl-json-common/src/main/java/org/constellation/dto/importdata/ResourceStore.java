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

import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class ResourceStore {
    
    public String id;
    public String file;
    public List<String> files;
    public boolean indivisible;

    public ResourceStore(String id, String file, List<String> files, boolean indivisible) {
        this.id = id;
        this.file = file;
        this.files = files;
        this.indivisible = indivisible;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.id);
        hash = 31 * hash + Objects.hashCode(this.file);
        hash = 31 * hash + Objects.hashCode(this.files);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof ResourceStore) {
            ResourceStore that = (ResourceStore) o;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.files, that.files)
                    && Objects.equals(this.file, that.file);
        }
        return false;
    }
}
