/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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
package org.constellation.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.sis.parameter.Parameters;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ProcessUtils {

    public static List getMultipleValues(final ParameterValueGroup pvg, final ParameterDescriptor desc) {
        Parameters param = Parameters.castOrWrap(pvg);
        return getMultipleValues(param, desc);
    }

    public static List getMultipleValues(final ParameterValueGroup pvg, final String descCode) {
        Parameters param = Parameters.castOrWrap(pvg);
        return getMultipleValues(param, descCode);
    }
    
    public static List getMultipleValues(final Parameters param, final ParameterDescriptor desc) {
        String descCode = desc.getName().getCode();
        return getMultipleValues(param, descCode);
    }

    public static List getMultipleValues(final Parameters param, final String descCode) {
        List results = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().getName().getCode().equals(descCode)) {
                results.add(((ParameterValue) value).getValue());
            }
        }
        return results;
    }

    public static void addMultipleValues(final Parameters params, Collection values, final ParameterDescriptor desc) {
        for (Object value : values) {
            ParameterValue<String> param = desc.createValue();
            param.setValue(value);
            params.values().add(param);
        }
    }
}
