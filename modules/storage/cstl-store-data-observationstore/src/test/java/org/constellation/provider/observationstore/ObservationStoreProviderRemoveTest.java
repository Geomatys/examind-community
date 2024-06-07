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
package org.constellation.provider.observationstore;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.service.config.sos.ObservationDataset;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertPeriodEquals;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderRemoveTest extends SpringContextTest {

    @Autowired
    protected IProviderBusiness providerBusiness;

    private static ObservationProvider omPr;

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();
            Integer omPid  = testResource.createProvider(TestEnvironment.TestResource.OM2_DB, providerBusiness, null).id;

            omPr = (ObservationProvider) DataProviders.getProvider(omPid);
            initialized = true;
          }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
    }

    @Test
    public void removeFullDatasetTest() throws Exception {
        
        int NB_OBSERVATION     = 24; // contains the phenomenon directly used in the observations
        int NB_USED_PHENOMENON = 6;
        int NB_PHENOMENON      = 11;
        int NB_COMPOSITE       = 4;
        int NB_FOI             = 3;  // only 3 because 3 of the recorded procedure have no observations
        int NB_PROCEDURE       = 18; // include empty procedure
        int NB_USED_PROCEDURE  = 16; // only 16 because 2 of the recorded procedure have no observation

        // get the full content of the store
        ObservationDataset fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        List<org.opengis.observation.Process> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:2"
        * type = profile
        * 3 instant observations
        * consistant phenomenon.
        */
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:2"));
        dsQuery.setIncludeTimeForProfile(true);
        ObservationDataset dataset2 = omPr.extractResults(dsQuery);
        Assert.assertEquals(3, dataset2.getObservations().size());
        Assert.assertEquals(1, dataset2.getPhenomenons().size());
        Assert.assertEquals(1, dataset2.getFeatureOfInterest().size());
        Assert.assertEquals(1, dataset2.getProcedures().size());

        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", dataset2.getDateStart(), dataset2.getDateEnd());

        // remove this dataset from the store
        omPr.removeDataset(dataset2);


        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        NB_OBSERVATION = NB_OBSERVATION - 3;  // 3 merged observations has been removed 
        NB_USED_PROCEDURE--;                  // 1 procedure has been removed
        NB_PROCEDURE--;
         // no foi removed, still in use
         // no phenomenon removed, still in use

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // verify that the procedure has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:13"
        * type = timeseries
        * 4 period observations
        * phenomenon variating from single component, subset composite, full composite
        */
        ObservationDataset dataset13 = omPr.extractResults(new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:13")));
        Assert.assertEquals(1, dataset13.getObservations().size());
        Assert.assertEquals(1, dataset13.getPhenomenons().size());
        Assert.assertEquals(1, dataset13.getFeatureOfInterest().size());
        Assert.assertEquals(1, dataset13.getProcedures().size());

        // the most complete aggregate is returned
        Assert.assertEquals("aggregatePhenomenon-2", ((Phenomenon)dataset13.getPhenomenons().get(0)).getId());

        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.getDateStart(), dataset13.getDateEnd());

        // remove this dataset from the store
        omPr.removeDataset(dataset13);

        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        NB_OBSERVATION--;                            // 1 merged observations has been removed 
        NB_USED_PROCEDURE--;                         // 1 procedure has been removed
        NB_PROCEDURE--;
         // no foi removed, still in use
         // no phenomenon removed, still in use

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

         // verify that the procedure has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:9" AND "urn:ogc:object:sensor:GEOM:8"
        * one is a profile and the other a timeseries
        * 1 period observation and 1 instant observation
        * phenomenon variating
        *
        * the point is to remove the foi "station-006"
        */
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:9", "urn:ogc:object:sensor:GEOM:8"));
        dsQuery.setIncludeTimeForProfile(true);
        ObservationDataset datasetDouble = omPr.extractResults(dsQuery);
        Assert.assertEquals(2, datasetDouble.getObservations().size());
        Assert.assertEquals(2, datasetDouble.getPhenomenons().size());
        Assert.assertEquals(1, datasetDouble.getFeatureOfInterest().size());
        Assert.assertEquals(2, datasetDouble.getProcedures().size());

        assertPeriodEquals("2007-05-01T12:59:00Z", "2009-05-01T13:47:00Z", datasetDouble.getDateStart(), datasetDouble.getDateEnd());

        // remove this dataset from the store
        omPr.removeDataset(datasetDouble);

         // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        NB_OBSERVATION = NB_OBSERVATION - 2;        // 1 merged observations has been removed 
        NB_USED_PROCEDURE = NB_USED_PROCEDURE - 2; // 1 procedure has been removed
        NB_PROCEDURE = NB_PROCEDURE - 2;
        NB_FOI--;                                  // 1 foi removed
         // no phenomenon removed, still in use

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

         // verify that the procedure has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:multi-type"
        * type = timeseries
        * 1 period observation
        * consistant phenomenon.
        *
        * the point is to remove the phenomenon "multi-type-phenomenon"
        */
        ObservationDataset datasetMt = omPr.extractResults(new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:multi-type")));
        Assert.assertEquals(1, datasetMt.getObservations().size());
        Assert.assertEquals(1, datasetMt.getPhenomenons().size());
        Assert.assertEquals(1, datasetMt.getFeatureOfInterest().size());
        Assert.assertEquals(1, datasetMt.getProcedures().size());

        assertPeriodEquals("1980-03-01T21:52:00Z", "1981-03-01T22:52:00Z", datasetMt.getDateStart(), datasetMt.getDateEnd());

        // remove this dataset from the store
        omPr.removeDataset(datasetMt);

         // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        NB_OBSERVATION--;                            // 1 merged observations has been removed 
        NB_USED_PROCEDURE--;                         // 1 procedure has been removed
        NB_PROCEDURE--;
        NB_USED_PHENOMENON--;                        // 1 phenomenon removed
         // no foi removed, still in use

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

         // verify that the procedure has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());
    }
}
