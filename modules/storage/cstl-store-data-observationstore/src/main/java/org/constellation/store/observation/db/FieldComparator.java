/*
 *    Examind Community An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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

import java.util.Comparator;
import org.geotoolkit.observation.model.Field;

/**
 * TODO. move to geotk
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FieldComparator implements Comparator<Field>{

    @Override
    public int compare(Field f1, Field f2) {
        return Integer.compare(f1.index, f2.index);
    }

   

}
