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

import java.util.Arrays;
import java.util.List;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertPeriodEquals;
import static org.constellation.provider.observationstore.ObservationTestUtils.castToModel;
import static org.constellation.provider.observationstore.ObservationTestUtils.getObservationById;
import org.constellation.store.observation.db.OM2Utils;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.Assert;
import org.junit.Test;
import org.geotoolkit.observation.model.Observation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderRemove2PhenTest extends AbstractObservationStoreProviderRemoveTest {

    @Test
    public void removePhenomenonTest() throws Exception {
        
        int nb_observation     = NB_OBSERVATION;
        int nb_used_phenomenon = NB_USED_PHENOMENON;
        int nb_phenomenon      = NB_PHENOMENON;
        int nb_composite       = NB_COMPOSITE;
        int nb_foi             = NB_FOI;
        int nb_procedure       = NB_PROCEDURE;
        int nb_used_procedure  = NB_USED_PROCEDURE;

        // list previous phenomenons
        List<Phenomenon> phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        Assert.assertEquals(nb_phenomenon, phenomenons.size());
        
        long nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(nb_composite, nbComposite);
        
        // get the full content of the store
        ObservationDataset fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(nb_observation,     fullDataset.observations.size());
        Assert.assertEquals(nb_used_phenomenon, fullDataset.phenomenons.size());
        Assert.assertEquals(nb_foi,             fullDataset.featureOfInterest.size());
        Assert.assertEquals(nb_used_procedure,  fullDataset.procedures.size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound);

        List<Procedure> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());

        /*
        * delete the phenomenon "temperature"
        */
        omPr.removePhenomenon("temperature");

        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        nb_observation     = nb_observation - 2;     // 2 merged observations has been removed 
        nb_used_phenomenon = nb_used_phenomenon - 2; // 2 phenomenon removed (temperature + aggregatePhenomenon in which only one component was remaining)
        nb_used_procedure  = nb_used_procedure - 2;  // 2 procedure has been removed
        nb_procedure       = nb_procedure - 2;
         // no foi removed, still in use

        Assert.assertEquals(nb_observation,     fullDataset.observations.size());
        Assert.assertEquals(nb_used_phenomenon, fullDataset.phenomenons.size());
        Assert.assertEquals(nb_foi,             fullDataset.featureOfInterest.size());
        Assert.assertEquals(nb_used_procedure,  fullDataset.procedures.size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound);

        // verify that the procedures has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());
        
        /* 
         * observations phenomenon having previously the phenomenon 'aggregatePhenomenon' is now 'depth' and as now one less field.
         * unless for profile observations that aree harmonized at procedure level
         */
        List<String> depthObservations = Arrays.asList("urn:ogc:object:observation:GEOM:201", "urn:ogc:object:observation:GEOM:507");
        for (String obsId : depthObservations) {
            Observation obs = getObservationById(obsId, fullDataset.observations);
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
            Observation obs = getObservationById(obsId, fullDataset.observations);
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

        for (ProcedureDataset pd : fullDataset.procedures) {
            Assert.assertFalse(pd.fields.contains("temperature"));
        }
        
        // list all phenomenons
        phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        /*
        * - temperature is removed
        * - aggregatePhenomenon is removed because only one component was remaining
        * - aggregatePhenomenon-2 is removed and recreate as another composite without the "temperature" field
        */
        Assert.assertEquals(10, phenomenons.size());
        
        nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(3L, nbComposite);
    }
}
