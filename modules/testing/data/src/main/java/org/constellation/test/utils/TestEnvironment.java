package org.constellation.test.utils;

import org.geotoolkit.nio.IOUtilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;

import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;

import org.apache.sis.referencing.CRS;

/**
 * Utility class used for setup test data environment of test classes.
 *
 * @author Quentin Boileau (Geomatys)
 */
public final class TestEnvironment {

    /**
     * Current version of EPSG databse.
     * Used to replace "EPSG_VERSION" in some test resource file.
     * Must be updated if current EPSG database version change.
     */
    public static final String EPSG_VERSION = getEPSGVersion();

    /*
        List of resources available
     */
    public enum TestResources{

        //image
        DATA("org/constellation/data/image"),

        NETCDF("org/constellation/netcdf"),

        SQL_SCRIPTS("org/constellation/sql"),

        WFS110_AGGREGATE("org/constellation/ws/embedded/wfs110/aggregate"),
        WFS110_ENTITY("org/constellation/ws/embedded/wfs110/entity"),
        WFS110_PRIMITIVE("org/constellation/ws/embedded/wfs110/primitive"),
        WFS110_CITE_GMLSF0("org/constellation/ws/embedded/wfs110/cite-gmlsf0.xsd"),

        WMS111_SHAPEFILES("org/constellation/ws/embedded/wms111/shapefiles"),
        WMS111_STYLES("org/constellation/ws/embedded/wms111/styles"),

        //xml files
        XM("org/constellation/xml"),
        XML_METADATA("org/constellation/xml/metadata"),
        XML_SML("org/constellation/xml/sml"),
        XML_SOS("org/constellation/xml/sos"),

        //json files
        JSON("org/constellation/ws/embedded/json");

        private final String path;

        TestResources(String path) {
            this.path = path;
        }
    }

    private TestEnvironment() {
    }

    /**
     * Copy a TestResource into target directory
     * @param workspace
     * @param resource
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static Path initWorkspaceData(Path workspace, TestResources resource) throws URISyntaxException, IOException {

        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try {
            return IOUtilities.copyResource(resource.path, classloader, workspace, true);
        } catch (FileNotFoundException ex) {
            return workspace;
        }
    }


    /**
     * Copy all test resources into target directory
     * @param workspace
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public static Path initWorkspaceData(Path workspace) throws URISyntaxException, IOException {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        TestResources[] resources = TestResources.values();
        for (TestResources resource : resources) {
            IOUtilities.copyResource(resource.path, classloader, workspace, true);
        }

        return workspace;
    }

    private static String getEPSGVersion() {
        final CRSAuthorityFactory epsg;
        try {
            epsg = CRS.getAuthorityFactory("EPSG");
        } catch (FactoryException e) {
            throw new IllegalStateException("No EPSG factory defined", e);
        }
        final InternationalString edition = epsg.getAuthority().getEdition();
        if (edition == null) throw new IllegalStateException("No EPSG version defined !");
        return edition.toString(Locale.ROOT);
    }
}
