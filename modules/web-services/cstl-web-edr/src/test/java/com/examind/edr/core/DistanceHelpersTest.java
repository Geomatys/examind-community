/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.edr.core;

import java.awt.Dimension;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.ws.CstlServiceException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit checks of the {@link DefaultEDRWorker} distance / resolution helpers (no server needed).
 *
 * @author Quentin Bialota (Geomatys)
 */
public class DistanceHelpersTest {

    @Test
    public void toMetresTest() throws Exception {
        assertEquals(10, DefaultEDRWorker.toMetres(10d, null, "u"), 0);
        assertEquals(10_000, DefaultEDRWorker.toMetres(10d, "km", "u"), 1e-9);
        assertEquals(1609.344, DefaultEDRWorker.toMetres(1d, "mi", "u"), 1e-6);
        assertEquals(0, DefaultEDRWorker.toMetres(null, "km", "u"), 0);
        for (String bad : new String[] {"kg", "s", "not-a-unit"}) {
            try {
                DefaultEDRWorker.toMetres(1d, bad, "u");
                fail(bad + " should be rejected");
            } catch (CstlServiceException expected) {
                // ok
            }
        }
    }

    @Test
    public void geographicBufferTest() throws Exception {
        final double lat = 10_000 / DefaultEDRWorker.METRES_PER_DEGREE;
        final double[] equator = DefaultEDRWorker.bufferInCrsUnits(CommonCRS.WGS84.normalizedGeographic(), 0, 10_000);
        assertEquals(lat, equator[1], 1e-12);
        assertEquals(lat, equator[0], 1e-6);
        // at 60°N a degree of longitude is half a degree of latitude: the longitude buffer doubles
        final double[] north = DefaultEDRWorker.bufferInCrsUnits(CommonCRS.WGS84.normalizedGeographic(), 60 - lat, 10_000);
        assertEquals(2 * lat, north[0], 1e-9);
        assertEquals(lat, north[1], 1e-12);
    }

    @Test
    public void projectedBufferTest() throws Exception {
        final double[] utm = DefaultEDRWorker.bufferInCrsUnits(CommonCRS.WGS84.universal(48, 2), 0, 10_000);
        assertArrayEquals(new double[] {10_000, 10_000}, utm, 1e-9);
    }

    @Test
    public void outputSizeTest() throws Exception {
        assertNull(DefaultEDRWorker.outputSize(null, null));
        assertEquals(new Dimension(4, 3), DefaultEDRWorker.outputSize(4d, 3d));
        for (Double[] bad : new Double[][] {{4d, null}, {0d, 3d}, {2.5, 3d}}) {
            try {
                DefaultEDRWorker.outputSize(bad[0], bad[1]);
                fail("should be rejected");
            } catch (CstlServiceException expected) {
                // ok
            }
        }
    }
}
