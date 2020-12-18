/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.process.provider;

import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ComparisonMode;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class GetConfigProviderTest extends AbstractProviderTest {

    public GetConfigProviderTest() {
        super(GetConfigProviderDescriptor.NAME);
    }

    @Test
    public void testGetConfigProvider() throws Exception {
        removeProvider("getConfigProvider1");

        final ParameterValueGroup parameters = buildCSVProvider(DATASTORE_SERVICE, "getConfigProvider1", EMPTY_CSV, ';');
        addProvider("getConfigProvider1",parameters);

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, GetConfigProviderDescriptor.NAME);
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(GetConfigProviderDescriptor.PROVIDER_ID_NAME).setValue("getConfigProvider1");

        final Process proc = desc.createProcess(in);
        final ParameterValueGroup outputs = proc.call();

        final DefaultParameterValueGroup val0 = (DefaultParameterValueGroup) DATASTORE_SERVICE.getProviderDescriptor().createValue();
        final ParameterValueGroup val1 = DATASTORE_SERVICE.getProviderDescriptor().createValue();
        Parameters.copy((ParameterValueGroup)outputs.parameter(GetConfigProviderDescriptor.CONFIG_NAME).getValue(), val0);
        Parameters.copy(parameters, val1);

        assertTrue(val0.equals(val1, ComparisonMode.IGNORE_METADATA));

        //assertTrue(new DefaultParameterValueGroup((ParameterValueGroup)outputs.parameter(GetConfigProviderDescriptor.CONFIG_NAME).getValue()).equals(parameters));

        removeProvider("getConfigProvider1");
    }


    @Test
    public void testFailGetConfigProvider() throws Exception {

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, GetConfigProviderDescriptor.NAME);
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(GetConfigProviderDescriptor.PROVIDER_ID_NAME).setValue("getConfigProvider2");

        try {
            final Process proc = desc.createProcess(in);
            proc.call();
            fail();
        } catch (ProcessException ex) {
            //do nothing
        }
    }
}
