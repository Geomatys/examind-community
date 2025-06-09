/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
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

import jakarta.annotation.PostConstruct;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.AbstractObservationStoreProviderWriteTest.omPr;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.junit.Test;

/**
 *
 * @author guilhem
 */
public class ObservationStoreProviderWriteDATest  extends AbstractObservationStoreProviderWriteTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();
            Integer omPid  = testResource.createProviderWithDatasource(TestEnvironment.TestResource.OM2_DB_NO_DATA, providerBusiness, datasourceBusiness, null).id;

            omPr = (ObservationProvider) DataProviders.getProvider(omPid);
            initialized = true;
          }
    }
    
    public ObservationStoreProviderWriteDATest() {
        super("_DA");
    }

    @Test
    @Override
    public void writeObservationQualityTest() throws Exception {
        super.writeObservationQualityTest();
    }

    @Test
    @Override
    public void writeObservationMultiTableTest() throws Exception {
        super.writeObservationMultiTableTest();

    }

    @Test
    @Override
    public void writeObservationMultiTypeTest() throws Exception {
        super.writeObservationMultiTypeTest();
    }

    @Test
    @Override
    public void writeDisjointObservationTest() throws Exception {
        super.writeDisjointObservationTest();
    }

    @Test
    @Override
    public void writeExtendObservationTest() throws Exception {
        super.writeExtendObservationTest();
    }

    @Test
    @Override
    public void writeTableExtendObservationTest() throws Exception {
        super.writeTableExtendObservationTest();
    }

    @Test
    @Override
    public void writeChangeUomObservationTest() throws Exception {
        super.writeChangeUomObservationTest();
    }

    @Test
    @Override
    public void writeOverlappingObservationTest() throws Exception {
        super.writeOverlappingObservationTest();
    }

    @Test
    @Override
    public void writeOverlappingSingleInstantObservationTest() throws Exception {
        super.writeOverlappingSingleInstantObservationTest();
    }

    @Test
    @Override
    public void writeOverlappingInstantObservationTest() throws Exception {
        super.writeOverlappingInstantObservationTest();
    }

    @Test
    @Override
    public void writeOverlappingInstantPhenChangeObservationTest() throws Exception {
        super.writeOverlappingInstantPhenChangeObservationTest();
    }

    @Test
    @Override
    public void writeIntersectingInstantPhenChangeObservationTest() throws Exception {
        super.writeIntersectingInstantPhenChangeObservationTest();
    }

    @Test
    @Override
    public void writeExtend2ObservationTest() throws Exception {
        super.writeExtend2ObservationTest();
    }
    
    @Test
    @Override
    public void writeObservationParameterTest() throws Exception {
        super.writeObservationParameterTest();
    }
}
