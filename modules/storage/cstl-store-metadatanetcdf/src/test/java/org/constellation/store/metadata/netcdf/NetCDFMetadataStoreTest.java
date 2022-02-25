/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.store.metadata.netcdf;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreProvider;
import static org.junit.Assert.assertNotNull;

import org.constellation.util.NodeUtilities;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.lang.Setup;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class NetCDFMetadataStoreTest {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.metadata.netcdf");

    private static NetCDFMetadataStore fsStore1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final FileSystem fs = Jimfs.newFileSystem("netcdf-md-test", Configuration.unix());
        final Path dataDir = fs.getPath("/").resolve("test");
        Files.createDirectories(dataDir);
        try (InputStream dataStream = NetCDFMetadataStore.class.getResourceAsStream("/org/constellation/netcdf/2005092200_sst_21-24.en.nc")) {
            Files.copy(dataStream, dataDir.resolve("2005092200_sst_21-24.en.nc"));
        }

        Setup.initialize(null);

        final DataStoreProvider factory = DataStores.getProviderById("NetCDFMetadata");
        LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());

        final ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter("folder").setValue(dataDir);

        fsStore1 = (NetCDFMetadataStore) factory.open(params);
    }

    @Test
    public void getMetadataTest() throws Exception {
        RecordInfo result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.NATIVE);
        assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        assertNotNull(result.node);
        Object obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);

        result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.DUBLINCORE_CSW202);
        assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof RecordType);

        result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.DUBLINCORE_CSW300);
        assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW300, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof org.geotoolkit.csw.xml.v300.RecordType);
    }

    @Test
    public void getFieldDomainofValuesTest() throws Exception {
        List<DomainValues> result = fsStore1.getFieldDomainofValues("title");
        Assert.assertEquals(1, result.size());
        assertNotNull(result.get(0));
        assertNotNull(result.get(0).getListOfValues());
        List<String> results = (List<String>) result.get(0).getListOfValues().getValue();

        Assert.assertTrue(results.contains("Sea Surface Temperature Analysis Model"));
    }

    @Test
    public void getFieldDomainofValuesForMetadataTest() throws Exception {
        List<String> results = fsStore1.getFieldDomainofValuesForMetadata("title", "2005092200_sst_21-24.en");
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("Sea Surface Temperature Analysis Model"));

    }

    @AfterClass
    public static void tearDownClass() {
        try {
            fsStore1.close();
        }  catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }
}
