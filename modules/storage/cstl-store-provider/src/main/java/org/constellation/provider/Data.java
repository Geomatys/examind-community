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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.util.Classes;
import org.constellation.api.DataType;
import org.constellation.api.ServiceDef;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DimensionRange;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.repository.DataRepository;
import org.geotoolkit.storage.multires.TiledResource;
import org.geotoolkit.storage.multires.ProgressiveResource;
import org.geotoolkit.storage.multires.TileFormat;
import org.geotoolkit.storage.multires.TileMatrix;
import org.geotoolkit.storage.multires.TileMatrixSet;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.style.Style;
import org.opengis.util.GenericName;


/**
 * Information about a {@linkplain Data data}.
 *
 * @version $Id$
 * @author Cédric Briançon
 */
public interface Data<T extends Resource> {

    String KEY_EXTRA_PARAMETERS = "EXTRA";

    /**
     * Returns the time range of this layer. This method is typically much faster than
     * {@link #getAvailableTimes()} when only the first date and/or the last date are
     * needed, rather than the set of all available dates.
     *
     * @return The time range of this layer, or {@code null} if this information is not available.
     * @throws ConstellationStoreException If an error occurred while fetching the time range.
     */
    SortedSet<Date> getDateRange() throws ConstellationStoreException;

    /**
     * Returns the set of dates when such dimension is available. Note that this method may
     * be slow and should be invoked only when the set of all dates is really needed.
     * If only the first or last date is needed, consider using {@link #getDateRange()}
     * instead.
     *
     * @return A not {@code null} set of dates.
     */
    SortedSet<Date> getAvailableTimes() throws ConstellationStoreException;

    /**
     * Returns the set of elevations when such dimension is available. Note that this method may
     * be slow and should be invoked only when the set of all dates is really needed.
     *
     * @return A not {@code null} set of elevations.
     */
    SortedSet<Number> getAvailableElevations() throws ConstellationStoreException;

    /**
     * Returns the native envelope of this layer.
     */
    Envelope getEnvelope() throws ConstellationStoreException;

    /**
     */
    GenericName getName();

    /**
     */
    SortedSet<DimensionRange> getSampleValueRanges() throws ConstellationStoreException;

    /**
     * Returns {@code true} if the layer is queryable by the specified service.
     *
     */
    boolean isQueryable(ServiceDef.Query query);

    /**
     * Origin source of this data can be :
     * FeatureSet, GridCoverageResource, ... or null.
     */
    T getOrigin();

    /**
     * Get the source of resource used by this layer.
     */
    DataStore getStore();

    DataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException;

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

    boolean isGeophysic() throws ConstellationStoreException;

    /**
     *
     * @return Metadata of the resource associated to this data.
     * @throws ConstellationStoreException If we cannot access data source.
     * @deprecated Not used directly, should not be used. Please do {@code getOrigin().getMetadata();}, but check
     * before-hand if {@link #getOrigin() origin} is not null.
     */
    @Deprecated
    DefaultMetadata getResourceMetadata() throws ConstellationStoreException;

    String getResourceCRSName() throws ConstellationStoreException;

    /**
     * Computes and returns data statistics. Beware, this operation could be a heavy one.
     * TODO: refactor API.
     *
     * @param dataId Data Identifier in given repository
     * @param dataRepository Repository to use to update state of given data.
     * @return Statistics result. For now, only one implementation exists, and it returns specific
     * {@link ImageStatistics}.
     */
    Object computeStatistic(int dataId, DataRepository dataRepository) throws ConstellationStoreException;

    /**
     * Create a MapItem with the given style and parameters.
     * if style is null, the favorite style of this layer will be used.
     *
     * @param style Style to apply to the data. Can be null.
     * @param params Extra parameters usable by specific implementations. No more details available at API level. Can be null.
     */
    MapItem getMapLayer(Style style, final Map<String, Object> params) throws ConstellationStoreException;


