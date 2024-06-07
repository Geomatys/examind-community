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
public class ObservationStoreProviderRemoveCompositePhen2Test extends SpringContextTest {

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
        
        int NB_OBSERVATION     = 24; // contains the phenomenon directly used in the observations
        int NB_USED_PHENOMENON = 6;
        int NB_PHENOMENON      = 11;
        int NB_COMPOSITE       = 4;
        int NB_FOI             = 3;  // only 3 because 3 of the recorded procedure have no observations
        int NB_PROCEDURE       = 18; // include empty procedure
        int NB_USED_PROCEDURE  = 16; // only 16 because 2 of the recorded procedure have no observation
        
        // list previous phenomenons
        List<org.opengis.observation.Phenomenon> phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        Assert.assertEquals(NB_PHENOMENON, phenomenons.size());
        
        long nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(NB_COMPOSITE, nbComposite);

        // get the full content of the store
        ObservationDataset fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // include empty procedure
        List<org.opengis.observation.Process> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());

        /*
        * delete the phenomenon "multi-type-phenomenon"
        */
        omPr.removePhenomenon("multi-type-phenomenon");


        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());

        NB_OBSERVATION--;         // 21 merged observations has been removed
        NB_USED_PHENOMENON = NB_USED_PHENOMENON - 2;  // ??
        NB_USED_PROCEDURE--; // one procedure has been removed
        NB_PROCEDURE--;
        // no foi removed
        
        Assert.assertEquals(NB_OBSERVATION,     fullDataset.getObservations().size());
        Assert.assertEquals(NB_USED_PHENOMENON, fullDataset.getPhenomenons().size());
        Assert.assertEquals(NB_FOI,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(NB_USED_PROCEDURE,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // verify that the procedures has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(NB_PROCEDURE, procedures.size());
        
        /* 
         * observations phenomenon having previously the phenomenon 'multi-type-phenprofile' is now 'depth' and as only one left field (main :().
         */
        List<String> modifiedProcedures = Arrays.asList("urn:ogc:object:sensor:GEOM:17");
        for (Observation obs : fullDataset.getObservations()) {
            Procedure p = castToModel(obs.getProcedure(), Procedure.class);
            if (modifiedProcedures.contains(p.getId())) {
                Phenomenon phen = castToModel(obs.getObservedProperty(), Phenomenon.class);
                Assert.assertEquals("depth", phen.getId());
                
                List<Field> fields = OM2Utils.getMeasureFields(obs);
                // the main field is not included
                Assert.assertEquals(0, fields.size());
            }
        }

        for (ProcedureDataset pd : fullDataset.getProcedures()) {
            if (modifiedProcedures.contains(pd.getId())) {
                // the main field is included (for profile)
                Assert.assertEquals(1, pd.getFields().size());
            }
        }
        
        // list all phenomenons
        phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        /*
        * - isHot is removed
        * - color is removed
        * - expiration is removed
        * - age is removed
        * - multi-type-phenprofile is removed because only one component was remaining
        * - multi-type-phenomenon is removed
        */
        NB_PHENOMENON = NB_PHENOMENON - 6;
        NB_COMPOSITE = NB_COMPOSITE - 2;
        
        Assert.assertEquals(NB_PHENOMENON, phenomenons.size());
        
        nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(NB_COMPOSITE, nbComposite);
    }
}
