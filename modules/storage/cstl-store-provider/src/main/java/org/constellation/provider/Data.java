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
import org.apache.sis.measure.MeasurementRange;
import org.constellation.api.ServiceDef;
import org.geotoolkit.util.DateRange;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;

import java.util.Date;
import java.util.SortedSet;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.constellation.api.DataType;
import org.constellation.dto.DataDescription;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.metadata.ImageStatistics;
import org.opengis.util.GenericName;


/**
 * Information about a {@linkplain Data data}.
 *
 * @version $Id$
 * @author Cédric Briançon
 */
public interface Data {

    String KEY_EXTRA_PARAMETERS = "EXTRA";

    /**
     * Default legend size, if not specified in the {@code GetLegend} request.
     */
    Dimension LEGEND_SIZE = new Dimension(200, 40);

    /**
     * Returns the time range of this layer. This method is typically much faster than
     * {@link #getAvailableTimes()} when only the first date and/or the last date are
     * needed, rather than the set of all available dates.
     *
     * @return The time range of this layer, or {@code null} if this information is not available.
     * @throws ConstellationStoreException If an error occurred while fetching the time range.
     */
    DateRange getDateRange() throws ConstellationStoreException;

    /**
     * Returns the set of dates when a coverage is available. Note that this method may
     * be slow and should be invoked only when the set of all dates is really needed.
     * If only the first or last date is needed, consider using {@link #getDateRange()}
     * instead.
     *
     */
    SortedSet<Date> getAvailableTimes() throws ConstellationStoreException;

    /**
     */
    SortedSet<Number> getAvailableElevations() throws ConstellationStoreException;

    /**
     */
    GeographicBoundingBox getGeographicBoundingBox() throws ConstellationStoreException;

    /**
     * Returns the native envelope of this layer.
     */
    Envelope getEnvelope() throws ConstellationStoreException;

    /**
     */
    GenericName getName();

    /**
     */
    MeasurementRange<?>[] getSampleValueRanges();

    /**
     * Returns {@code true} if the layer is queryable by the specified service.
     *
     */
    boolean isQueryable(ServiceDef.Query query);

    /**
     * Origin source of this data can be :
     * FeatureCollection, CoverageRefence, null.
     */
    Object getOrigin();

    /**
     * Get the source of resource used by this layer.
     */
    DataStore getStore();

    DataDescription getDataDescription(ImageStatistics stats) throws ConstellationStoreException;

    DataType getDataType();

    /**
     * Return the geometry type for a Feature data.
     * For a Coverage data return "pyramid" or {@code null}.
     * return {@code null}, for other data type.
     *
     * (TODO move to GeoData when the interface will be clean of geotk dependencies and moved to provider base module)
     */
    String getSubType() throws ConstellationStoreException;

    /**
     * Return the rendered state of a coverage data
     * return {@code null}, for other data type.
     *
     * (TODO move to CoverageData when the interface will be clean of geotk dependencies and moved to provider base module)
     */
    Boolean isRendered();

    /**
     * Return a list of pyramid linked to this coverage data.
     * return an empty list, for other data type.
     *
     * (TODO move to CoverageData when the interface will be clean of geotk dependencies and moved to provider base module)
     */
    ProviderPyramidChoiceList.CachePyramid getPyramid() throws ConstellationStoreException;

    boolean isGeophysic() throws ConstellationStoreException;

    DefaultMetadata getResourceMetadata() throws ConstellationStoreException;

    String getResourceCRSName() throws ConstellationStoreException;

}
