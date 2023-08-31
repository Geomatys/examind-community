/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2019 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.AbstractSosHarvesterTest.ORIGIN_NB_SENSOR;
import static com.examind.process.sos.SosHarvesterTestUtils.getNbOffering;
import com.examind.sts.core.STSWorker;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.sos.core.SOSworker;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSHarvesterProcessErrorTest extends AbstractSosHarvesterTest {

    @Test
    public void harvestCSVSingleErrorHeaderTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");
        
        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:er";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory_1.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.SSS");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("lat");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("lon");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("temperature");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("salinity");
        in.values().add(val2);

        // bad separator
        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ProcessException error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        // i don't know why the message is prfixed with the type of the exception
        Assert.assertEquals("org.apache.sis.storage.DataStoreException: Unable to find main column(s): [time]", error.getMessage());

        /*
         * try again on two files (with same name)
        */
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory.toUri().toString());

        error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        // i don't know why the message is prfixed with the type of the exception
        Assert.assertEquals("All the files insertion failed", error.getMessage());

        /*
        * two files but one pass
        */
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory2.toUri().toString());
        ParameterValueGroup results = proc.call();
        String insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        String errorFile = results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_NAME).stringValue();
        int nbError = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, nbError);
        Assert.assertEquals("/error-header.csv", errorFile);
        Assert.assertEquals("/error-header-2.csv", insertedFile);

    }

}
