/*
 *    Examind comunity - An open source and standard compliant SDI
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
package com.examind.process.test;

import static com.examind.process.test.ParameterGroupDescriptor.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.sis.parameter.Parameters;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Process used for testing the aprameter group in/out.
 * 
 * @author Guilhem Legal (Geomatys).
 */
public class ParameterGroupProcess extends AbstractCstlProcess {


    public ParameterGroupProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }


    @Override
    protected void execute() throws ProcessException {
        // we pass through a map just for the example
        Map<String, List<String>> results = new HashMap<>();

        List<ParameterValueGroup> inputs = inputParameters.groups(IN_TEST_GROUP.getName().getCode());
        for (ParameterValueGroup group : inputs) {
            String key = group.parameter(IN_TITLE.getName().getCode()).stringValue();
            List<String> values = getValues(group, IN_VALUES.getName().getCode());
            results.put(key, values);
        }

        for (Entry<String, List<String>> entry : results.entrySet()) {
            ParameterValueGroup outGrp = outputParameters.addGroup(OUT_TEST_GROUP.getName().getCode());
            outGrp.parameter(OUT_TITLE.getName().getCode()).setValue(entry.getKey());
            for (String value : entry.getValue()) {
                ParameterValue<String> pValue = OUT_VALUES.createValue();
                pValue.setValue(value);
                outGrp.values().add(pValue);
            }
        }

    }

    /**
     * is this method somewhere in some utils?
     */
    private List getValues(final ParameterValueGroup pvg, final String descCode) {
        Parameters param = Parameters.castOrWrap(pvg);
        List results = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().getName().getCode().equals(descCode)) {
                results.add(((ParameterValue) value).getValue());
            }
        }
        return results;
    }
}
