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
import static org.constellation.store.observation.db.OM2Utils.getMeasureFields;
import org.constellation.store.observation.db.ResultProcessor;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractResultDecimator extends ResultProcessor {

    protected final int width;

    protected final boolean skipProfileMain;
    protected final boolean onlyProfileMain;

    public AbstractResultDecimator(List<Field> fields, boolean includeId, int width, ProcedureInfo procedure) {
        super(fields, includeId, false, false, procedure, "");
        this.width = width;
        List<DbField> measureFields = getMeasureFields(fields, procedure);
        onlyProfileMain = measureFields.size() == 1 && measureFields.get(0).type.equals(FieldType.MAIN); // main field will only be included in measure fields for profile
        skipProfileMain = nonTimeseries && !measureFields.stream().anyMatch(mf -> mf.type.equals(FieldType.MAIN));
    }

}
