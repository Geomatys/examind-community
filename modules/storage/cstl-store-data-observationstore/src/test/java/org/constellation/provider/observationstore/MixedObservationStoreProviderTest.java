/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
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
package org.constellation.provider.observationstore;

import java.io.File;
import jakarta.annotation.PostConstruct;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.junit.AfterClass;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MixedObservationStoreProviderTest extends AbstractMixedObservationStoreProviderTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();
            Integer omPid  = testResource.createProviderWithDatasource(TestEnvironment.TestResource.OM2_DB_MIXED, providerBusiness, datasourceBusiness, null).id;

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
        File mappingFile = new File("mapping.properties");
        if (mappingFile.exists()) {
            mappingFile.delete();
        }
    }

    @Test
    @Override
    public void getProcedureTreesTest() throws Exception {
        super.getProcedureTreesTest();
    }

    @Test
    @Override
    public void existFeatureOfInterestTest() throws Exception {
       super.existFeatureOfInterestTest();
    }

    @Test
    @Override
    public void getFeatureOfInterestNamesTest() throws Exception {
        super.getFeatureOfInterestNamesTest();
    }

    @Test
    @Override
    public void getFeatureOfInterestTest() throws Exception {
        super.getFeatureOfInterestTest();
    }
    
    /**
     *  This test cannot work because of the disparity between geometry CRS.
     * TODO:
     * - harmonize geometry crs in test data
     * - add a global crs configuration
     * - reproject the envelope before applying filter if in a different crs
     */
    @Ignore
    @Override
    public void getFeatureOfInterestIdsBBOXTest() throws Exception {
        super.getFeatureOfInterestIdsBBOXTest();
    }

    /**
     *  This test cannot work because of the disparity between geometry CRS.
     * TODO:
     * - harmonize geometry crs in test data
     * - add a global crs configuration
     * - reproject the envelope before applying filter if in a different crs
     */
    @Ignore
    @Override
    public void getFullFeatureOfInterestBBOXTest() throws Exception {
        super.getFullFeatureOfInterestBBOXTest();
    }

    @Test
    @Override
    public void existPhenomenonTest() throws Exception {
        super.existPhenomenonTest();
    }

    @Test
    @Override
    public void getPhenomenonNamesTest() throws Exception {
        super.getPhenomenonNamesTest();
    }

    @Test
    @Override
    public void getPhenomenonNames2Test() throws Exception {
        super.getPhenomenonNames2Test();
    }
    
    @Test
    @Override
    public void getPhenomenonsTest() throws Exception {
        super.getPhenomenonsTest();
    }
    
    @Test
    @Override
    public void getPhenomenons2Test() throws Exception {
        super.getPhenomenons2Test();
    }

    @Test
    @Override
    public void getPhenomenonsForProcedureTest() throws Exception {
        super.getPhenomenonsForProcedureTest();
    }

    @Test
    @Override
    public void getPhenomenonPagingTest() throws Exception {
       super.getPhenomenonPagingTest();
    }

    @Test
    @Override
    public void getHistoricalLocationNameTest() throws Exception {
        super.getHistoricalLocationNameTest();
    }

    @Test
    @Override
    public void getHistoricalLocationTest() throws Exception {
        super.getHistoricalLocationTest();
    }

    @Test
    @Override
    public void getSensorTimesTest() throws Exception {
        super.getSensorTimesTest();
    }

    @Test
    @Override
    public void getLocationNameTest() throws Exception {
       super.getLocationNameTest();
    }

    @Test
    @Override
    public void getLocationTest() throws Exception {
        super.getLocationTest();
    }
    
    @Test
    @Override
    public void existProcedureTest() throws Exception {
        super.existProcedureTest();
    }

    @Test
    @Override
    public void getProcedureNamesTest() throws Exception {
        super.getProcedureNamesTest();
    }
    
    @Test
    @Override
    public void getProcedureNames2Test() throws Exception {
        super.getProcedureNamesTest();
    }

    @Test
    @Override
    public void getProcedureTest() throws Exception {
        super.getProcedureTest();
    }
    
    @Test
    @Override
    public void getProcedure2Test() throws Exception {
        super.getProcedureTest();
    }
    
    @Test
    @Override
    public void getProcedureComplexFilterTest() throws Exception {
         super.getProcedureComplexFilterTest();
    }

    @Test
    @Override
    public void existOfferingTest() throws Exception {
        super.existOfferingTest();
    }

    @Test
    @Override
    public void getOfferingNamesTest() throws Exception {
        super.getOfferingNamesTest();
    }

    @Test
    @Override
    public void getOfferingsTest() throws Exception {
        super.getOfferingsTest();
    }

    @Test
    @Override
    public void getObservationTemplateNamesTest() throws Exception {
        super.getObservationTemplateNamesTest();
    }

    @Test
    @Override
    public void getTimeForTemplateTest() throws Exception {
        super.getTimeForTemplateTest();
    }

    @Test
    @Override
    public void getSensorTemplateTest() throws Exception {
        super.getSensorTemplateTest();
    }

    @Test
    @Override
    public void getObservationTemplateTest() throws Exception {
        super.getObservationTemplateTest();
    }
    
    @Test
    @Override
    public void getObservationTemplate2Test() throws Exception {
        super.getObservationTemplate2Test();
    }

    @Test
    @Override
    public void getMeasurementTemplateTest() throws Exception {
        super.getMeasurementTemplateTest();
    }

    @Test
    @Override
    public void getMeasurementTemplateResultFilterTest() throws Exception {
        super.getMeasurementTemplateResultFilterTest();
    }

    /**
     * No text field in this implementation
     */
    @Ignore
    @Override
    public void getMeasurementTemplateResultFilterTextFieldTest() throws Exception {
        super.getMeasurementTemplateResultFilterTextFieldTest();
    }
    
    @Test
    @Override
    public void getMeasurementTemplateResult2FilterTest() throws Exception {
        super.getMeasurementTemplateResult2FilterTest();
    }

    /**
     * No quality flag in this implementation 
     */
    @Ignore
    @Override
    public void getMeasurementTemplateResult3FilterTest() throws Exception {
        super.getMeasurementTemplateResult3FilterTest();
    }
    
    @Test
    @Override
    public void getMeasurementTemplateFilterTest() throws Exception {
        super.getMeasurementTemplateFilterTest();
    }
    
    @Test
    @Override
    public void getMeasurementTemplateFilter2Test() throws Exception {
        super.getMeasurementTemplateFilter2Test();
    }
    
    @Test
    @Override
    public void getMeasurementTemplateFilter3Test() throws Exception {
        super.getMeasurementTemplateFilter3Test();
    }
    
    @Test
    @Override
    public void getObservationNamesTest() throws Exception {
        super.getObservationNamesTest();
    }

    @Test
    @Override
    public void getObservationNames2Test() throws Exception {
        super.getObservationNames2Test();
    }

    @Test
    @Override
    public void getObservationNames3Test() throws Exception {
        super.getObservationNames3Test();
    }

    /**
     * TODO the followings test don't work has the result fiters only remove the not matching values from lines
     * 
     */
    @Ignore
    @Override
    public void getObservationNames4Test() throws Exception {
        super.getObservationNames4Test();
    }
    
    @Test
    @Override
    public void getMeasurementsSimpleTest() throws Exception {
        super.getMeasurementsSimpleTest();
    }

    @Test
    @Override
    public void getMeasurementsTest() throws Exception {
        super.getMeasurementsTest();
    }

    @Test
    @Override
    public void getMeasurements2Test() throws Exception {
        super.getMeasurements2Test();
    }

    @Test
    @Override
    public void getMeasurements3Test() throws Exception {
        super.getMeasurements3Test();
    }

    @Test
    @Override
    public void getMeasurements4Test() throws Exception {
        super.getMeasurements4Test();
    }
    
    @Test
    @Override
    public void getObservationsTimeDisorderTest() throws Exception {
        super.getObservationsTimeDisorderTest();
    }
    
    @Test
    @Override
    public void getObservationsSimpleTest() throws Exception {
        super. getObservationsSimpleTest();
    }

    @Test
    @Override
    public void getObservationsTest() throws Exception {
        super.getObservationsTest();
    }

    @Test
    @Override
    public void getObservationsFilterTest() throws Exception {
        super.getObservationsFilterTest();
    }
    
    /**
     * TODO not working because measure of the sensor 17 are not handled yet
     * @throws Exception 
     */
    @Ignore
    @Override
    public void getObservationsFilter2Test() throws Exception {
        super.getObservationsFilter2Test();
    }
    
    /**
     * TODO not working because there is no quality field handling for now
     * @throws Exception 
     */
    @Ignore
    @Override
    public void getObservationsFilter3Test() throws Exception {
        super.getObservationsFilter3Test();
    }
    
    @Test
    @Override
    public void getObservationsNanTest() throws Exception {
        super.getObservationsNanTest();
    }

    @Test
    @Override
    public void getObservationsNanProfileTest() throws Exception {
        super.getObservationsNanProfileTest();
    }

    @Test
    @Override
    public void getResultsProfileSingleMainFieldTest() throws Exception {
        super.getResultsProfileSingleMainFieldTest();
    }

    @Test
    @Override
    public void getResultsProfileSingleFieldTest() throws Exception {
        super.getResultsProfileSingleFieldTest();
    }

    @Test
    @Override
    public void getResultsTest() throws Exception {
        super.getResultsTest();
    }
    
    @Test
    @Override
    public void getResults2Test() throws Exception {
        super.getResults2Test();
    }
    
    @Test
    @Override
    public void getResultsSingleFilterFlatTest() throws Exception {
        super.getResultsSingleFilterFlatTest();
    }
    
    @Test
    @Override
    public void getResultsSingleFilterFlatProfileTest() throws Exception {
        super.getResultsSingleFilterFlatProfileTest();
    }
    
    /**
     * the sensor "urn:ogc:object:sensor:GEOM:17" does not have any measure int this implementation because:
     * - quality flag are not supported
     * - non-double field are not supported
     * 
     */
    @Ignore
    @Override
    public void getResultsSingleFilterFlatSubFIeldTest() throws Exception {
        super.getResultsSingleFilterFlatSubFIeldTest();
    }

    @Test
    @Override
    public void getResultsSingleFilterTest() throws Exception {
        super.getResultsSingleFilterTest();
    }

    @Test
    @Override
    public void getResultsSingleFilter2Test() throws Exception {
        super.getResultsSingleFilter2Test();
    }

    @Test
    @Override
    public void getResultsMultiFilterTest() throws Exception {
        super.getResultsMultiFilterTest();
    }
    
    /**
     * This implementation does not handle quality field for now
     */
    @Ignore
    @Override
    public void getResultsMultiFilterQualityTest() throws Exception {
        super.getResultsMultiFilterQualityTest();
    }
    
    /**
     * This implementation does not handle parameter field for now
     */
    @Ignore
    @Override
    public void getResultsMultiFilterParameterTest() throws Exception {
        super.getResultsMultiFilterParameterTest();
    }
    
    /**
     * the sensor "urn:ogc:object:sensor:GEOM:17" does not have any measure int this implementation because:
     * - quality flag are not supported
     * - non-double field are not supported
     * 
     */
    @Ignore
    @Override
    public void getResultsMultiTableTest() throws Exception {
        super.getResultsMultiTableTest();
    }
    
    @Test
    @Override
    public void getResultsSimpleTest() throws Exception {
        super.getResultsSimpleTest();
    }
    
    @Test
    @Override
    public void getResultsNanTest() throws Exception {
        super.getResultsNanTest();
    }

    @Test
    @Override
    public void getResultsSingleNanTest() throws Exception {
        super.getResultsSingleNanTest();
    }
    
    @Test
    @Override
    public void getResultsSingleNanMultiTableTest() throws Exception {
        super.getResultsSingleNanMultiTableTest();
    }
    
    @Test
    @Override
    public void getResultsSingleNanMultiTable2Test() throws Exception {
        super.getResultsSingleNanMultiTable2Test();
    }
    
    @Test
    @Override
    public void getResultsSingleNanMultiTable3Test() throws Exception {
        super.getResultsSingleNanMultiTable3Test();
    }

    @Test
    @Override
    public void getResultsProfileTest() throws Exception {
        super.getResultsProfileTest();
    }
    
    @Test
    @Override
    public void getResultsProfileFilterTest() throws Exception {
        super.getResultsProfileFilterTest();
    }
    
    @Test
    @Override
    public void getResultsProfileFilter2Test() throws Exception {
        super.getResultsProfileFilter2Test();
    }

    @Test
    @Override
    public void getResultTest() throws Exception {
        super.getResultTest();
    }
    
    @Test
    @Override
    public void getResultsobsPropPropertiesTest() throws Exception {
        super.getResultsobsPropPropertiesTest();
    }
    
    /**
     * TODO the sensor 17 has no data in this implementation.
     */
    @Test
    @Ignore
    public void getResultsobsPropProperties2Test() throws Exception {
        super.getResultsobsPropProperties2Test();
    }
    
    @Test
    @Override
    public void getProfileFilterTest() throws Exception {
        super.getProfileFilterTest();
    }
}
