package org.constellation.admin.converter;

import org.apache.sis.storage.Resource;
import org.apache.sis.util.ObjectConverters;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import static org.junit.Assert.assertEquals;


@RunWith(SpringTestRunner.class)
public class DataProcessReferenceToResourceTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.converter");

    /**
     * DatasetBusiness used for provider GUI editors data
     */
    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    private static int nbVectorData = -1;

    private static boolean initialized = false;

    @PostConstruct
    public void init() throws Exception {
        if (!initialized) {
            dataBusiness.deleteAll();
            providerBusiness.removeAll();

            //Initialize geotoolkit
            ImageIO.scanForPlugins();
            org.geotoolkit.lang.Setup.initialize(null);

            // coverage-file datastore
            testResources.createProvider(TestEnvironment.TestResource.TIF, providerBusiness, null);
            testResources.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null);

            // shapefile datastore
            nbVectorData = testResources.createProviders(TestEnvironment.TestResource.SHAPEFILES, providerBusiness, null).datas().size();

            initialized = true;
        }
    }

    /**
     * This test uses all datas already existing in examind.
     * The Datas are added to a list as DataProcessReferences.
     * The converter DataProcessReferenceToResourceConverter is called to convert all those datas from DataProcessReference to Resources.
     */
    @Test
    public void convertTest() {
        List<DataProcessReference> dataPRef = dataBusiness.findDataProcessReference("VECTOR");
        assertEquals(nbVectorData, dataPRef.size());
        for (DataProcessReference dpr : dataPRef) {
            final Resource converted = ObjectConverters.convert(dpr, Resource.class);
            Assert.isInstanceOf(FeatureSet.class, converted);
        }

        dataPRef = dataBusiness.findDataProcessReference("COVERAGE");
        assertEquals(2, dataPRef.size());
        for (DataProcessReference dpr : dataPRef) {
            final Resource converted = ObjectConverters.convert(dpr, Resource.class);
            Assert.isInstanceOf(GridCoverageResource.class, converted);
        }
    }

}
