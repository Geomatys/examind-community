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
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameters;

import static org.junit.Assert.*;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class UpdateProviderTest extends AbstractProviderTest {

    public UpdateProviderTest () {
        super(UpdateProviderDescriptor.NAME);
    }

    @Test
    public void testUpdateProvider() throws Exception {
        removeProvider("updateProvider10");


        Integer pid = addProvider("updateProvider10",buildCSVProvider(DATASTORE_SERVICE, "updateProvider10", EMPTY_CSV, ';'));

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, UpdateProviderDescriptor.NAME);

        final ParameterValueGroup parameters = buildCSVProvider(DATASTORE_SERVICE, "updateProvider10", EMPTY_CSV, '|');
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("provider_id").setValue(pid);
        in.parameter("source").setValue(parameters);

        final Process proc = desc.createProcess(in);
        proc.call();

        DataProvider provider = DataProviders.getProvider(pid);

        assertNotNull(provider);

        final ParameterValueGroup val = DATASTORE_SERVICE.getProviderDescriptor().createValue();
        Parameters.copy(provider.getSource(), val);
        assertEquals(parameters, val);

        removeProvider("updateProvider10");
    }

    @Test
    public void testFailUpdateProvider() throws Exception {

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, UpdateProviderDescriptor.NAME);

        final ParameterValueGroup parameters = buildCSVProvider(DATASTORE_SERVICE, "updateProvider20", EMPTY_CSV, ';');
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("provider_id").setValue(999);
        in.parameter("source").setValue(parameters);

        try {
            final Process proc = desc.createProcess(in);
            proc.call();
            fail();
        } catch (ProcessException ex) {

        }

    }
}
