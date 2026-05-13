/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2026 Geomatys.
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

import java.util.List;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import static org.geotoolkit.observation.model.FieldType.PARAMETER;
import static org.geotoolkit.observation.model.FieldType.QUALITY;
import org.geotoolkit.observation.model.ObservationType;
import static org.geotoolkit.observation.model.ObservationType.PROFILE;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FieldInfos {
    
    public final ObservationType observationType;
    public final boolean isProfile;
    public final List<? extends Field> measureFields;

    public FieldInfos(List<? extends Field> measureFields, ObservationType observationType) {
        this.observationType = observationType;
        this.isProfile = PROFILE.equals(observationType);
        this.measureFields = measureFields;
    }

    public Field getExtraField(String fieldName, int index, FieldType type) {
        Field field = null;
        for (Field mf : measureFields) {
            if (mf.getName().equals(fieldName)) {
                field = mf;
                break;
            }
        }
        if (field == null) throw new IllegalStateException("Unable to find a field named: " + fieldName);
        if (type  == null) throw new IllegalArgumentException("fieldtype must not be null");
        return switch (type) {
            case PARAMETER -> field.getParameterFields().get(index);
            case QUALITY   -> field.getQualityFields().get(index);
            default        -> throw new IllegalArgumentException("Only PARAMETER or QUALITY field type are expected; Following type is unsupported : " + type);
        };
    }
    
    public boolean containsMeasureField(String name) {
        return measureFields.stream().anyMatch(f -> f.getName().equals(name));
    }
    
    public MeasureBuilder newBuilder() {
        return new MeasureBuilder(this);
    }

    public Field getFieldByName(String name) {
        return measureFields.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }
}
