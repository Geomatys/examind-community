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
import org.constellation.dto.service.config.sos.ObservationDataset;
import org.constellation.dto.service.config.sos.ProcedureDataset;
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
import org.opengis.observation.Observation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderRemoveCompositePhen2Test extends AbstractObservationStoreProviderRemoveTest {

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
        List<org.opengis.observation.Phenomenon> phenomenons = omPr.getPhenomenon(new ObservedPropertyQuery());
        
        Assert.assertEquals(nb_phenomenon, phenomenons.size());
        
        long nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(nb_composite, nbComposite);

        // get the full content of the store
        ObservationDataset fullDataset = omPr.extractResults(new DatasetQuery());

        Assert.assertEquals(nb_observation,     fullDataset.getObservations().size());
        Assert.assertEquals(nb_used_phenomenon, fullDataset.getPhenomenons().size());
        Assert.assertEquals(nb_foi,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(nb_used_procedure,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // include empty procedure
        List<org.opengis.observation.Process> procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());

        /*
        * delete the phenomenon "multi-type-phenomenon"
        */
        omPr.removePhenomenon("multi-type-phenomenon");


        // get the full content of the store to verify the deletion
        fullDataset = omPr.extractResults(new DatasetQuery());

        nb_observation--;         // 21 merged observations has been removed
        nb_used_phenomenon = nb_used_phenomenon - 2;  // ??
        nb_used_procedure--; // one procedure has been removed
        nb_procedure--;
        // no foi removed
        
        Assert.assertEquals(nb_observation,     fullDataset.getObservations().size());
        Assert.assertEquals(nb_used_phenomenon, fullDataset.getPhenomenons().size());
        Assert.assertEquals(nb_foi,             fullDataset.getFeatureOfInterest().size());
        Assert.assertEquals(nb_used_procedure,  fullDataset.getProcedures().size());
        assertPeriodEquals("1980-03-01T21:52:00Z", "2012-12-22T00:00:00Z", fullDataset.getDateStart(), fullDataset.getDateEnd());

        // verify that the procedures has been totaly removed
        procedures = omPr.getProcedures(new ProcedureQuery());
        Assert.assertEquals(nb_procedure, procedures.size());
        
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
        nb_phenomenon = nb_phenomenon - 6;
        nb_composite = nb_composite - 2;
        
        Assert.assertEquals(nb_phenomenon, phenomenons.size());
        
        nbComposite = phenomenons.stream().filter(ph -> ph instanceof CompositePhenomenon).count();

        Assert.assertEquals(nb_composite, nbComposite);
    }
}
