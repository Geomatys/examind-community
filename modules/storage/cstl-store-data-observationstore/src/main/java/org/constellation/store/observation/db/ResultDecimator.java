/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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

import java.util.List;
import org.geotoolkit.observation.ResultBuilder;
import org.geotoolkit.observation.model.Field;

/**
 *
 * @author guilhem
 */
public abstract class ResultDecimator extends ResultProcessor {

    protected final int width;

    protected List<Integer> fieldFilters;
    
    public ResultDecimator(ResultBuilder values, List<Field> fields, boolean profileWithTime, boolean profile, int width, List<Integer> fieldFilters) {
        super(values, fields, profileWithTime, profile);
        this.width = width;
        this.fieldFilters = fieldFilters;
    }

}
