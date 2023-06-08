/*
 *    Examind - An open source and standard compliant SDI
 *    https://www.examind.com/
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
package org.constellation.sos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import static org.constellation.provider.observationstore.ObservationTestUtils.*;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.util.Util;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.observation.Process;
import org.opengis.temporal.Period;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseDataStoreRemoveTest {

    private static TestEnvironment.TestResources testResource;

    private ObservationStore store;

    @BeforeClass
    public static void setUp() throws Exception {
        testResource = initDataDirectory();
    }

    private ObjectMapper mapper;

    @Before
    public void before() {
        mapper = ObservationJsonUtils.getMapper();
        store = (ObservationStore) testResource.createStore(TestEnvironment.TestResource.OM2_DB);
    }
    
    @Test
    public void removeFullDatasetTest() throws Exception {

        // get the full content of the store
        ObservationDataset fullDataset = store.getDataset(new DatasetQuery());

        Assert.assertEquals(23, fullDataset.observations.size());
        // contains the phenomenon directly used in the observations
        Assert.assertEquals(6, fullDataset.phenomenons.size());
        Assert.assertEquals(17, fullDataset.offerings.size());
        // only 3 because 3 of the recorded procedure have no observations
        Assert.assertEquals(3, fullDataset.featureOfInterest.size());
        // only 14 because 2 of the recorded procedure have no observation
        Assert.assertEquals(15, fullDataset.procedures.size());
        Assert.assertTrue(fullDataset.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound.getTimeObject());

        // include empty procedure
        List<Process> procedures = store.getProcedures(new ProcedureQuery());
        Assert.assertEquals(17, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:2"
        * type = profile
        * 3 instant observations
        * consistant phenomenon.
        */
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:2"));
        dsQuery.setIncludeTimeForProfile(true);
        ObservationDataset dataset2 = store.getDataset(dsQuery);
        Assert.assertEquals(3, dataset2.observations.size());
        Assert.assertEquals(1, dataset2.phenomenons.size());
        Assert.assertEquals(1, dataset2.offerings.size());
        Assert.assertEquals(1, dataset2.featureOfInterest.size());
        Assert.assertEquals(1, dataset2.procedures.size());

        Assert.assertTrue(dataset2.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", dataset2.spatialBound.getTimeObject());

        // remove this dataset from the store
        store.getWriter().removeDataSet(dataset2);


        // get the full content of the store to verify the deletion
        fullDataset = store.getDataset(new DatasetQuery());

        Assert.assertEquals(20, fullDataset.observations.size()); // 1 merged observations has been removed
        Assert.assertEquals(6, fullDataset.phenomenons.size()); // no phenomenon removed, still in use
        Assert.assertEquals(16, fullDataset.offerings.size());  // 1 offering has been removed
        Assert.assertEquals(3, fullDataset.featureOfInterest.size());  // no foi removed, still in use
        Assert.assertEquals(14, fullDataset.procedures.size());  // 1 procedure has been removed
        Assert.assertTrue(fullDataset.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound.getTimeObject());

        // verify that the procedure has been totaly removed
        procedures = store.getProcedures(new ProcedureQuery());
        Assert.assertEquals(16, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:13"
        * type = timeseries
        * 4 period observations
        * phenomenon variating from single component, subset composite, full composite
        */
        ObservationDataset dataset13 = store.getDataset(new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:13")));
        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertEquals(1, dataset13.phenomenons.size());
        Assert.assertEquals(1, dataset13.offerings.size());
        Assert.assertEquals(1, dataset13.featureOfInterest.size());
        Assert.assertEquals(1, dataset13.procedures.size());

        // the most complete aggregate is returned
        Assert.assertEquals("aggregatePhenomenon-2", dataset13.phenomenons.get(0).getId());

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        // remove this dataset from the store
        store.getWriter().removeDataSet(dataset13);


        // get the full content of the store to verify the deletion
        fullDataset = store.getDataset(new DatasetQuery());

        Assert.assertEquals(19, fullDataset.observations.size()); // 1 merged observations has been removed
        Assert.assertEquals(6, fullDataset.phenomenons.size());  // no phenomenon removed, still in use
        Assert.assertEquals(15, fullDataset.offerings.size()); // 1 offering has been removed
        Assert.assertEquals(3, fullDataset.featureOfInterest.size());  // no foi removed, still in use
        Assert.assertEquals(13, fullDataset.procedures.size());  // 1 procedure has been removed
        Assert.assertTrue(fullDataset.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound.getTimeObject());

         // verify that the procedure has been totaly removed
        procedures = store.getProcedures(new ProcedureQuery());
        Assert.assertEquals(15, procedures.size());
        
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
        ObservationDataset datasetDouble = store.getDataset(dsQuery);
        Assert.assertEquals(2, datasetDouble.observations.size());
        Assert.assertEquals(2, datasetDouble.phenomenons.size());
        Assert.assertEquals(2, datasetDouble.offerings.size());
        Assert.assertEquals(1, datasetDouble.featureOfInterest.size());
        Assert.assertEquals(2, datasetDouble.procedures.size());

        Assert.assertTrue(datasetDouble.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2007-05-01T12:59:00Z", "2009-05-01T13:47:00Z", datasetDouble.spatialBound.getTimeObject());

        // remove this dataset from the store
        store.getWriter().removeDataSet(datasetDouble);

         // get the full content of the store to verify the deletion
        fullDataset = store.getDataset(new DatasetQuery());

        Assert.assertEquals(17, fullDataset.observations.size()); // 2 observations has been removed
        Assert.assertEquals(6, fullDataset.phenomenons.size());  // no phenomenon removed, still in use
        Assert.assertEquals(13, fullDataset.offerings.size()); // 2 offering has been removed
        Assert.assertEquals(2, fullDataset.featureOfInterest.size());  // 1 foi removed
        Assert.assertEquals(11, fullDataset.procedures.size());  // 2 procedure has been removed
        Assert.assertTrue(fullDataset.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound.getTimeObject());

         // verify that the procedure has been totaly removed
        procedures = store.getProcedures(new ProcedureQuery());
        Assert.assertEquals(13, procedures.size());

        /*
        * retrieve the dataset for sensor "urn:ogc:object:sensor:GEOM:multi-type"
        * type = timeseries
        * 1 period observation
        * consistant phenomenon.
        *
        * the point is to remove the phenomenon "multi-type-phenomenon"
        */
        ObservationDataset datasetMt = store.getDataset(new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:multi-type")));
        Assert.assertEquals(1, datasetMt.observations.size());
        Assert.assertEquals(1, datasetMt.phenomenons.size());
        Assert.assertEquals(1, datasetMt.offerings.size());
        Assert.assertEquals(1, datasetMt.featureOfInterest.size());
        Assert.assertEquals(1, datasetMt.procedures.size());

        Assert.assertTrue(datasetMt.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "1981-03-01T22:52:00Z", datasetMt.spatialBound.getTimeObject());

        // remove this dataset from the store
        store.getWriter().removeDataSet(datasetMt);

         // get the full content of the store to verify the deletion
        fullDataset = store.getDataset(new DatasetQuery());

        Assert.assertEquals(16, fullDataset.observations.size()); // 1 observations has been removed
        Assert.assertEquals(5, fullDataset.phenomenons.size());  // 1 phenomenon removed
        Assert.assertEquals(12, fullDataset.offerings.size()); // 1 offering has been removed
        Assert.assertEquals(2, fullDataset.featureOfInterest.size());  // no foi removed
        Assert.assertEquals(10, fullDataset.procedures.size());  // 1 procedure has been removed
        Assert.assertTrue(fullDataset.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound.getTimeObject());

         // verify that the procedure has been totaly removed
        procedures = store.getProcedures(new ProcedureQuery());
        Assert.assertEquals(12, procedures.size());
    }

    @Test
    public void removePartialDatasetTest() throws Exception {

        // look for the current procedure dataset observation results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:14"));
        dsQuery.setIncludeTimeForProfile(true);
        dsQuery.setSeparatedProfileObservation(false);
        ObservationDataset dataset14 = store.getDataset(dsQuery);
        Assert.assertEquals(1, dataset14.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-24T00:00:00Z", dataset14.spatialBound.getTimeObject());

        Assert.assertTrue(dataset14.observations.get(0).getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) dataset14.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(28), cr.getNbValues());

        String values = "2000-12-01T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-01T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-01T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-01T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-01T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-01T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-01T00:00:00.0,31.1,12.2,@@" +
                        "2000-12-11T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-11T00:00:00.0,19.7,12.9,@@" +
                        "2000-12-11T00:00:00.0,21.2,13.0,@@" +
                        "2000-12-11T00:00:00.0,23.9,13.1,@@" +
                        "2000-12-11T00:00:00.0,24.2,13.2,@@" +
                        "2000-12-11T00:00:00.0,29.4,13.3,@@" +
                        "2000-12-11T00:00:00.0,31.1,13.4,@@" +
                        "2000-12-22T00:00:00.0,18.5,12.8,5.1@@" +
                        "2000-12-22T00:00:00.0,19.7,12.7,5.2@@" +
                        "2000-12-22T00:00:00.0,21.2,12.6,5.3@@" +
                        "2000-12-22T00:00:00.0,23.9,12.5,5.4@@" +
                        "2000-12-22T00:00:00.0,24.2,12.4,5.5@@" +
                        "2000-12-22T00:00:00.0,29.4,12.3,5.6@@" +
                        "2000-12-22T00:00:00.0,31.1,12.2,5.7@@" +
                        "2000-12-24T00:00:00.0,18.5,12.8,5.1@@" +
                        "2000-12-24T00:00:00.0,19.7,12.9,5.0@@" +
                        "2000-12-24T00:00:00.0,21.2,13.0,4.9@@" +
                        "2000-12-24T00:00:00.0,23.9,13.1,4.8@@" +
                        "2000-12-24T00:00:00.0,24.2,13.2,4.7@@" +
                        "2000-12-24T00:00:00.0,29.4,13.3,4.6@@" +
                        "2000-12-24T00:00:00.0,31.1,13.4,4.5@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:14"
        * type = profile
        * 1 instant observation in the middle of the 4 that has the full dataset
        *
        * the point is to remove the depth field and so all the measure since there is no other field non-empty
        */
        ObservationDataset datasetPartial14Temp = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-14-temp.json"), ObservationDataset.class);
        
        // remove this dataset from the store
        store.getWriter().removeDataSet(datasetPartial14Temp);

        // verify that some fields have been emptied
        // look for the current procedure dataset observation results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:14"));
        dsQuery.setIncludeTimeForProfile(true);
        dsQuery.setSeparatedProfileObservation(false);
        dataset14 = store.getDataset(dsQuery);
        Assert.assertEquals(1, dataset14.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-24T00:00:00Z", dataset14.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset14.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset14.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(21), cr.getNbValues());

        values        = "2000-12-01T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-01T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-01T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-01T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-01T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-01T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-01T00:00:00.0,31.1,12.2,@@" +
                        "2000-12-22T00:00:00.0,18.5,12.8,5.1@@" +
                        "2000-12-22T00:00:00.0,19.7,12.7,5.2@@" +
                        "2000-12-22T00:00:00.0,21.2,12.6,5.3@@" +
                        "2000-12-22T00:00:00.0,23.9,12.5,5.4@@" +
                        "2000-12-22T00:00:00.0,24.2,12.4,5.5@@" +
                        "2000-12-22T00:00:00.0,29.4,12.3,5.6@@" +
                        "2000-12-22T00:00:00.0,31.1,12.2,5.7@@" +
                        "2000-12-24T00:00:00.0,18.5,12.8,5.1@@" +
                        "2000-12-24T00:00:00.0,19.7,12.9,5.0@@" +
                        "2000-12-24T00:00:00.0,21.2,13.0,4.9@@" +
                        "2000-12-24T00:00:00.0,23.9,13.1,4.8@@" +
                        "2000-12-24T00:00:00.0,24.2,13.2,4.7@@" +
                        "2000-12-24T00:00:00.0,29.4,13.3,4.6@@" +
                        "2000-12-24T00:00:00.0,31.1,13.4,4.5@@";

        Assert.assertEquals(values, cr.getValues());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:14"
        * type = profile
        * 1 instant observation in the middle of the 3 that has the full dataset
        *
        * the point is to remove the salinity field and so another existing composite is set to the observation
        */
        ObservationDataset datasetPartial14Salinity = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-14-salinity.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial14Salinity);

        dataset14 = store.getDataset(dsQuery);
        Assert.assertEquals(1, dataset14.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-24T00:00:00Z", dataset14.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset14.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset14.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(21), cr.getNbValues());

        values        = "2000-12-01T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-01T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-01T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-01T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-01T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-01T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-01T00:00:00.0,31.1,12.2,@@" +
                        "2000-12-22T00:00:00.0,18.5,12.8,@@" +// start emptied
                        "2000-12-22T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-22T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-22T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-22T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-22T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-22T00:00:00.0,31.1,12.2,@@" +// end emptied
                        "2000-12-24T00:00:00.0,18.5,12.8,5.1@@" +
                        "2000-12-24T00:00:00.0,19.7,12.9,5.0@@" +
                        "2000-12-24T00:00:00.0,21.2,13.0,4.9@@" +
                        "2000-12-24T00:00:00.0,23.9,13.1,4.8@@" +
                        "2000-12-24T00:00:00.0,24.2,13.2,4.7@@" +
                        "2000-12-24T00:00:00.0,29.4,13.3,4.6@@" +
                        "2000-12-24T00:00:00.0,31.1,13.4,4.5@@";

        Assert.assertEquals(values, cr.getValues());

         /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:14"
        * type = profile
        * 1 instant observation at the end of the 3 that has the full dataset
        *
        * the point is to remove the temperature field and so another unexisting composite is set to the observation
        */
        ObservationDataset datasetPartial14Temp2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-14-temp2.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial14Temp2);

        dataset14 = store.getDataset(dsQuery);
        Assert.assertEquals(1, dataset14.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-24T00:00:00Z", dataset14.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset14.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset14.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(21), cr.getNbValues());

        values        = "2000-12-01T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-01T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-01T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-01T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-01T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-01T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-01T00:00:00.0,31.1,12.2,@@" +
                        "2000-12-22T00:00:00.0,18.5,12.8,@@" +
                        "2000-12-22T00:00:00.0,19.7,12.7,@@" +
                        "2000-12-22T00:00:00.0,21.2,12.6,@@" +
                        "2000-12-22T00:00:00.0,23.9,12.5,@@" +
                        "2000-12-22T00:00:00.0,24.2,12.4,@@" +
                        "2000-12-22T00:00:00.0,29.4,12.3,@@" +
                        "2000-12-22T00:00:00.0,31.1,12.2,@@" +
                        "2000-12-24T00:00:00.0,18.5,,5.1@@"  + // start emptied
                        "2000-12-24T00:00:00.0,19.7,,5.0@@"  +
                        "2000-12-24T00:00:00.0,21.2,,4.9@@"  +
                        "2000-12-24T00:00:00.0,23.9,,4.8@@"  +
                        "2000-12-24T00:00:00.0,24.2,,4.7@@"  +
                        "2000-12-24T00:00:00.0,29.4,,4.6@@"  +
                        "2000-12-24T00:00:00.0,31.1,,4.5@@";   // end emptied

        Assert.assertEquals(values, cr.getValues());


        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:10"
        * type = timeseries
        * 1 period observation intersecting the first observation
        *
        * the point is to partially remove measures
        */

        // verify previous results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:10"));
        ObservationDataset dataset10 = store.getDataset(dsQuery);

        Assert.assertEquals(2, dataset10.observations.size()); // 2 observervations because of different foi
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:04:00Z", dataset10.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset10.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset10.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(2), cr.getNbValues());

        values = "2009-05-01T13:47:00.0,4.5@@" +
                 "2009-05-01T14:00:00.0,5.9@@";
        Assert.assertEquals(values, cr.getValues());

        Assert.assertTrue(dataset10.observations.get(1).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset10.observations.get(1).getResult();
        Assert.assertEquals(Integer.valueOf(4), cr.getNbValues());

        values = "2009-05-01T14:01:00.0,8.9@@" +
                 "2009-05-01T14:02:00.0,7.8@@" +
                 "2009-05-01T14:03:00.0,9.9@@" +
                 "2009-05-01T14:04:00.0,9.1@@";
        Assert.assertEquals(values, cr.getValues());


        ObservationDataset datasetPartial10 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-10-1.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial10);

        // verify deletion
        dataset10 = store.getDataset(dsQuery);

        Assert.assertEquals(2, dataset10.observations.size()); // 2 observervations because of different foi
        assertPeriodEquals("2009-05-01T14:00:00Z", "2009-05-01T14:04:00Z", dataset10.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset10.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset10.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(1), cr.getNbValues());

        values = "2009-05-01T14:00:00.0,5.9@@";
        Assert.assertEquals(values, cr.getValues());

        Assert.assertTrue(dataset10.observations.get(1).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset10.observations.get(1).getResult();
        Assert.assertEquals(Integer.valueOf(4), cr.getNbValues());

        values = "2009-05-01T14:01:00.0,8.9@@" +
                 "2009-05-01T14:02:00.0,7.8@@" +
                 "2009-05-01T14:03:00.0,9.9@@" +
                 "2009-05-01T14:04:00.0,9.1@@";
        Assert.assertEquals(values, cr.getValues());


        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 1 period observation in the middle of the single observation
        *
        * the point is to empty some measure field.
        * bonus this sensor is in multi table mode.
        */

        // verify previous results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:12"));
        ObservationDataset dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                 "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                 "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                 "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());


        ObservationDataset datasetPartial12 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-12.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                 "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                 "2009-12-11T14:01:00.0,,78.5,@@" + // emptied
                 "2009-12-15T14:02:00.0,,14.5,@@" + // emptied
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());

       /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 1 period observation in the middle of the single observation
        *
        * the point is to empty some measure field, and so the line finish empty
        * bonus this sensor is in multi table mode.
        */
         ObservationDataset datasetPartial12_2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-12-2.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12_2);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(3), cr.getNbValues());

        values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                 "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());

         /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 1 period observation meeting the single observation
        *
        * the point is to remove the first measure
        */
        ObservationDataset datasetPartial12_3 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-12-3.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12_3);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2009-12-01T14:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());

        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(2), cr.getNbValues());

        values = "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());

         /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 1 period observation meeting the single observation
        *
        * the point is to remove the last measure
        */
        ObservationDataset datasetPartial12_4 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-12-4.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12_4);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(1), cr.getNbValues());

        values = "2009-12-01T14:00:00.0,5.9,1.5,3.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:13"
        * type = timeseries
        * 1 period observation at the start of the 4 observation
        *
        * the point is to create empty lines to test the removal
        */

        // verify previous results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:13"));
        ObservationDataset dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertEquals("aggregatePhenomenon-2", dataset13.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(13), cr.getNbValues());

        values = "2000-01-01T00:00:00.0,4.5,98.5,@@" +
                 "2000-02-01T00:00:00.0,4.6,97.5,@@" +
                 "2000-03-01T00:00:00.0,4.7,97.5,@@" +
                 "2000-04-01T00:00:00.0,4.8,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                 "2000-11-01T00:00:00.0,,96.5,@@" +
                 "2000-12-01T00:00:00.0,,99.5,@@" +
                 "2001-01-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());


        // first we empty the 3 first temperature fields
        ObservationDataset datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-1.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon-2", dataset13.observations.get(0).getObservedProperty().getId());
         
        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(13), cr.getNbValues());

        values = "2000-01-01T00:00:00.0,4.5,,@@" +
                 "2000-02-01T00:00:00.0,4.6,,@@" +
                 "2000-03-01T00:00:00.0,4.7,,@@" +
                 "2000-04-01T00:00:00.0,4.8,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                 "2000-11-01T00:00:00.0,,96.5,@@" +
                 "2000-12-01T00:00:00.0,,99.5,@@" +
                 "2001-01-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());

        // now we perform a remove on the depth field on the first observation. this will end up emptying the 3 first lines
        datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-2.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-04-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(10), cr.getNbValues());

        values = "2000-04-01T00:00:00.0,,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                 "2000-11-01T00:00:00.0,,96.5,@@" +
                 "2000-12-01T00:00:00.0,,99.5,@@" +
                 "2001-01-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());

        // now we empty the 2 last line
        datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-3.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-04-01T00:00:00Z", "2000-11-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertEquals("aggregatePhenomenon-2", dataset13.observations.get(0).getObservedProperty().getId());
        
        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(8), cr.getNbValues());

        values = "2000-04-01T00:00:00.0,,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                 "2000-11-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());

        // now we empty the temperature field for the two last line (each are from a diferent observation)
        // the last line will be empty, remove the observation
        datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-4.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-04-01T00:00:00Z", "2000-10-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertEquals("aggregatePhenomenon-2", dataset13.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(7), cr.getNbValues());

        values = "2000-04-01T00:00:00.0,,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,,1.3@@";
        Assert.assertEquals(values, cr.getValues());

         // now we empty the depth and salinity fields the last observation
        // the last line will be empty,  and need to be removed
        datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-5.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertTrue(dataset13.spatialBound.getTimeObject() instanceof Period);
        assertPeriodEquals("2000-04-01T00:00:00Z", "2000-09-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertEquals(1, dataset13.observations.size());
        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(6), cr.getNbValues());

        values = "2000-04-01T00:00:00.0,,96.5,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,,98.5,@@" +
                 "2000-09-01T00:00:00.0,,87.5,@@";
        Assert.assertEquals(values, cr.getValues());

    }

    @Test
    public void removeMeasurementDatasetTest() throws Exception {

        // verify previous results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:12"));
        ObservationDataset dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        Assert.assertEquals("aggregatePhenomenon-2", dataset12.observations.get(0).getObservedProperty().getId());
        
        ComplexResult cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        String values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                        "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                        "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                        "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                        "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());


        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 1 single measure in the middle of the single observation
        *
        * the point is to empty a measure field.
        * bonus this sensor is in multi table mode.
        */

        ObservationDataset datasetPartial12 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-12-1.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon-2", dataset12.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                 "2009-12-01T14:00:00.0,,1.5,3.0@@"     + // emptied
                 "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" + 
                 "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 2 single measure in the middle of the single observation
        *
        * the point is to empty all the measure left in the previous modified line.
        * bonus this sensor is in multi table mode.
        */

        datasetPartial12 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-12-2.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial12);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon-2", dataset12.observations.get(0).getObservedProperty().getId());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(4), cr.getNbValues());

        values = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                 "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                 "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                 "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());
        
        /*
         * remove values until only the last line is still here
        */
        datasetPartial12 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-12-5.json"), ObservationDataset.class);
        store.getWriter().removeDataSet(datasetPartial12);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset12.observations.size());
        assertInstantEquals("2012-12-22T00:00:00Z", dataset12.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon-2", dataset12.observations.get(0).getObservedProperty().getId());
        
        Assert.assertTrue(dataset12.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset12.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(1), cr.getNbValues());

        values = "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:12"
        * type = timeseries
        * 3 single measure equals to the single (complex) measure (left) existing
        *
        * the point is to remove the observation
        */

        datasetPartial12 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-12-3.json"), ObservationDataset.class);
        store.getWriter().removeDataSet(datasetPartial12);

        // verify deletion
        dataset12 = store.getDataset(dsQuery);

        Assert.assertEquals(0, dataset12.observations.size());

        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:7"
        * type = timeseries
        * 1 single measure equals to the single measure existing
        *
        * the point is to remove the observation
        */

        // verify previous results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:7"));
        ObservationDataset dataset7 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset7.observations.size());
        assertInstantEquals("2007-05-01T16:59:00Z", dataset7.spatialBound.getTimeObject());
        Assert.assertTrue(dataset7.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset7.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(1), cr.getNbValues());

        values = "2007-05-01T16:59:00.0,6.56@@";
        Assert.assertEquals(values, cr.getValues());

        dataset7 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-7.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(dataset7);

        // verify deletion
        dataset7 = store.getDataset(dsQuery);

        Assert.assertEquals(0, dataset7.observations.size());


       /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:test-id"
        * type = timeseries
        * 2 single measure at start and end of the single observation
        *
        * the point is to remove lines whith measurement.
        */

        // verify previous results
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:test-id"));
        ObservationDataset dataset11 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset11.observations.size());
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:03:00Z", dataset11.spatialBound.getTimeObject());
        Assert.assertEquals("depth", dataset11.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset11.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset11.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        values = "2009-05-01T13:47:00.0,4.5@@" +
                 "2009-05-01T14:00:00.0,5.9@@" +
                 "2009-05-01T14:01:00.0,8.9@@" +
                 "2009-05-01T14:02:00.0,7.8@@" +
                 "2009-05-01T14:03:00.0,9.9@@";
        Assert.assertEquals(values, cr.getValues());

        dataset11 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-11.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(dataset11);

        // verify deletion
        dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:test-id"));
        dataset11 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset11.observations.size());
        Assert.assertEquals("depth", dataset11.observations.get(0).getObservedProperty().getId());
        assertPeriodEquals("2009-05-01T14:00:00Z", "2009-05-01T14:02:00Z", dataset11.spatialBound.getTimeObject());

        Assert.assertTrue(dataset11.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset11.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(3), cr.getNbValues());

        values = "2009-05-01T14:00:00.0,5.9@@" +
                 "2009-05-01T14:01:00.0,8.9@@" +
                 "2009-05-01T14:02:00.0,7.8@@";
        Assert.assertEquals(values, cr.getValues());

        //System.out.println(mapper.writeValueAsString(dataset7));
    }

    @Test
    public void removePartialDataset2Test() throws Exception {

         // verify previous results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:13"));
        ObservationDataset dataset13 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset13.observations.size());
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(13), cr.getNbValues());

        String values = "2000-01-01T00:00:00.0,4.5,98.5,@@" +
                        "2000-02-01T00:00:00.0,4.6,97.5,@@" +
                        "2000-03-01T00:00:00.0,4.7,97.5,@@" +
                        "2000-04-01T00:00:00.0,4.8,96.5,@@" +
                        "2000-05-01T00:00:00.0,4.9,,@@" +
                        "2000-06-01T00:00:00.0,5.0,,@@" +
                        "2000-07-01T00:00:00.0,5.1,,@@" +
                        "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                        "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                        "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                        "2000-11-01T00:00:00.0,,96.5,@@" +
                        "2000-12-01T00:00:00.0,,99.5,@@" +
                        "2001-01-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());


        /*
        * use a partial dataset for sensor "urn:ogc:object:sensor:GEOM:13"
        * type = timeseries
        * 1 observation containing the full first observation + one measure of the second
        *
        * only temperature field
        * the point is to change phenomenon on the first observation in an intersect context
        */

        ObservationDataset datasetPartial13 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-13-6.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial13);

        // verify deletion
        dataset13 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset13.observations.size());
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", dataset13.spatialBound.getTimeObject());

        Assert.assertTrue(dataset13.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset13.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(13), cr.getNbValues());

        values = "2000-01-01T00:00:00.0,4.5,,@@" +
                 "2000-02-01T00:00:00.0,4.6,,@@" +
                 "2000-03-01T00:00:00.0,4.7,,@@" +
                 "2000-04-01T00:00:00.0,4.8,,@@" +
                 "2000-05-01T00:00:00.0,4.9,,@@" +
                 "2000-06-01T00:00:00.0,5.0,,@@" +
                 "2000-07-01T00:00:00.0,5.1,,@@" +
                 "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                 "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                 "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                 "2000-11-01T00:00:00.0,,96.5,@@" +
                 "2000-12-01T00:00:00.0,,99.5,@@" +
                 "2001-01-01T00:00:00.0,,96.5,@@";
        Assert.assertEquals(values, cr.getValues());
    }

    @Test
    public void removePartialDataset3Test() throws Exception {

         // verify previous results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:8"));
        ObservationDataset dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        String values = "2007-05-01T12:59:00.0,6.56,12.0@@" +
                        "2007-05-01T13:59:00.0,6.56,13.0@@" +
                        "2007-05-01T14:59:00.0,6.56,14.0@@" +
                        "2007-05-01T15:59:00.0,6.56,15.0@@" +
                        "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        // first we empty the first depth measure
        ObservationDataset datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-8.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        values = "2007-05-01T12:59:00.0,,12.0@@" +
                 "2007-05-01T13:59:00.0,6.56,13.0@@" +
                 "2007-05-01T14:59:00.0,6.56,14.0@@" +
                 "2007-05-01T15:59:00.0,6.56,15.0@@" +
                 "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * we remove all the temperature measure
        * leading to an empy line at the beginning.
        */
        datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-8-1.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T13:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("depth", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(4), cr.getNbValues());

        // TODO here we see that there is still a temperature field. should we remove it?
        values = "2007-05-01T13:59:00.0,6.56,@@" +
                 "2007-05-01T14:59:00.0,6.56,@@" +
                 "2007-05-01T15:59:00.0,6.56,@@" +
                 "2007-05-01T16:59:00.0,6.56,@@";
        Assert.assertEquals(values, cr.getValues());
    }

    @Test
    public void removePartialDataset4Test() throws Exception {

         // verify previous results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:8"));
        ObservationDataset dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        String values = "2007-05-01T12:59:00.0,6.56,12.0@@" +
                        "2007-05-01T13:59:00.0,6.56,13.0@@" +
                        "2007-05-01T14:59:00.0,6.56,14.0@@" +
                        "2007-05-01T15:59:00.0,6.56,15.0@@" +
                        "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        // first we empty the first depth measure
        ObservationDataset datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-8.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        values = "2007-05-01T12:59:00.0,,12.0@@" +
                 "2007-05-01T13:59:00.0,6.56,13.0@@" +
                 "2007-05-01T14:59:00.0,6.56,14.0@@" +
                 "2007-05-01T15:59:00.0,6.56,15.0@@" +
                 "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * now we remove the 4 last line.
        * the point is to change the phenomenon.
        */
        datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-8-2.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertInstantEquals("2007-05-01T12:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("temperature", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(1), cr.getNbValues());

        // TODO here we see that there is still a temperature field. should we remove it?
        values = "2007-05-01T12:59:00.0,,12.0@@";
        Assert.assertEquals(values, cr.getValues());
    }

    @Test
    public void removePartialDataset5Test() throws Exception {

         // verify previous results
        DatasetQuery dsQuery = new DatasetQuery(Arrays.asList("urn:ogc:object:sensor:GEOM:8"));
        ObservationDataset dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        String values = "2007-05-01T12:59:00.0,6.56,12.0@@" +
                        "2007-05-01T13:59:00.0,6.56,13.0@@" +
                        "2007-05-01T14:59:00.0,6.56,14.0@@" +
                        "2007-05-01T15:59:00.0,6.56,15.0@@" +
                        "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        // first we empty the first depth measure
        ObservationDataset datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-measurement-8.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("aggregatePhenomenon", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());


        values = "2007-05-01T12:59:00.0,,12.0@@" +
                 "2007-05-01T13:59:00.0,6.56,13.0@@" +
                 "2007-05-01T14:59:00.0,6.56,14.0@@" +
                 "2007-05-01T15:59:00.0,6.56,15.0@@" +
                 "2007-05-01T16:59:00.0,6.56,16.0@@";
        Assert.assertEquals(values, cr.getValues());

        /*
        * now we empty the 4 last line depth.
        * the point is to change the phenomenon.
        */
        datasetPartial8 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/dataset-partial-8-3.json"), ObservationDataset.class);

        store.getWriter().removeDataSet(datasetPartial8);

        dataset8 = store.getDataset(dsQuery);

        Assert.assertEquals(1, dataset8.observations.size());
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", dataset8.spatialBound.getTimeObject());
        Assert.assertEquals("temperature", dataset8.observations.get(0).getObservedProperty().getId());

        Assert.assertTrue(dataset8.observations.get(0).getResult() instanceof ComplexResult);
        cr = (ComplexResult) dataset8.observations.get(0).getResult();
        Assert.assertEquals(Integer.valueOf(5), cr.getNbValues());

        // TODO here we see that there is still a temperature field. should we remove it?
        values = "2007-05-01T12:59:00.0,,12.0@@" +
                 "2007-05-01T13:59:00.0,,13.0@@" +
                 "2007-05-01T14:59:00.0,,14.0@@" +
                 "2007-05-01T15:59:00.0,,15.0@@" +
                 "2007-05-01T16:59:00.0,,16.0@@";
        Assert.assertEquals(values, cr.getValues());
    }

}
