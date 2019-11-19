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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.stream.DoubleStream;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.api.DataType;
import static org.constellation.api.StatisticState.*;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.util.DataStatisticsListener;
import org.constellation.provider.util.ImageStatisticDeserializer;
import org.constellation.repository.DataRepository;
import org.geotoolkit.coverage.grid.GridGeometryIterator;
import org.geotoolkit.coverage.grid.GridIterator;
import org.geotoolkit.coverage.worldfile.FileCoverageResource;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.map.DefaultCoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.coverage.resample.ResampleProcess;
import org.geotoolkit.processing.coverage.statistics.Statistics;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.geotoolkit.storage.feature.query.QueryBuilder;
import org.geotoolkit.storage.multires.MultiResolutionResource;
import org.geotoolkit.storage.multires.Pyramid;
import org.geotoolkit.storage.multires.Pyramids;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.StyleConstants;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Regroups information about a {@linkplain Data data}.
 *
 * TODO : cache grid geometry ? It could really speed up most of the methods here.
 * However, the main problem is that data could be updated anytime, so we should
 * think about a cache with a short life time (few seconds).
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultCoverageData extends AbstractData implements CoverageData {

    private static final MutableStyle DEFAULT =
            new DefaultStyleFactory().style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER);

    /**
     * AxisDirection name for Lat/Long, Elevation, temporal dimensions.
     */
    private static final List<String> COMMONS_DIM = UnmodifiableArrayList.wrap(new String[] {
            "NORTH", "EAST", "SOUTH", "WEST",
            "UP", "DOWN",
            "FUTURE", "PAST"});

    private final GridCoverageResource ref;
    private final DataStore store;


    public DefaultCoverageData(final GenericName name, final GridCoverageResource ref, final DataStore store){
        super(name);
        this.ref = ref;
        this.store = store;
    }

    @Override
    public GridCoverageResource getOrigin() {
        return ref;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GridCoverage getCoverage(final Envelope envelope, final Dimension dimension, final Double elevation,
                                      final Date time) throws ConstellationStoreException
    {
        double[] res = null;
        if (envelope != null && dimension != null) {
            //compute resolution
            res = new double[envelope.getDimension()];
            for (int i = 0 ; i < envelope.getDimension(); i++) {
                switch (i) {
                    case 0: res[i] = envelope.getSpan(i) / dimension.width; break;
                    case 1: res[i] = envelope.getSpan(i) / dimension.height; break;
                    default : res[i] = envelope.getSpan(i); break;
                }
            }
        }

        try {
            final GridGeometry refGrid = getGeometry();
            final CoordinateReferenceSystem crs2D;
            final GridGeometry grid;
            if (envelope != null) {
                crs2D = CRS.getHorizontalComponent(envelope.getCoordinateReferenceSystem());
                GridDerivation gd;
                if (res != null) {
                    gd = refGrid.derive().subgrid(envelope, res);
                } else {
                    gd = refGrid.derive().subgrid(envelope);
                }
                gd = gd.sliceByRatio(0.5, 0, 1);
                grid = gd.build();
            } else {
                crs2D = CRS.getHorizontalComponent(refGrid.getCoordinateReferenceSystem());
                grid = refGrid;
            }

            final GridCoverage cov = ref.read(grid);
            return new ResampleProcess(cov, crs2D, null, null, null).executeNow();

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);

        } catch (CancellationException | ProcessException ex) {
            throw new ConstellationStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapLayer getMapLayer(MutableStyle style, final Map<String, Object> params) throws ConstellationStoreException {
        if(style == null){
            style = getDefaultStyle();
        }

        final MapLayer layer = MapBuilder.createCoverageLayer(ref, style);

        // EXTRA FILTER extra parameter ////////////////////////////////////////
        if (params != null && layer instanceof DefaultCoverageMapLayer) {
            final Map<String,?> extras = (Map<String, ?>) params.get(KEY_EXTRA_PARAMETERS);
            if (extras != null) {
                Filter filter = null;
                for (String key : extras.keySet()) {
                    if (key.equalsIgnoreCase("cql_filter")) {
                        final Object extra = extras.get(key);
                        String cqlFilter = null;
                        if (extra instanceof List) {
                            cqlFilter = ((List) extra).get(0).toString();
                        } else if (extra instanceof String){
                            cqlFilter = (String)extra;
                        }
                        if (cqlFilter != null){
                            filter = buildCQLFilter(cqlFilter, filter);
                        }
                    } else if (key.startsWith("dim_") || key.startsWith("DIM_")){
                        final String dimValue = ((List)extras.get(key)).get(0).toString();
                        final String dimName  = key.substring(4);
                        filter = buildDimFilter(dimName, dimValue, filter);
                    }
                }
                if (filter != null) {
                    final DefaultCoverageMapLayer cml = (DefaultCoverageMapLayer) layer;
                    try {
                        cml.setQuery(QueryBuilder.filtered(cml.getResource().getIdentifier().toString(), filter));
                    } catch (DataStoreException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }
        }
        ////////////////////////////////////////////////////////////////////////

        return layer;
    }

    /**
     * {@inheritDoc}
     */
    private MutableStyle getDefaultStyle() {
        return DEFAULT;
    }

    /**
     * Search for times for which data is available.
     * TODO : use Instant instead of dates.
     *
     * @return a set of all available dates for this data. Never null, but can be
     * empty if this data has no time axis defined.
     * @throws ConstellationStoreException if we failed reading data geometry, or converting
     * native time units in proper java dates.
     */
    @Override
    public SortedSet<Date> getAvailableTimes() throws ConstellationStoreException {
        SortedSet<Date> dates = new TreeSet<>();
        try {
            GridGeometry ggg = getGeometry();
            if (ggg != null) {
                final CoordinateReferenceSystem crs = ggg.getCoordinateReferenceSystem();
                final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
                if (temporalCRS != null) {

                    final MathTransform mt = CRS.findOperation(temporalCRS, CommonCRS.Temporal.JAVA.crs(), null).getMathTransform();

                    final double[] positions = getPositions(temporalCRS, ggg);
                    mt.transform(positions, 0, positions, 0, positions.length);

                    for (final double pos : positions) {
                        dates.add(new Date((long) pos));
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Unable to get a GridGeometry for coverage data:{0}", name);
            }
        } catch (FactoryException | TransformException | DataStoreException | IncompleteGridGeometryException e) {
            throw new ConstellationStoreException("Unable to extract available times from coverage data " + name, e);
        }
        return dates;
    }
    /**
     * Search for information about vertical dimension of this data (if any).
     * @return The Identifier of the vertical CRS used, as with available
     * positions in it. Never null, but can be empty (if no vertical dimension
     * is used by the data).
     *
     * @throws ConstellationStoreException If we cannot get data grid, or an error occurs
     * while searching for a CRS identifier.
     */
    @Override
    public SortedSet<Number> getAvailableElevations() throws ConstellationStoreException {
        final TreeSet<Number> result = new TreeSet<>();
        try {
            final GridGeometry ggg = getGeometry();
            if (ggg != null) {
                final CoordinateReferenceSystem crs = ggg.getCoordinateReferenceSystem();
                final VerticalCRS verticalCrs = CRS.getVerticalComponent(crs, true);
                if (verticalCrs != null) {

                    final double[] positions = getPositions(verticalCrs, ggg);
                    for (final double pos : positions) {
                        result.add(pos);
                    }

                }
            } else {
                LOGGER.log(Level.WARNING, "Unable to get a GridGeometry for coverage data:{0}", name);
            }
        } catch (TransformException | DataStoreException | IncompleteGridGeometryException e) {
            throw new ConstellationStoreException("Unable to extract available elevations from coverage data " + name, e);
        }

        return result;
    }

    @Override
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        GridGeometry ggg = getGeometry();
        if (ggg != null) {
            return ggg.getEnvelope();
        }
        LOGGER.log(Level.WARNING, "Unable to get a GridGeometry for coverage data:{0}", name);
        return null;
    }

    @Override
    public String getImageFormat() {
         if (ref instanceof FileCoverageResource) {
            FileCoverageResource fref = (FileCoverageResource) ref;
            if (fref.getSpi() != null &&
                fref.getSpi().getMIMETypes() != null &&
                fref.getSpi().getMIMETypes().length > 0) {
                return fref.getSpi().getMIMETypes()[0];
            }
        }
        return "";
    }

    @Override
    public SpatialMetadata getSpatialMetadata() throws ConstellationStoreException {
        return null;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws ConstellationStoreException {
        try {
            return ref.getSampleDimensions();
        } catch (CancellationException | DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    /**
     * Get back grid geometry for the data.
     *
     * @return The grid geometry of this data.
     * @throws CoverageStoreException If we cannot extract geometry information
     * from the resource.
     */
    @Override
    public GridGeometry getGeometry() throws ConstellationStoreException {
        try {
            return ref.getGridGeometry();
        } catch (DataStoreException e) {
            throw new ConstellationStoreException(e.getMessage(), e);
        }
    }

    /**
     * Find all spatial points available for a single dimension.
     * @implNote :
     * <ul>
     * <li>Require 1D system as input.</li>
     * <li>do NOT work when queried dimension index is flipped between grid
     * and spatial envelope.</li>
     * <li>Use an approach which works with non-linear axes : find all grid
     * steps, then reproject them all to find dimension values.</li>
     * </ul>
     * @param dimOfInterest Dimension to get spatial points for.
     * @return An ordered list (in grid order) of available values.
     * @throws IllegalArgumentException if given CRS is not a single dimension one.
     * @throws CoverageStoreException If we cannot read data grid.
     * @throws TransformException If we cannot transform grid coordinate into
     * spatial ones.
     */
    private double[] getPositions(final SingleCRS dimOfInterest, GridGeometry geom) throws DataStoreException, TransformException {
        ArgumentChecks.ensureNonNull("Dimension of interest", dimOfInterest);
        ArgumentChecks.ensureDimensionMatches("Dimension of interest", 1, dimOfInterest);
        int dimIdx = 0;
        for (SingleCRS part : CRS.getSingleComponents(geom.getCoordinateReferenceSystem())) {
            if (part == dimOfInterest) break;
            dimIdx += part.getCoordinateSystem().getDimension();
        }

        final MathTransform gridToCRS = geom.getGridToCRS(PixelInCell.CELL_CENTER);
        final TransformSeparator sep = new TransformSeparator(gridToCRS);
        sep.addSourceDimensions(dimIdx);
        sep.addTargetDimensions(dimIdx);
        final GridExtent extent = geom.getExtent();
        final int dimGridSpan = Math.toIntExact(extent.getSize(dimIdx));
        try {
            final MathTransform targetTransform = sep.separate();
            final double[] gridPoints = DoubleStream.iterate(0, i -> i + 1)
                    .limit(dimGridSpan)
                    .toArray();
            final double[] axisValues = new double[gridPoints.length];
            targetTransform.transform(gridPoints, 0, axisValues, 0, gridPoints.length);
            return axisValues;
        } catch (Exception e) {
            // Fallback on costly approach : project entire grid points
            final int[] steps = new int[extent.getDimension()];
            steps[dimIdx] = 1;
            final GridIterator it = new GridIterator(extent, steps);
            final double[] values = new double[dimGridSpan];
            int i = 0;
            final double[] buffer = new double[extent.getDimension()];
            while (it.hasNext() && i < values.length) {
                final GridExtent next = it.next();
                for (int j = 0; j < buffer.length; j++) {
                    buffer[j] = next.getLow(j);
                }
                gridToCRS.transform(buffer, 0, buffer, 0, 1);
                values[i++] = buffer[dimIdx];
            }
            return values;
        }
    }

    @Override
    public DataStore getStore() {
        return store;
    }

    @Override
    public CoverageDataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        final CoverageDataDescription description = new CoverageDataDescription();
        if (statInfo != null) {
            ImageStatistics stats = getDataStatistics(statInfo);
            if (stats != null) {
                // Bands description.
                for (int i = 0; i < stats.getBands().length; i++) {
                    final ImageStatistics.Band band = stats.getBand(i);
                    final String bandName = band.getName();
                    String indice = String.valueOf(i);
                    final double min = band.getMin();
                    final double max = band.getMax();
                    double[] noData = band.getNoData();
                    description.getBands().add(new BandDescription(indice, bandName, min, max, noData));
                }
            }
        }

        // Geographic extent description.
        final GridGeometry ggg = getGeometry();
        if (ggg != null && ggg.isDefined(GridGeometry.ENVELOPE)) {
            final Envelope envelope = ggg.getEnvelope();
            DataProviders.fillGeographicDescription(envelope, description);
        } else {
            LOGGER.log(Level.WARNING, "Unable to get a GridGeometry for coverage data:{0}", name);
        }
        return description;
    }

    @Override
    public DataType getDataType() {
        return DataType.COVERAGE;
    }

    @Override
    public String getSubType() throws ConstellationStoreException {
        Object origin = getOrigin();
        if (origin instanceof MultiResolutionResource) {
            return "pyramid";
        }
        return null;
    }

    @Override
    public Boolean isRendered() {
        Object origin = getOrigin();
        if (origin instanceof MultiResolutionResource) {
//            try {
//                ViewType packMode = ((MultiResolutionResource) origin).getPackMode();
//                if (ViewType.RENDERED.equals(packMode)) {
                    return Boolean.TRUE;
//                }
//            } catch (DataStoreException e) {
//                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
//            }
        }
        return Boolean.FALSE;
    }

    @Override
    public ProviderPyramidChoiceList.CachePyramid getPyramid() throws ConstellationStoreException {
         try {
            final Object origin = getOrigin();
            if (origin instanceof MultiResolutionResource) {
                final MultiResolutionResource cacheRef = (MultiResolutionResource) getOrigin();
                final Collection<Pyramid> pyramids = Pyramids.getPyramids(cacheRef);
                if(pyramids.isEmpty()) return null;

                //TODO what do we do if there are more then one pyramid ?
                //it the current state of constellation there is only one pyramid
                final Pyramid pyramid = pyramids.iterator().next();
                final Identifier crsid = pyramid.getCoordinateReferenceSystem().getIdentifiers().iterator().next();

                final ProviderPyramidChoiceList.CachePyramid cache = new ProviderPyramidChoiceList.CachePyramid();
                cache.setCrs(crsid.getCode());
                cache.setScales(pyramid.getScales());
                //cache.setProviderId(provider.getId());
                //cache.setDataId(layerName);
                //cache.setConform(childRec.getIdentifier().startsWith("conform_"));

                return cache;
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return null;
    }

    @Override
    public boolean isGeophysic() throws ConstellationStoreException {
        boolean isGeophysic = false;
        try {
            final List<SampleDimension> dims = ref.getSampleDimensions();
            if(dims!=null && !dims.isEmpty()){
                isGeophysic = true;
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return isGeophysic;
    }

    @Override
    public List<org.constellation.dto.Dimension> getSpecialDimensions() throws ConstellationStoreException {
        final List<org.constellation.dto.Dimension> dimensions = new ArrayList<>();

        final GridGeometry gridGeom = getGeometry();
        final CoordinateReferenceSystem crsLayer                       = gridGeom.getCoordinateReferenceSystem();
        final Map<Integer, CoordinateReferenceSystem> indexedDecompose = ReferencingUtilities.indexedDecompose(crsLayer);

        //-- for each CRS part if crs is not 2D part or Temporal or elevation add value
        for (Integer key : indexedDecompose.keySet()) {
            final CoordinateReferenceSystem currentCrs = indexedDecompose.get(key);

            //-- in this case we add value only if crs is one dimensional -> 1 dimension -> getAxis(0).
            final CoordinateSystemAxis axis = currentCrs.getCoordinateSystem().getAxis(0);

            if (!COMMONS_DIM.contains(axis.getDirection().name())) {

                final GridGeometryIterator ite = new GridGeometryIterator(gridGeom, key);
                final List<NumberRange> numberRanges = new ArrayList<>();
                while (ite.hasNext()) {
                    GridGeometry slice = ite.next();
                    Envelope envelope = slice.getEnvelope();
                    numberRanges.add(NumberRange.create(envelope.getMinimum(key), true, envelope.getMaximum(key), false));
                }

                final StringBuilder values = new StringBuilder();
                for (int i = 0,n=numberRanges.size(); i < n; i++) {
                    final NumberRange numberRange = numberRanges.get(i);
                    values.append(numberRange.getMinDouble());
                    if (i != numberRanges.size() - 1) values.append(',');
                }
                final String unitStr = (axis.getUnit() != null) ? axis.getUnit().toString() : null;
                final String defaut = (!(numberRanges.size() != 0)) ? ""+numberRanges.get(0).getMinDouble() : null;
                String unitSymbol;
                try {
                    unitSymbol = new org.apache.sis.measure.UnitFormat(Locale.UK).format(axis.getUnit());
                } catch (IllegalArgumentException e) {
                    // Workaround for one more bug in javax.measure...
                    unitSymbol = unitStr;
                }
                dimensions.add(new org.constellation.dto.Dimension(values.toString(),
                                                                   axis.getName().getCode(),
                                                                   unitStr,
                                                                   unitSymbol,
                                                                   defaut,
                                                                   null,null,null));
            }
        }
        return dimensions;
    }

    @Override
    public DefaultMetadata getResourceMetadata() throws ConstellationStoreException {
        try {
            if (ref instanceof GridCoverageResource) {
                return new DefaultMetadata(ref.getMetadata());
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return null;
    }

    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        try {
            final GridGeometry ggg = getGeometry();
            if (ggg != null) {
                final CoordinateReferenceSystem crs = ggg.getCoordinateReferenceSystem();
                if (crs != null) {
                    final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs, true);
                    if (crsIdentifier != null) {
                        return crsIdentifier;
                    }
                }
            }
        } catch(Exception ex) {
            LOGGER.finer(ex.getMessage());
        }
        return null;
    }

    @Override
    public void computeStatistic(int dataId, DataRepository dataRepository) {
        // Hack compute statistic from 5% of the first slice
        try {
            GridGeometry gg = getGeometry();
            if (gg != null) {
                GridExtent extent = gg.getExtent();
                int[] subSample = new int[extent.getDimension()];
                subSample[0] = Math.round(extent.getSize(0) * 0.05f);
                subSample[1] = Math.round(extent.getSize(1) * 0.05f);
                for (int i = 2; i < extent.getDimension(); i++) {
                    subSample[i] = Math.toIntExact(extent.getSize(i));
                }
                gg = gg.derive().subsample(subSample).build();

                final GridCoverage cov = ref.read(gg);
                final org.geotoolkit.process.Process process = new Statistics(cov, false);
                process.addListener(new DataStatisticsListener(dataId, dataRepository));
                process.call();
            }
        } catch(Throwable ex) {
            //we tryed
        }
    }



    /**
     * Get and parse data statistics.
     *
     * @param d
     *
     * @return ImageStatistics object or null if data is not a coverage or if Statistics were not computed.
     * @throws org.constellation.exception.ConfigurationException
     */
    public static ImageStatistics getDataStatistics(StatInfo s) throws ConstellationStoreException {
        final String state = s.getState();
        final String result = s.getResult();
        try {
            if (state != null) {
                switch (state) {
                    case STATE_PARTIAL : //fall through
                    case STATE_COMPLETED :
                        if (result != null && result.startsWith("{")) {
                            return deserializeImageStatistics(result);
                        } else {
                            LOGGER.log(Level.WARNING, "Unreadable statistics flagged as {0}", state);
                            return null;
                        }
                    case STATE_PENDING : return null;
                    case STATE_ERROR :
                        //can have partial statistics even if an error occurs.
                        if (result != null && result.startsWith("{")) {
                            return deserializeImageStatistics(result);
                        } else {
                            return null;
                        }
                }
            }

        } catch (IOException e) {
            throw new ConstellationStoreException("Invalid statistic JSON format for data. ", e);
        }
        return null;
    }

    private static ImageStatistics deserializeImageStatistics(String state) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ImageStatistics.class, new ImageStatisticDeserializer()); //custom deserializer
        mapper.registerModule(module);
        return mapper.readValue(state, ImageStatistics.class);
    }
}
