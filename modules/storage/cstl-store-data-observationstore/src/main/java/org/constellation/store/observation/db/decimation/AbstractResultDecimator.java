/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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
package org.constellation.store.observation.db.decimation;

import java.util.List;
import org.constellation.store.observation.db.ResultProcessor;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.model.Field;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractResultDecimator extends ResultProcessor {

    protected final int width;

    protected final List<Integer> fieldFilters;

    protected final boolean skipProfileMain;
    protected final boolean onlyProfileMain;

    public AbstractResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters, boolean includeTimeInProfile, ProcedureInfo procedure) {
        super(fields, includeId, false, false, includeTimeInProfile, procedure, "");
        this.width = width;
        this.fieldFilters = fieldFilters;
        onlyProfileMain = fieldFilters.contains(procedure.mainField.index) && fieldFilters.size() == 1;
        skipProfileMain = nonTimeseries && !fieldFilters.isEmpty() && !fieldFilters.contains(procedure.mainField.index);
    }

}