    /**
     * Returns a map structure describing the resource of this data.
     *
     * @return Map.
     */
    default Map<String,Object> rawDescription() throws DataStoreException {

        final Resource rs = getOrigin();
        return toRawModel(rs);
    }

    static Map<String,Object> toRawModel(Resource rs) {

        final Map<String,Object> mp = new LinkedHashMap<>();

        try {
            final GenericName identifier = rs.getIdentifier().orElse(null);
            if (identifier != null) {
                mp.put("identifier", identifier.toString());
            }
        } catch (DataStoreException ex) {
            mp.put("identifier", "ERROR (" + ex.getMessage() + ")");
        }

        if (rs instanceof DataSet) {
            final DataSet cdt = (DataSet) rs;

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("DataSet", map);

            map.put("class", Classes.getShortClassName(rs));
            try {
                final Envelope env = cdt.getEnvelope().orElse(null);
                if (env != null) {
                    map.put("envelope", new GeneralEnvelope(env).toString());
                    if (env.getCoordinateReferenceSystem() != null) {
                        map.put("crs", env.getCoordinateReferenceSystem().toWKT());
                    }
                }
            } catch (DataStoreException ex) {
                map.put("envelope", "ERROR (" + ex.getMessage() + ")");
            }
        }

        if (rs instanceof FeatureSet) {
            final FeatureSet cdt = (FeatureSet) rs;

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("FeatureSet", map);

            map.put("class", Classes.getShortClassName(rs));
            map.put("writable", (rs instanceof WritableFeatureSet));

            try {
                final FeatureType type = cdt.getType();
                map.put("type name", type.getName().toString());
            } catch (DataStoreException ex) {
                map.put("type name", "ERROR (" + ex.getMessage() + ")");
            }
        }

        if (rs instanceof GridCoverageResource) {
            final GridCoverageResource cdt = (GridCoverageResource) rs;

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("GridCoverageResource", map);

            map.put("class", Classes.getShortClassName(rs));
            try {
                final GridGeometry gridGeometry = cdt.getGridGeometry();
                if (gridGeometry.isDefined(GridGeometry.CRS)) {
                    map.put("crs", gridGeometry.getCoordinateReferenceSystem().toWKT());
                }
                if (gridGeometry.isDefined(GridGeometry.ENVELOPE)) {
                    map.put("envelope", new GeneralEnvelope(gridGeometry.getEnvelope()).toString());
                }
                if (gridGeometry.isDefined(GridGeometry.EXTENT)) {
                    final GridExtent extent = gridGeometry.getExtent();
                    map.put("extent", extent.toString());
                }
                if (gridGeometry.isDefined(GridGeometry.GRID_TO_CRS)) {
    //                map.put("grid to crs", gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER).toWKT());
                }
            } catch (DataStoreException ex) {
                map.put("grid geometry", "ERROR (" + ex.getMessage() + ")");
            }

            try {
                final List<SampleDimension> sampleDimensions = cdt.getSampleDimensions();
                final List<Map> sds = new ArrayList<>();
                for (SampleDimension sd : sampleDimensions) {
                    final Map<String,Object> tf = new LinkedHashMap<>();
                    tf.put("name", String.valueOf(sd.getName()));
                    tf.put("unit", String.valueOf(sd.getUnits().orElse(Units.UNITY).getSymbol()));
                    tf.put("background", String.valueOf(sd.getBackground().orElse(null)));

                    final List<Map> cats = new ArrayList<>();
                    for (Category cat : sd.getCategories()) {
                        final Map<String,Object> cf = new LinkedHashMap<>();
                        cf.put("name", cat.getName().toString());
                        cf.put("quantitative", cat.isQuantitative());
                        cats.add(cf);
                    }
                    tf.put("categories", cats);
                    sds.add(tf);
                }
                map.put("sample dimensions", sds);
            } catch (DataStoreException ex) {
                map.put("sample dimensions", "ERROR (" + ex.getMessage() + ")");
            }

        }

        if (rs instanceof TiledResource) {
            final TiledResource cdt = (TiledResource) rs;
            final TileFormat tileFormat = cdt.getTileFormat();

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("MultiResolutionResource", map);

            map.put("class", Classes.getShortClassName(rs));

            if (tileFormat != null) {
                final Map<String,Object> tf = new LinkedHashMap<>();
                map.put("tile format", tf);
                tf.put("mime type", tileFormat.getMimeType());
                tf.put("provider id", tileFormat.getProviderId());
                tf.put("compression", tileFormat.getCompression().name());
            }

            try {
                final Collection<? extends TileMatrixSet> models = cdt.getTileMatrixSets();
                final List<Map> mms = new ArrayList<>();
                for (TileMatrixSet mrm : models) {
                    final Map<String,Object> tf = new LinkedHashMap<>();
                    mms.add(tf);
                    tf.put("identifier", mrm.getIdentifier());
                    tf.put("format", mrm.getFormat());

                    if (mrm instanceof TileMatrixSet) {
                        final TileMatrixSet p = (TileMatrixSet) mrm;

                        final Map<String,Object> pf = new LinkedHashMap<>();
                        tf.put("pyramid", pf);

                        pf.put("crs", p.getCoordinateReferenceSystem().toWKT());
                        pf.put("envelope", new GeneralEnvelope(p.getEnvelope()).toString());

                        final List<Map> moss = new ArrayList<>();
                        for (TileMatrix m : p.getTileMatrices()) {
                            final Map<String,Object> mf = new LinkedHashMap<>();
                            moss.add(mf);
                            mf.put("identifier", m.getIdentifier());
                            mf.put("scale", m.getScale());
                            mf.put("envelope", new GeneralEnvelope(m.getEnvelope()).toString());
                            mf.put("grid size width", m.getGridSize().width);
                            mf.put("grid size height", m.getGridSize().height);
                            mf.put("tile size width", m.getTileSize().width);
                            mf.put("tile size height", m.getTileSize().height);
                        }
                        pf.put("mosaics", moss);
                    }
                }
                map.put("models", mms);
            } catch (DataStoreException ex) {
                map.put("models", "ERROR (" + ex.getMessage() + ")");
            }
        }

        if (rs instanceof Aggregate) {
            final Aggregate cdt = (Aggregate) rs;

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("Aggregate", map);

            map.put("class", Classes.getShortClassName(rs));
            map.put("writable", (rs instanceof WritableAggregate));

            try {
                final Collection<? extends Resource> components = cdt.components();
                map.put("nb components", components.size());
                int i = 0;
                for (Resource r : components) {
                    map.put("component[" + i++ + "]", toRawModel(r));
                }
            } catch (DataStoreException ex) {
                map.put("components", "ERROR (" + ex.getMessage() + ")");
            }
        }

        if (rs instanceof ProgressiveResource) {
            final ProgressiveResource pr = (ProgressiveResource) rs;

            final Map<String,Object> map = new LinkedHashMap<>();
            mp.put("ProgressiveResource", map);

            map.put("class", Classes.getShortClassName(rs));
// TODO : after geotk version update, uncomment code
//            try {
//                final TileGenerator generator = pr.getGenerator();
//                if (generator instanceof CoverageTileGenerator) {
//                    CoverageTileGenerator ctg = (CoverageTileGenerator) generator;
//
//                    GridCoverageResource origin = ctg.getOrigin();
//                    if (origin != null) {
//                        map.put("origin", toRawModel(origin));
//                    }
//
//                } else if (generator != null) {
//                    map.put("generator", generator.toString());
//                }
//            } catch (Exception ex) {
//                map.put("generator", "ERROR (" + ex.getMessage() + ")");
//            }
        }

        return mp;
    }

}
