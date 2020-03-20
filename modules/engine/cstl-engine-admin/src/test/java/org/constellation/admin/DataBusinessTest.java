/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.admin;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Role;

import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;

import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.DataBrief;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.ParameterValues;
import org.constellation.exception.ConstellationException;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.constellation.test.utils.TestEnvironment.initDataDirectory;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class DataBusinessTest {

    public static final String GEOMATYS = "Geomatys";
    @Autowired
    private IDatasetBusiness datasetBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Inject
    protected IProviderBusiness providerBusiness;

    private static boolean initialized = false;

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");
    private static final String confDirName = "DataBusinessTest" + UUID.randomUUID().toString();

    private static int coveragePID;
    private static int vectorPID;
    public static final QName COVERAGE_NAME = new QName("SSTMDE200305");
    public static final QName FEATURE_NAME = new QName("BuildingCenters");

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement(confDirName);
    }

    @AfterClass
    public static void tearDown() {
        try {
            final IDataBusiness dbus = SpringHelper.getBean(IDataBusiness.class);
            if (dbus != null) {
                dbus.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
            if (provider != null) {
                provider.removeAll();
            }
            final IDatasetBusiness dsBus = SpringHelper.getBean(IDatasetBusiness.class);
            if (dsBus != null) {
                dsBus.removeAllDatasets();
            }
            ConfigDirectory.shutdownTestEnvironement(confDirName);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @PostConstruct
    public void init() {
        if (!initialized) {
            try {
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
                datasetBusiness.removeAllDatasets();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);
                final TestResources testResource = initDataDirectory();

                // dataset
                int dsId = datasetBusiness.createDataset("DataBusinessTest", null, null);

                // coverage-file datastore
                coveragePID = testResource.createProvider(TestResource.PNG, providerBusiness);
                providerBusiness.createOrUpdateData(coveragePID, dsId, false);

                vectorPID = testResource.createProvider(TestResource.WMS111_SHAPEFILES, providerBusiness);
                providerBusiness.createOrUpdateData(vectorPID, dsId, false);

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Ensure that a dynamic proxy is overriding data metadata with the one registered in Examind.
     */
    @Test
    public void coverageWrappedForMetadata() throws Exception {
        DataBrief testData = dataBusiness.getDataBrief(COVERAGE_NAME, coveragePID);
        testMetadataWrapping(testData);
    }

    @Test
    public void featureWrappedForMetadata() throws Exception {
        DataBrief testData = dataBusiness.getDataBrief(FEATURE_NAME, vectorPID);
        testMetadataWrapping(testData);
    }

    private void testMetadataWrapping(final DataBrief testData) throws Exception {
        // Create a metadata
        dataBusiness.initDataMetadata(testData.getId(), false);

        // Create a copy / modify it / update data related metadata
        final MetadataCopier mdCopier = new MetadataCopier(null);
        final Metadata mdCopy = mdCopier.copy(Metadata.class, (Metadata) metadataBusiness.getIsoMetadataForData(testData.getId()));
        final DefaultResponsibility geomatys = new DefaultResponsibility(Role.AUTHOR, null, new DefaultOrganisation(GEOMATYS, null, null, null));
        ((Collection)mdCopy.getContacts()).add(geomatys);
        metadataBusiness.updateMetadata(mdCopy.getFileIdentifier(), mdCopy, testData.getId(), null, null, null, null, null);

        // Ensure SIS resource is overriden to give back Examind metadata.
        final DataProvider dataProvider = DataProviders.getProvider(testData.getProviderId());
        final Data data = dataProvider.get(testData.getNamespace(), testData.getName());
        final Resource r = data.getOrigin();
        final Metadata resourceMetadata = r.getMetadata();

        Assert.assertTrue(
                "Metadata not properly overriden",
                resourceMetadata.getContacts().stream()
                        .filter(contact -> Role.AUTHOR.equals(contact.getRole()))
                        .flatMap(contact -> contact.getParties().stream())
                        .map(Party::getName)
                        .map(Objects::toString)
                        .anyMatch(GEOMATYS::equals)
        );
    }

    @Test
    public void dataCoverageTest() throws Exception {
        DataBrief db = dataBusiness.getDataBrief(COVERAGE_NAME, coveragePID);
        Assert.assertNotNull(db);

        LOGGER.info("wait for SSTMDE200305 stats to complete.....");
        while (db.getStatsState() == null || db.getStatsState().equals("PENDING"))  {
            db = dataBusiness.getDataBrief(COVERAGE_NAME, coveragePID);
            Thread.sleep(1000);
        }
        Assert.assertEquals("COMPLETED", db.getStatsState());

        Assert.assertNotNull(db.getDataDescription());
        Assert.assertTrue(db.getDataDescription() instanceof CoverageDataDescription);
        CoverageDataDescription desc = (CoverageDataDescription) db.getDataDescription();
        Assert.assertEquals(1, desc.getBands().size());
    }

    @Test
    public void dataVectorTest() throws Exception {
        DataBrief db = dataBusiness.getDataBrief(FEATURE_NAME, vectorPID);
        Assert.assertNotNull(db);
        Assert.assertNotNull(db.getDataDescription());
        Assert.assertTrue(db.getDataDescription() instanceof FeatureDataDescription);
        FeatureDataDescription desc = (FeatureDataDescription) db.getDataDescription();
        Assert.assertEquals(3, desc.getProperties().size());

        ParameterValues results = dataBusiness.getVectorDataColumns(db.getId());
        ParameterValues expected = new ParameterValues();
        expected.getValues().put("FID","FID");
        expected.getValues().put("sis:identifier","sis:identifier");
        expected.getValues().put("sis:envelope","sis:envelope");
        expected.getValues().put("ADDRESS","ADDRESS");
        expected.getValues().put("sis:geometry","sis:geometry");
        expected.getValues().put("the_geom","the_geom");
        Assert.assertEquals(expected, results);

    }
}
