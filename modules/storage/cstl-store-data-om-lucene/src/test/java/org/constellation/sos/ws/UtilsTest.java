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

package org.constellation.sos.ws;

import java.time.Instant;
import org.geotoolkit.gml.xml.v311.TimePositionType;
import org.geotoolkit.observation.ObservationStoreException;
import org.junit.Test;
import static org.constellation.sos.io.lucene.LuceneObervationUtils.getLuceneTimeValue;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class UtilsTest {
    @Test
    public void getLuceneTimeValueTest() throws Exception {

        TimePositionType position = new TimePositionType("2007-05-01T07:59:00.0");
        String result             = getLuceneTimeValue(position.getDate());
        String expResult          = "20070501075900";

        assertEquals(expResult, result);

        position = new TimePositionType("2007051T07:59:00.0");

        boolean exLaunched = false;
        try {
            getLuceneTimeValue(position.getDate());
        } catch (ObservationStoreException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "eventTime");
        }

        assertFalse(exLaunched);

        String t = null;
        position = new TimePositionType(t);

        exLaunched = false;
        try {
            getLuceneTimeValue(position.getDate());
        } catch (ObservationStoreException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "eventTime");
        }

        assertTrue(exLaunched);

        exLaunched = false;
        try {
            getLuceneTimeValue((Instant) null);
        } catch (ObservationStoreException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "eventTime");
        }

        assertTrue(exLaunched);
    }
}
