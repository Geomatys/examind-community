/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.observation.model.Field;

/**
 * @deprecated use {@link org.geotoolkit.observation.OMUtils}
 * @author Guilhem Legal (Geomatys)
 */
@Deprecated
public class OM2Utils {

    /**
     * @deprecated use {@link org.geotoolkit.observation.OMUtils#reOrderFields(java.util.List, java.util.List) }
     */
    @Deprecated
    public static List<Field> reOrderFields(List<Field> procedureFields, List<Field> subset) {
        List<Field> result = new ArrayList();
        for (Field pField : procedureFields) {
            if (subset.contains(pField)) {
                result.add(pField);
            }
        }
        return result;
    }
}
