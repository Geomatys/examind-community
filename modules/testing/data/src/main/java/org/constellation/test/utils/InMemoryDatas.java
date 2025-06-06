/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.test.utils;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRenderedImage;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.DataType;
import org.apache.sis.image.WritablePixelIterator;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.util.iso.Names;

/**
 * Store some in memory datas for tests.
 *
 * Those datas are difficult to store in files, therefor we create them in memory for testing.
 *
 * @author Johann Sorel (Geomatys)
 */
public class InMemoryDatas {

    public static final MemoryGridCoverageResource COVERAGE_2D;
    public static final MemoryGridCoverageResource COVERAGE_4D;

    static {
        try {
            final GeneralEnvelope dataEnv = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            dataEnv.setRange(0, -180, 180);
            dataEnv.setRange(1, -90, 90);
            final GridGeometry dataGrid = new GridGeometry(
                    new GridExtent(null, new long[2], new long[] {360,180}, false),
                    dataEnv,
                    GridOrientation.HOMOTHETY
            );
            final BufferedGridCoverage coverage = new BufferedGridCoverage(
                    dataGrid,
                    (List)List.of(
                            new SampleDimension.Builder().setName("band1").build(),
                            new SampleDimension.Builder().setName("band2").build()),
                    DataBuffer.TYPE_INT
            );
            WritableRenderedImage image = (WritableRenderedImage) coverage.render(null);
            final WritablePixelIterator ite = WritablePixelIterator.create(image);
            while (ite.next()) {
                Point position = ite.getPosition();
                ite.setPixel(new int[]{position.x, position.y});
            }

            COVERAGE_2D = new MemoryGridCoverageResource(null, Names.createLocalName(null, null, "coverage2d"), coverage, null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        try {
            final GeneralEnvelope dataEnv = new GeneralEnvelope(CRS.compound(
                    CommonCRS.WGS84.normalizedGeographic(),
                    CommonCRS.Vertical.ELLIPSOIDAL.crs(),
                    CommonCRS.Temporal.JAVA.crs()
            ));
            dataEnv.setRange(0, -180, 180);
            dataEnv.setRange(1, -90, 90);
            dataEnv.setRange(2, 100, 400);
            dataEnv.setRange(3,
                    Instant.parse("2000-06-10T10:00:00Z").toEpochMilli(),
                    Instant.parse("2000-06-10T12:00:00Z").toEpochMilli());
            final GridGeometry dataGrid = new GridGeometry(
                    new GridExtent(null, new long[4], new long[] { 2, 2, 3, 3 }, false),
                    dataEnv,
                    GridOrientation.DISPLAY
            );

            final ByteBuffer dataBuffer = ByteBuffer.wrap(new byte[] {
                    // v0 t0
                    1, 1,
                    1, 1,
                    // v1 t0
                    2, 2,
                    2, 2,
                    // v2 t0
                    3, 3,
                    3, 3,
                    // v0 t1
                    4, 4,
                    4, 4,
                    // v1 t1
                    5, 5,
                    5, 5,
                    // v2 t1
                    6, 6,
                    6, 6,
                    // v0 t2
                    7, 7,
                    7, 7,
                    // v1 t2
                    8, 8,
                    8, 8,
                    // v2 t2
                    9, 9,
                    9, 9
            });

            final BufferedGridCoverage coverage = new BufferedGridCoverage(
                    dataGrid,
                    (List)List.of(new SampleDimension.Builder().setName("test").build()),
                    RasterFactory.wrap(DataType.BYTE, dataBuffer)
            );
            COVERAGE_4D = new MemoryGridCoverageResource(null, Names.createLocalName(null, null, "coverage4d"), coverage, null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
