package com.examind.community.storage.coverage.aggregation;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.examind.community.storage.coverage.aggregation.GridGeometryUtil.requireCrsMatchWith;

// TODO: remove this once Apache SIS CoverageAggregator works well enough for this use-case.
public class TimeSeries extends DerivedGridCoverageResource {

    private final List<GridCoverageResource> sources;
    private final TimeSeriesSpecification spec;

    public TimeSeries(List<GridCoverageResource> sources, GenericName name) {
        super(name);
        this.sources = sources;
        try {
            this.spec = analyze(sources);
        } catch (DataStoreException e) {
            throw new IllegalArgumentException("Time series aggregation can't be done : error while retrieving GridGeometry ");
        }
    }

    @Override
    public List<GridCoverageResource> sources() {
        return sources;
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return Optional.empty();
    }

    @Override
    public GridGeometry getGridGeometry() {
        return spec.unionGeometry();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sources.get(0).getSampleDimensions();
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        if (domain == null) {
            throw new IllegalArgumentException("Domain cannot be null");
        }

        NumberRange<Double> timeRange = spec.referencing().extractTimeRange(domain);

        List<ResourceTimeSpan> candidates = spec.searchIndex().search(timeRange).toList();
        if (candidates.isEmpty()) {
            throw new NoSuchDataException("No data found for specified time");
        } else if (candidates.size() == 1) {
            return candidates.get(0).source().read(domain, ranges);
        } else {
            Optional<ResourceTimeSpan> chosen = candidates.stream()
                    .max(Comparator.comparingDouble(span ->
                            Math.min(span.max(), timeRange.getMaxValue()) - Math.max(span.min(), timeRange.getMinValue())))
                    .filter(span -> span.max() >= timeRange.getMinValue() && span.min() <= timeRange.getMaxValue());

            if(chosen.isEmpty()){
                throw new NoSuchDataException("No data found for specified time");
            }

            return chosen.get().source().read(domain, ranges);
        }
    }

    public static Envelope union(Envelope e1, Envelope e2) {
        GeneralEnvelope inter = new GeneralEnvelope(e1);
        inter.add(e2);
        return new ImmutableEnvelope(inter);
    }

    public static TimeSeriesSpecification analyze(List<GridCoverageResource> sources) throws DataStoreException {
        if (sources.size() <= 1) {
            throw new IllegalArgumentException("Time series aggregation does not make sense for a single dataset.");
        }

        List<GridGeometry> geoms = new ArrayList<>();
        for (GridCoverageResource source : sources) {
            GridGeometry gridGeometry = source.getGridGeometry();
            geoms.add(gridGeometry);
        }

        for (int i = 0; i < geoms.size() - 1; i++) {
            requireCrsMatchWith(geoms.get(i), geoms.get(i + 1));
        }

        GridGeometry firstGeom = geoms.get(0);
        CoordinateReferenceSystem crs = firstGeom.getCoordinateReferenceSystem();
        TemporalReferencing referencing = extractTemporalReferencing(crs)
                .orElseThrow(() -> new IllegalArgumentException("No temporal CRS found in provided data series"));


        List<ResourceTimeSpan> timeSpans = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            GridCoverageResource source = sources.get(i);
            GridGeometry geom = geoms.get(i);

            double tMin = geom.getEnvelope().getMinimum(referencing.timeIndex());
            double tMax = geom.getEnvelope().getMaximum(referencing.timeIndex());

            timeSpans.add(new ResourceTimeSpan(tMin, tMax, source));
        }
        TimeIndex timeIndex = new TimeIndex(timeSpans.stream().sorted().collect(Collectors.toList()));

        return new TimeSeriesSpecification(geoms.stream().reduce(GridGeometryUtil::mergeWith).get(), referencing, timeIndex);
    }

    private static Optional<TemporalReferencing> extractTemporalReferencing(CoordinateReferenceSystem crs) {
        return Optional.ofNullable(CRS.getTemporalComponent(crs))
                .map(temporalCRS -> {
                    int timeIdx = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), temporalCRS.getCoordinateSystem());
                    if (timeIdx < 0) {
                        throw new IllegalArgumentException("Cannot find back extracted temporal CRS index");
                    }
                    return new TemporalReferencing(crs, timeIdx, temporalCRS);
                });
    }
}

record TimeSeriesSpecification(GridGeometry unionGeometry, TemporalReferencing referencing, TimeIndex searchIndex) {}

class GridGeometryUtil {

    public static GridGeometry mergeWith(GridGeometry geom1, GridGeometry geom2) {
        requireCrsMatchWith(geom1, geom2);

        if (!(isComplete(geom1) && isComplete(geom2))) {
            throw new IllegalArgumentException("Both Grid geometries must be complete to allow merge");
        }

        double[] outputResolution = new double[geom1.getDimension()];
        for (int i = 0; i < geom1.getDimension(); i++) {
            outputResolution[i] = Math.min(geom1.getResolution(true)[i], geom2.getResolution(true)[i]);
        }

        Envelope mergeEnvelope = TimeSeries.union(geom1.getEnvelope(), geom2.getEnvelope());

        long[] gridHigh = new long[mergeEnvelope.getDimension()];
        for (int i = 0; i < mergeEnvelope.getDimension(); i++) {
            double extent = mergeEnvelope.getSpan(i) / outputResolution[i];
            double ceilExtent = Math.ceil(extent);
            if (!Double.isFinite(ceilExtent)) throw new IllegalStateException("Cannot compute Grid size for dimension "+i);
            gridHigh[i] = (long) ceilExtent;

        }

        long[] gridLow = new long[gridHigh.length];

        GridExtent extent = new GridExtent(null, gridLow, gridHigh, false);
        return new GridGeometry(extent, mergeEnvelope, GridOrientation.HOMOTHETY);
    }

    public static void requireCrsMatchWith(GridGeometry geom1, GridGeometry geom2) {
        if (!geom1.getCoordinateReferenceSystem().equals(geom2.getCoordinateReferenceSystem())) {
            throw new IllegalArgumentException("Coordinate Reference Systems must match for grid geometries");
        }
    }

    private static boolean isComplete(GridGeometry geom) {
        return geom.isDefined(GridGeometry.ENVELOPE + GridGeometry.GRID_TO_CRS + GridGeometry.EXTENT);
    }
}

