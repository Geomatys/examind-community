/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

import static com.examind.process.test.MultipleTypeDescriptor.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.Parameters;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MultipleTypeProcess extends AbstractCstlProcess {


    public MultipleTypeProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final Double[] doubleArray = inputParameters.getValue(DOUBLE_ARRAY_INPUT);
        final double[] doublePrimArray = inputParameters.getValue(DOUBLE_PRIM_ARRAY_INPUT);
        final List<Double> doubleList  = getValues(inputParameters, DOUBLE_MULTIPLE_INPUT.getName().getCode());

        outputParameters.getOrCreate(DOUBLE_ARRAY_OUTPUT).setValue(doubleArray);
        outputParameters.getOrCreate(DOUBLE_PRIM_ARRAY_OUTPUT).setValue(doublePrimArray);

        for (Double dl : doubleList) {
            ParameterValue dm = (ParameterValue) DOUBLE_MULTIPLE_OUTPUT.createValue();
            dm.setValue(dl);
            outputParameters.values().add(dm);
        }

        final Integer[] intArray = inputParameters.getValue(INTEGER_ARRAY_INPUT);
        final int[] intPrimArray = inputParameters.getValue(INTEGER_PRIM_ARRAY_INPUT);
        final List<Integer> intList  = getValues(inputParameters, INTEGER_MULTIPLE_INPUT.getName().getCode());

        outputParameters.getOrCreate(INTEGER_ARRAY_OUTPUT).setValue(intArray);
        outputParameters.getOrCreate(INTEGER_PRIM_ARRAY_OUTPUT).setValue(intPrimArray);

        for (Integer il : intList) {
            ParameterValue dm = (ParameterValue) INTEGER_MULTIPLE_OUTPUT.createValue();
            dm.setValue(il);
            outputParameters.values().add(dm);
        }

        final Boolean[] booleanArray = inputParameters.getValue(BOOLEAN_ARRAY_INPUT);
        final boolean[] booleanPrimArray = inputParameters.getValue(BOOLEAN_PRIM_ARRAY_INPUT);
        final List<Boolean> booleanList  = getValues(inputParameters, BOOLEAN_MULTIPLE_INPUT.getName().getCode());

        outputParameters.getOrCreate(BOOLEAN_ARRAY_OUTPUT).setValue(booleanArray);
        outputParameters.getOrCreate(BOOLEAN_PRIM_ARRAY_OUTPUT).setValue(booleanPrimArray);

        for (Boolean dl : booleanList) {
            ParameterValue dm = (ParameterValue) BOOLEAN_MULTIPLE_OUTPUT.createValue();
            dm.setValue(dl);
            outputParameters.values().add(dm);
        }

        final Long[] longArray = inputParameters.getValue(LONG_ARRAY_INPUT);
        final long[] longPrimArray = inputParameters.getValue(LONG_PRIM_ARRAY_INPUT);
        final List<Long> longList  = getValues(inputParameters, LONG_MULTIPLE_INPUT.getName().getCode());

        outputParameters.getOrCreate(LONG_ARRAY_OUTPUT).setValue(longArray);
        outputParameters.getOrCreate(LONG_PRIM_ARRAY_OUTPUT).setValue(longPrimArray);

        for (Long dl : longList) {
            ParameterValue dm = (ParameterValue) LONG_MULTIPLE_OUTPUT.createValue();
            dm.setValue(dl);
            outputParameters.values().add(dm);
        }

        final Character[] charArray = inputParameters.getValue(CHAR_ARRAY_INPUT);
        final char[] charPrimArray = inputParameters.getValue(CHAR_PRIM_ARRAY_INPUT);
        final List<Character> charList  = getValues(inputParameters, CHAR_MULTIPLE_INPUT.getName().getCode());

        outputParameters.getOrCreate(CHAR_ARRAY_OUTPUT).setValue(charArray);
        outputParameters.getOrCreate(CHAR_PRIM_ARRAY_OUTPUT).setValue(charPrimArray);

        for (Character dl : charList) {
            ParameterValue dm = (ParameterValue) CHAR_MULTIPLE_OUTPUT.createValue();
            dm.setValue(dl);
            outputParameters.values().add(dm);
        }

        final File file = inputParameters.getValue(FILE_INPUT);
        if (!file.exists()) {
            LOGGER.warning("file does not exist");
        } else {
            outputParameters.getOrCreate(FILE_OUTPUT).setValue(file);
        }

        final Path p = inputParameters.getValue(PATH_INPUT);
        if (!Files.exists(p)) {
            LOGGER.warning("path does not exist");
        } else {
            outputParameters.getOrCreate(PATH_OUTPUT).setValue(p);
        }

    }

    private List getValues(final Parameters param, final String descCode) {
        List results = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().getName().getCode().equals(descCode)) {
                results.add(((ParameterValue) value).getValue());
            }
        }
        return results;
    }
}
