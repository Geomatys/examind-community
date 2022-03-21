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
import org.geotoolkit.process.ProcessFinder;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class CreateProviderTest extends AbstractProviderTest {

    public CreateProviderTest () {
        super(CreateProviderDescriptor.NAME);
    }

    @Test
    public void testCreateProvider() throws Exception {
        removeProvider("newProvider");

        final int nbProvider = providerBusiness.getProviderIds().size();
        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, CreateProviderDescriptor.NAME);

        final ParameterValueGroup parameters = buildCSVProvider(DATASTORE_SERVICE, "newProvider", EMPTY_CSV, ';');
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("provider_type").setValue(DATASTORE_SERVICE.getName());
        in.parameter("source").setValue(parameters);

        final Process proc = desc.createProcess(in);
        final ParameterValueGroup outputs = proc.call();

        Integer pid = (Integer) outputs.parameter(CreateProviderDescriptor.PROVIDER_ID_NAME).getValue();

        DataProvider provider = DataProviders.getProvider(pid);

        assertTrue(nbProvider+1 == providerBusiness.getProviderIds().size());
        assertNotNull(provider);

        removeProvider("newProvider");

    }
}
