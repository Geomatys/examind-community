
package com.examind.setup;

import org.apache.sis.referencing.CRS;
import org.junit.Test;

/**
 *
 * @author guilhem
 */
public class CRSExtTest {

    @Test
    public void crsTest() throws Exception {

        CRS.forCode("ESRI:102018");
    }
}
