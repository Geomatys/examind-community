
package org.constellation.provider.observationstore;

import jakarta.annotation.PostConstruct;
import java.io.File;
import org.constellation.business.IProviderBusiness;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.junit.AfterClass;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author glegal
 */
public abstract class AbstractObservationStoreProviderRemoveTest extends SpringContextTest{
    
    protected final int NB_OBSERVATION     = 25; // contains the phenomenon directly used in the observations
    protected final int NB_USED_PHENOMENON = 6;
    protected final int NB_PHENOMENON      = 12;
    protected final int NB_COMPOSITE       = 4;
    protected final int NB_FOI             = 3;  // only 3 because 3 of the recorded procedure have no observations
    protected final int NB_PROCEDURE       = 19; // include empty procedure
    protected final int NB_USED_PROCEDURE  = 17; // only 16 because 2 of the recorded procedure have no observation
    
    @Autowired
    protected IProviderBusiness providerBusiness;

    protected static ObservationProvider omPr;

    private boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();
            Integer omPid  = testResource.createProvider(TestEnvironment.TestResource.OM2_DB, providerBusiness, null).id;

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
    }
}
