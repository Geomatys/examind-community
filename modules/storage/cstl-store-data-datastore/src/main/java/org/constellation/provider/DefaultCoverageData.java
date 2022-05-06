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

import org.constellation.provider.util.StatsUtilities;
import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.stream.DoubleStream;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.internal.storage.image.WorldFileStore;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.api.DataType;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.util.DataStatisticsListener;
import org.constellation.repository.DataRepository;
import org.geotoolkit.coverage.grid.GridGeometryIterator;
import org.geotoolkit.coverage.grid.GridIterator;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.processing.coverage.statistics.Statistics;
import org.geotoolkit.processing.coverage.statistics.StatisticsDescriptor;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.StyleConstants;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.Style;
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
public class DefaultCoverageData extends DefaultGeoData<GridCoverageResource> implements CoverageData {

    private static final MutableStyle DEFAULT =
            new DefaultStyleFactory().style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER);

    /**
     * AxisDirection name for Lat/Long, Elevation, temporal dimensions.
     */
    private static final List<String> COMMONS_DIM = UnmodifiableArrayList.wrap(new String[] {
            "NORTH", "EAST", "SOUTH", "WEST",
            "UP", "DOWN",
            "FUTURE", "PAST"});

    public DefaultCoverageData(final GenericName name, final GridCoverageResource ref, final DataStore store){
        super(name, ref, store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapItem getMapLayer(Style styleI, boolean forceSampleDimensions) throws ConstellationStoreException {
        if (!forceSampleDimensions) {
            return getMapLayer(styleI);
        } else {
            MapLayer layer = MapBuilder.createLayer(forceSampleDimensions(origin));
            if (styleI == null) {
                styleI = getDefaultStyle();
            }
            layer.setStyle(styleI);
            final String title = getName().tip().toString();
            layer.setIdentifier(title);
            layer.setTitle(title);
            return layer;
        }
    }

    /**
     * find the type of data we are dealing with, geophysic or photographic
     *
     * @param inRef
     * @return
     * @throws ConstellationException
     */
    private GridCoverageResource forceSampleDimensions(GridCoverageResource inRef) throws ConstellationStoreException {
        try {
            final List<SampleDimension> sampleDimensions = inRef.getSampleDimensions();
            if (sampleDimensions != null) {
                final int nbBand = sampleDimensions.size();
                for (int i = 0; i < nbBand; i++) {
                    if (sampleDimensions.get(i).getCategories() != null) {
                        return inRef;
                    }
                }

                //no sample dimension categories, we force some categories
                //this is a bypass solution to avoid black border images in pyramids
                //note : we need a pyramid storage model that doesn't produce any pixels
                //outside the original coverage area
                GridGeometry gg = inRef.getGridGeometry();
                RenderedImage img = readSmallImage(inRef, gg);
                if (img == null) {
                    return inRef;
                }
                final int dataType = img.getSampleModel().getDataType();
                final List<SampleDimension> newDims = new ArrayList<>();
                for (int i = 0; i < nbBand; i++) {
                    final SampleDimension sd = sampleDimensions.get(i);
                    NumberRange range;
                    switch (dataType) {
                        case DataBuffer.TYPE_BYTE : range = NumberRange.create(0, true, 255, true); break;
                        case DataBuffer.TYPE_SHORT : range = NumberRange.create(Short.MIN_VALUE, true, Short.MAX_VALUE, true); break;
                        case DataBuffer.TYPE_USHORT : range = NumberRange.create(0, true, 0xFFFF, true); break;
                        case DataBuffer.TYPE_INT : range = NumberRange.create(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true); break;
                        default : range = NumberRange.create(-Double.MAX_VALUE, true, +Double.MAX_VALUE, true); break;
                    }

                    final SampleDimension nsd = new SampleDimension.Builder()
                            .setName(sd.getName())
                            .addQuantitative("data", range, (MathTransform1D) MathTransforms.linear(1, 0), sd.getUnits().orElse(null))
                            .build();
                    newDims.add(nsd);
                }
                inRef = new ForcedSampleDimensionsCoverageResource(inRef, newDims);
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException("Failed to extract no-data values for resampling " + ex.getMessage(),ex);
        }
        return inRef;
    }

    private RenderedImage readSmallImage(GridCoverageResource ref, GridGeometry gg) throws DataStoreException{
        //read a single pixel value
        try {
            double[] resolution = gg.getResolution(false);
            final GeneralEnvelope envelope = new GeneralEnvelope(gg.getEnvelope());
            for(int i=0;i<resolution.length;i++){
                resolution[i] = envelope.getSpan(i)/ 5.0;
            }

            GridGeometry query = gg.derive().subgrid(envelope, resolution).sliceByRatio(0.5, 0,1).build();
            return ref.read(query).render(null);
        } catch (IncompleteGridGeometryException ex){}
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GridCoverage getCoverage(final Envelope envelope, final Dimension dimension) throws ConstellationStoreException
    {
        try {
            final GridGeometry refGrid = getGeometry();

            GridExtent extent = null;
            if (dimension != null) {
                long[] high = new long[refGrid.getDimension()];
                high[0] = dimension.width  -1;
                high[1] = dimension.height -1;
                extent = new GridExtent(null, null, high, true);
            }

            final GridGeometry grid;
            if (envelope != null || extent != null) {
                 grid = new GridGeometry(extent, envelope, GridOrientation.REFLECTION_Y);
            } else {
                grid = refGrid;
            }
            return origin.read(grid);

        } catch (Exception ex) {
            throw new ConstellationStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MutableStyle getDefaultStyle() {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw new ConstellationStoreException("Unable to extract available elevations from coverage data " + name, e);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        return getEnvelope(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Envelope getEnvelope(CoordinateReferenceSystem crs) throws ConstellationStoreException {
        GridGeometry ggg = getGeometry();
        if (ggg != null && ggg.isDefined(GridGeometry.ENVELOPE)) {
            if (crs != null) {
                try {
                    return ggg.getEnvelope(crs);
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            } else {
                return ggg.getEnvelope();
            }
        }
        LOGGER.log(Level.WARNING, "Unable to get a GridGeometry for coverage data:{0}", name);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getImageFormat() {
        Object r = origin;
        if (r instanceof StoreResource sr) {
            r = sr.getOriginator();
        }
        if (r instanceof WorldFileStore wfs) {
            String[] names = wfs.getImageFormat(true);
            if (names.length != 0) {
                return Optional.of(names[0]);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpatialMetadata getSpatialMetadata() throws ConstellationStoreException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SampleDimension> getSampleDimensions() throws ConstellationStoreException {
        try {
            return origin.getSampleDimensions();
        } catch (CancellationException | DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public GridGeometry getGeometry() throws ConstellationStoreException {
        try {
            return origin.getGridGeometry();
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
    public CoverageDataDescription getDataDescription(StatInfo statInfo, Envelope env) throws ConstellationStoreException {
        final CoverageDataDescription description = new CoverageDataDescription();
        if (statInfo != null) {
            ImageStatistics stats = StatsUtilities.getDataStatistics(statInfo).orElse(null);
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
        if (env == null) {
            env = getEnvelope();
        }
        DataProviders.fillGeographicDescription(env, description);
        return description;
    }

    @Override
    public DataType getDataType() {
        return DataType.COVERAGE;
    }

    @Override
    public Boolean isRendered() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isGeophysic() throws ConstellationStoreException {
        boolean isGeophysic = false;
        try {
            final List<SampleDimension> dims = origin.getSampleDimensions();
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
    public ImageStatistics computeStatistic(int dataId, DataRepository dataRepository) throws ConstellationStoreException {
        // Hack compute statistic from 5% of the first slice
        try {
            GridGeometry gg = getGeometry();
            if (gg.isDefined(GridGeometry.EXTENT)) {
                GridExtent extent = gg.getExtent();
                int[] subSample = new int[extent.getDimension()];
                subSample[0] = Math.round(extent.getSize(0) * 0.05f);
                subSample[1] = Math.round(extent.getSize(1) * 0.05f);
                for (int i = 2; i < extent.getDimension(); i++) {
                    subSample[i] = Math.toIntExact(extent.getSize(i));
                }
                gg = gg.derive().subgrid(null, subSample).build();

            } else if (gg.isDefined(GridGeometry.ENVELOPE)) {
                final Envelope env = gg.getEnvelope();

                // find horizontal crs and it's index.
                final List<SingleCRS> areaCrsComponents = CRS.getSingleComponents(env.getCoordinateReferenceSystem());
                int areaHorizontalIndex = 0;
                int areaHorizontalOffset = 0;
                for (int n=areaCrsComponents.size(); areaHorizontalIndex < n; areaHorizontalIndex++) {
                    SingleCRS areaCmpCrs = areaCrsComponents.get(areaHorizontalIndex);
                    if (CRS.isHorizontalCRS(areaCmpCrs)) {
                        break;
                    }
                    areaHorizontalOffset += areaCmpCrs.getCoordinateSystem().getDimension();
                }

                final long[] low = new long[env.getDimension()];
                final long[] high = new long[env.getDimension()];
                for (int i=0;i<high.length;i++) {
                    if (i == areaHorizontalOffset || i == areaHorizontalOffset+1) {
                        //horizontal crs
                        high[i] = 1000;
                    } else {
                        //make a single slice
                        high[i] = 1;
                    }
                }
                gg = new GridGeometry(new GridExtent(null, low, high, true), env, GridOrientation.HOMOTHETY);
            }

            final GridCoverage cov = origin.read(gg);
            final org.geotoolkit.process.Process process = new Statistics(cov, false);
            if (dataRepository != null) {
                process.addListener(new DataStatisticsListener(dataId, dataRepository));
            }
            final Parameters out = Parameters.castOrWrap(process.call());
            return out.getMandatoryValue(StatisticsDescriptor.OUTCOVERAGE);
        } catch(Exception ex) {
            throw new ConstellationStoreException("Statistics computing failed", ex);
        }
    }
}
