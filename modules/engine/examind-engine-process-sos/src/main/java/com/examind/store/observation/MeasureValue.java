/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2025 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MeasureValue {

    public final Object value;
    public final Object[] qualityValues;
    public final Object[] parameterValues;
    public final boolean isNaN;

    public MeasureValue(Object value, Object[] qualityValues, Object[] parameterValues) {
        this.value = value;
        this.qualityValues = qualityValues;
        this.parameterValues = parameterValues;
        
        if (value instanceof Double d) {
            isNaN = Double.isNaN(d);
        } else if (value instanceof String s) {
            isNaN = s.isBlank();
        } else {
            isNaN = (value == null);
        }
    }
}
