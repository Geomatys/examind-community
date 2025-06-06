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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Requirements class 20: Requirements Class PNG Data
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_png
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-png
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance20_PngZoneDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.12.2.  PNG Zone Data
    // A.20.1.  Abstract Test for Requirement PNG Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-png_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-png_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-png_null-values
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-png_crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-png_scale-offset
     */
    @Test
    public void requirement34() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data as png image
            final byte[] dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=3"),
                        "image/png", byte[].class);
            //check the start of the signature
            assertEquals(137, dto[0] & 0xFF);
            assertEquals(80, dto[1] & 0xFF);
            assertEquals(78, dto[2] & 0xFF);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(dto));

            final int[] pixelCorner = image.getData().getPixel(0, 0, new int[4]);
            final int[] pixelCenter = image.getData().getPixel(64, 64, new int[4]);
            assertArrayEquals(new int[]{0,0,0,0}, pixelCorner);
            assertArrayEquals(new int[]{180,180,180,85}, pixelCenter);
        }

        { //check scale and offset
            final byte[] dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=3&values-scale=0.5&values-offset=10"),
                        "image/png", byte[].class);
            //check the start of the signature
            assertEquals(137, dto[0] & 0xFF);
            assertEquals(80, dto[1] & 0xFF);
            assertEquals(78, dto[2] & 0xFF);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(dto));

            final int[] pixelCorner = image.getData().getPixel(0, 0, new int[4]);
            final int[] pixelCenter = image.getData().getPixel(64, 64, new int[4]);
            assertArrayEquals(new int[]{0,0,0,0}, pixelCorner);
            assertArrayEquals(new int[]{180/2+10,180/2+10,180/2+10,85/2+10}, pixelCenter);
        }
    }

}
