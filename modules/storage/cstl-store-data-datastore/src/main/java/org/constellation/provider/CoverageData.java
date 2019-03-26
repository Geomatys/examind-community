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
package org.constellation.provider;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.opengis.geometry.Envelope;

/**
 * Coverage extension of a {@link Data}, which add some methods specific
 * for coverage layers.
 *
 * @version $Id$
 * @author Cédric Briançon (Geomatys)
 *
 * @since 0.4
 */
public interface CoverageData extends GeoData {
    /**
     */
    String getImageFormat();

    /**
     */
    @Deprecated
    String getRemarks();

    /**
     */
    @Deprecated
    String getThematic();

    SpatialMetadata getSpatialMetadata() throws ConstellationStoreException;

    List<GridSampleDimension> getSampleDimensions() throws ConstellationStoreException;

    /**
     * Returns the coverage requested.
     *
     * @param envelope The {@link Envelope} to request. Should  not be {@code null}.
     * @param dimension A {@link Dimension} for the image. Should  not be {@code null}.
     * @param elevation The elevation to request, in the case of nD data.
     * @param time The date for the data, in the case of temporal data.
     *
     * @throws ConstellationStoreException
     * @throws java.io.IOException
     */
    GridCoverage2D getCoverage(final Envelope envelope, final Dimension dimension,
            final Double elevation, final Date time) throws ConstellationStoreException, IOException;

    GeneralGridGeometry getGeometry() throws DataStoreException;
}
