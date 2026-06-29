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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.sis.image.PixelIterator;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.dggs.MediaTypes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Requirements class 24: Requirements Class GeoTIFF Zone List
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-zone_geotiff
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-geotiff
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance24_GeotiffZoneListEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 17.5.2.  GeoTIFF Zone List
    // A.24.1.  Abstract Test for Requirement GeoTIFF zone list encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-geotiff_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-geotiff_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geotiff_crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geotiff_values
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geotiff_non-rectilinear
     */
    @Test
    public void requirement38() throws Exception {
        initLayerList();

        final String dggrsId = "Healpix";

        { //check getting the zones as geotiff for dggrs
            final byte[] dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones"),
                        MediaTypes.ZONELIST_GEOTIFF, byte[].class);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(dto));
            PixelIterator iterator = PixelIterator.create(image);

            final Set<Long> toBeFound = new HashSet();
            toBeFound.addAll(List.of(0l,16l,17l,18l,19l,20l,21l,22l,23l,24l,25l,26l,27l));

            final Set<Long> result = new HashSet();
            double[] pixel = null;
            while (iterator.next()) {
                pixel = iterator.getPixel(pixel);
                result.add(Double.doubleToLongBits(pixel[0]));
            }

            Assert.assertTrue(toBeFound.size() == result.size());
            Assert.assertTrue(toBeFound.containsAll(result));

        }
    }

}
