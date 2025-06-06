/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dggs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Requirements class 14: Requirements Class GeoTIFF Data
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_geotiff
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-geotiff
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance14_GeotiffZoneDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.6.2.  GeoTIFF Zone Data
    // A.14.1.  Abstract Test for Requirement GeoTIFF Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-geotiff_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-geotiff_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geotiff_null-values
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geotiff_crs
     */
    @Test
    public void requirement28() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data as geotiff image
            final byte[] dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=3"),
                        "image/tiff;application=geotiff", byte[].class);
            //check the start of the signature
            assertEquals(0x49, dto[0] & 0xFF);
            assertEquals(0x49, dto[1] & 0xFF);
            assertEquals(0x2A, dto[2] & 0xFF);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(dto));

            final double[] pixelCorner = image.getData().getPixel(0, 0, new double[2]);
            final double[] pixelCenter = image.getData().getPixel(64, 64, new double[2]);
            assertArrayEquals(new double[]{Double.NaN,Double.NaN}, pixelCorner, 0.0);
            assertArrayEquals(new double[]{180,85}, pixelCenter, 0.0);
        }

        { //check scale and offset
            final byte[] dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=3&values-scale=0.5&values-offset=10"),
                        "image/tiff;application=geotiff", byte[].class);
            //check the start of the signature
            assertEquals(0x49, dto[0] & 0xFF);
            assertEquals(0x49, dto[1] & 0xFF);
            assertEquals(0x2A, dto[2] & 0xFF);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(dto));

            final double[] pixelCorner = image.getData().getPixel(0, 0, new double[2]);
            final double[] pixelCenter = image.getData().getPixel(64, 64, new double[2]);
            assertArrayEquals(new double[]{Double.NaN,Double.NaN}, pixelCorner, 0.0);
            assertArrayEquals(new double[]{180.0/2+10,85.0/2+10}, pixelCenter, 0.0);
        }
    }

}
