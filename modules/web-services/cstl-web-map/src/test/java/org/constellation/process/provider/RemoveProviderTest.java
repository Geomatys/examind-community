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

import org.constellation.exception.ConfigurationException;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class RemoveProviderTest extends AbstractProviderTest {

    public RemoveProviderTest() {
        super(DeleteProviderDescriptor.NAME);
    }

    @Test
    public void testRemoveProvider() throws Exception{
        providerBusiness.removeAll();

        Integer pid = addProvider("removeProvider1",buildCSVProvider(DATASTORE_SERVICE, "removeProvider1", EMPTY_CSV, ';'));

        final int nbProvider = providerBusiness.getProviderIds().size();

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, DeleteProviderDescriptor.NAME);
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("provider_id").setValue(pid);

        final Process proc = desc.createProcess(in);
        proc.call();

        try{
            DataProvider provider = DataProviders.getProvider(pid);
            fail("Provider should not exist anymore");
        }catch(ConfigurationException ex){
            //normal provider doesn't exist anymore
        }
        assertTrue(nbProvider-1 == providerBusiness.getProviderIds().size());

        removeProvider("removeProvider1");
    }


    @Test
    public void testFailRemoveProvider() throws Exception{


        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, DeleteProviderDescriptor.NAME);
        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("provider_id").setValue(999);

        try {
            final Process proc = desc.createProcess(in);
            proc.call();
            fail();
        } catch (ProcessException ex) {

        }

    }

}
