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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertPeriodEquals;
import static org.constellation.provider.observationstore.ObservationTestUtils.castToModel;
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
public class ObservationStoreProviderRemoveCompositePhenTest extends AbstractObservationStoreProviderRemoveTest {

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

        // include empty procedure
        List<Procedure> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());

        /*
        * delete the phenomenon "aggregatePhenomenon"
        */
        omPr.removePhenomenon("aggregatePhenomenon");


        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());
        
        nb_observation     = nb_observation    - 21;  // 21 merged observations has been removed
        nb_used_phenomenon = nb_used_phenomenon - 4;  // 4 phenomenon removed (depth + temperature + aggregatePhenomenon + aggregatePhenomenon-2 in which only one component was remaining)
        nb_used_procedure  = nb_used_procedure - 13;  // 13 procedure has been removed
        nb_procedure       = nb_procedure      - 13;
        nb_foi--;                                     // one foi removed
        
        Set<String> expectedResultIds = new HashSet<>();
        expectedResultIds.add("urn:ogc:object:observation:GEOM:3000");
        expectedResultIds.add("urn:ogc:object:observation:GEOM:4002");
        expectedResultIds.add("urn:ogc:object:observation:GEOM:7001");
        expectedResultIds.add("urn:ogc:object:observation:GEOM:9002");
        
        Set<String> resultIds = fullDataset.observations.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());

        Assert.assertEquals("expected:" + expectedResultIds + " but was " + resultIds, nb_observation,     fullDataset.observations.size());
        Assert.assertEquals(nb_used_phenomenon, fullDataset.phenomenons.size());
        Assert.assertEquals(nb_foi,             fullDataset.featureOfInterest.size());
        Assert.assertEquals(nb_used_procedure,  fullDataset.procedures.size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.spatialBound);

        // verify that the procedures has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());
        
        /* 
         * observations phenomenon having previously the phenomenon 'aggregatePhenomenon-2' is now 'salinity' and as now 2 less field.
         */
        List<String> modifiedProcedures = Arrays.asList("urn:ogc:object:sensor:GEOM:12", "urn:ogc:object:sensor:GEOM:13");
        for (Observation obs : fullDataset.observations) {
            Procedure p = castToModel(obs.getProcedure(), Procedure.class);
            if (modifiedProcedures.contains(p.getId())) {
                Phenomenon phen = castToModel(obs.getObservedProperty(), Phenomenon.class);
                Assert.assertEquals("salinity", phen.getId());
                
                List<Field> fields = OM2Utils.getMeasureFields(obs);
                // the main field is not included
                Assert.assertEquals(1, fields.size());
            }
        }

        for (ProcedureDataset pd : fullDataset.procedures) {
            if (modifiedProcedures.contains(pd.getId())) {
                // the main field is not included
                Assert.assertEquals(1, pd.fields.size());
            }
        }
        
        // list all phenomenons
        phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        /*
        * - temperature is removed
        * - depth is removed
        * - aggregatePhenomenon is removed
        * - aggregatePhenomenon-2 is removed because only one component was remaining
        * - multi-type-phenprofile is removed because all the procedure using it were profile with main field "depth"
        * - metadata is removed  is removed because it was used only by the composite multi-type-phenprofile
        */
        nb_phenomenon = nb_phenomenon - 6;
        nb_composite  = nb_composite  - 3;
        
        Assert.assertEquals(nb_phenomenon, phenomenons.size());
        
        nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(nb_composite, nbComposite);
    }
}
