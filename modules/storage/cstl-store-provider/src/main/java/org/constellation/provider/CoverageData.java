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
import java.util.List;
import java.util.Optional;

import org.opengis.geometry.Envelope;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.map.MapItem;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.style.Style;

import org.geotoolkit.image.io.metadata.SpatialMetadata;

import org.constellation.exception.ConstellationStoreException;

/**
 * Coverage extension of a {@link Data}, which add some methods specific
 * for coverage layers.
 *
 * @version $Id$
 * @author Cédric Briançon (Geomatys)
 *
 * @since 0.4
 */
public interface CoverageData extends Data<GridCoverageResource> {

    /**
     * @return The original MIME type of the data.
     */
    Optional<String> getImageFormat();

    SpatialMetadata getSpatialMetadata() throws ConstellationStoreException;

    List<SampleDimension> getSampleDimensions() throws ConstellationStoreException;

    /**
     * Returns the coverage requested.
     *
     * @param envelope The {@link Envelope} to request. Should  not be {@code null}.
     * @param dimension A {@link Dimension} for the image. Should  not be {@code null}.
     *
     * @throws ConstellationStoreException
     */
    GridCoverage getCoverage(final Envelope envelope, final Dimension dimension) throws ConstellationStoreException;

    /**
     * Get back grid geometry for the data.
     *
     * @return The grid geometry of this data.
     * @throws ConstellationStoreException If we cannot extract geometry information from the resource.
     */
    GridGeometry getGeometry() throws ConstellationStoreException;

    /**
     * Return the special dimensions that are not Temporal or elevation
     * @return
     */
    List<org.constellation.dto.Dimension> getSpecialDimensions() throws ConstellationStoreException;

    /**
     *
     * @param style Style to apply to the data. Can be null.
     * @param forceSampleDimensions if set to {@code true} the sample dimensions will be overriden.
     *
     * @return A map Item.
     */
    MapItem getMapLayer(Style style, boolean forceSampleDimensions) throws ConstellationStoreException;
}
