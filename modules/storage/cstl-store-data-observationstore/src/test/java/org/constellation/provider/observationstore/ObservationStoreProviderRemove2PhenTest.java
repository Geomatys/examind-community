/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2024 Geomatys.
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
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertPeriodEquals;
import static org.constellation.provider.observationstore.ObservationTestUtils.castToModel;
import static org.constellation.provider.observationstore.ObservationTestUtils.getObservationById;
import org.constellation.store.observation.db.OM2Utils;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.observation.Observation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderRemove2PhenTest extends SpringContextTest {

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
    public void removePhenomenonTest() throws Exception {

        // list previous phenomenons
        List<org.opengis.observation.Phenomenon> phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        Assert.assertEquals(11, phenomenons.size());
        
        long nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(4L, nbComposite);
        
        // get the full content of the store
        ObservationDataset fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(23, fullDataset.getObservations().size());
        // contains the phenomenon directly used in the observations
        Assert.assertEquals(6, fullDataset.getPhenomenons().size());
        // only 3 because 3 of the recorded procedure have no observations
        Assert.assertEquals(3, fullDataset.getFeatureOfInterest().size());
        // only 15 because 2 of the recorded procedure have no observation
        Assert.assertEquals(15, fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // include empty procedure
        List<org.opengis.observation.Process> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(17, procedures.size());

        /*
        * delete the phenomenon "temperature"
        */
        omPr.removePhenomenon("temperature");


        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(22, fullDataset.getObservations().size()); // 14 merged observations has been removed
        Assert.assertEquals(4, fullDataset.getPhenomenons().size()); // 2 phenomenon removed (temperature + aggregatePhenomenon in which only one component was remaining)
        Assert.assertEquals(3, fullDataset.getFeatureOfInterest().size());  // no foi removed, still in use
        Assert.assertEquals(14, fullDataset.getProcedures().size());  // 1 procedure has been removed
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // verify that the procedures has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(16, procedures.size());
        
        /* 
         * observations phenomenon having previously the phenomenon 'aggregatePhenomenon' is now 'depth' and as now one less field.
         * unless for profile observations that aree harmonized at procedure level
         */
        List<String> depthObservations = Arrays.asList("urn:ogc:object:observation:GEOM:201", "urn:ogc:object:observation:GEOM:507");
        for (String obsId : depthObservations) {
            Observation obs = getObservationById(obsId, fullDataset.getObservations());
            Phenomenon phen = castToModel(obs.getObservedProperty(), Phenomenon.class);
            Assert.assertEquals("depth", phen.getId());
            
            boolean isProfile = castToModel(obs.getProcedure(), Procedure.class).getProperties().get("type").equals("profile");

            List<Field> fields = OM2Utils.getMeasureFields(obs);
            // the main field is not included
            if (isProfile) {
                 Assert.assertEquals(0, fields.size());
            } else {
                Assert.assertEquals(1, fields.size());
            }
        }
        
        /*
        * observations phenomenon having previously the phenomenon 'aggregatePhenomenon-2' is now a computed phenomenon and as now one less field.
        */
        List<String> aggregateObservations = Arrays.asList("urn:ogc:object:observation:GEOM:3000", 
                                                           "urn:ogc:object:observation:GEOM:4001",
                                                           "urn:ogc:object:observation:GEOM:5001", 
                                                           "urn:ogc:object:observation:GEOM:5002", 
                                                           "urn:ogc:object:observation:GEOM:5003", 
                                                           "urn:ogc:object:observation:GEOM:5004");
        for (String obsId : aggregateObservations) {
            Observation obs = getObservationById(obsId, fullDataset.getObservations());
            Phenomenon phen = castToModel(obs.getObservedProperty(), Phenomenon.class);
            Assert.assertTrue(phen.getId().startsWith("computed"));
            
            boolean isProfile = castToModel(obs.getProcedure(), Procedure.class).getProperties().get("type").equals("profile");

            List<Field> fields = OM2Utils.getMeasureFields(obs);
            // the main field is not included
            if (isProfile) {
                 Assert.assertEquals(1, fields.size());
            } else {
                Assert.assertEquals(2, fields.size());
            }
        }

        for (ProcedureDataset pd : fullDataset.getProcedures()) {
            Assert.assertFalse(pd.getFields().contains("temperature"));
        }
        
        // list all phenomenons
        phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        /*
        * - temperature is removed
        * - aggregatePhenomenon is removed because only one component was remaining
        * - aggregatePhenomenon-2 is removed and recreate as another composite without the "temperature" field
        */
        Assert.assertEquals(9, phenomenons.size());
        
        nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(3L, nbComposite);
    }
}
