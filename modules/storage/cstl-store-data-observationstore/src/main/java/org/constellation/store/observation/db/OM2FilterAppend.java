/*
 *    Examind - An open source and standard compliant SDI
 *    https://www.examind.com/examind-community/
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
package org.constellation.store.observation.db;

import org.geotoolkit.observation.FilterAppend;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2FilterAppend extends FilterAppend {
    
    public boolean main;
    public boolean result;
    
    public OM2FilterAppend() {
        this.main = false;
        this.result = false;
    }
    
    public OM2FilterAppend(boolean mainAppend, boolean resultAppend) {
        this.main = mainAppend;
        this.result = resultAppend;
    }

    @Override
    public OM2FilterAppend merge(FilterAppend fa) {
        if (fa instanceof OM2FilterAppend ofa) {
            this.append = this.append || ofa.append;
            this.main   = this.main   || ofa.main;
            this.result = this.result || ofa.result;
            return this;
        }
        throw new IllegalArgumentException("can nt merge a non OM2 filterAppend");
        
    }
    
    
}
