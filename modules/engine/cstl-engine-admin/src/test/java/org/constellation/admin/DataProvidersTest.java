package org.constellation.admin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.data.shapefile.ShapefileProvider;
import org.geotoolkit.test.VerifiableStorageConnector;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.junit.Assert.fail;

public class DataProvidersTest {

    private static Path rootDir;

    @BeforeClass
    public static void initTestDir() throws IOException {
        rootDir = initDataDirectory().outputDir;
    }

    /**
     * To quickly identify a datastore provider badly rewinding given storage connector, we can create a fresh connector
     * for each one, then ask for {@link DataStoreProvider#probeContent(StorageConnector) content probing} with it and
     * followed by the correct datastore to use. This way, if the first tested datastore did not properly rewind the
     * storage connector, an error will be risen.
     *
     * Note that this test is not exhaustive, as it cannot test all possible storage connector views.
     */
    @Test
    @Ignore("Until SIS StorageConnector does not provide fail-fast behavior, it's hard to debug.")
    public void properlyProbeShapefile() throws DataStoreException {
        final Path targetShapefile = rootDir.resolve(Paths.get("org", "constellation", "data", "shapefiles", "city.shp"));
        final ShapefileProvider shpp = new ShapefileProvider();
        final VerifiableStorageConnector initConnector = new VerifiableStorageConnector(
                "Init: ensure Shapefile is Ok.", targetShapefile
        );
        try {
            if (!shpp.probeContent(initConnector).isSupported()) throw new AssertionError("File is not a supported shapefile");
        } finally {
            initConnector.closeAllExcept(null); // trigger verification
        }
        for (DataStoreProvider dsp : DataStores.providers()) {
            final VerifiableStorageConnector connector = new VerifiableStorageConnector(dsp.getShortName(), targetShapefile);
            try {
                dsp.probeContent(connector);
                connector.verifyAll();
                ProbeResult probe = shpp.probeContent(connector);
                if (!probe.isSupported()) fail("Probing failed following " + dsp.getShortName());
            } finally {
                connector.closeAllExcept(null);
            }
        }
    }
}
