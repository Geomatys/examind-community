package com.examind.provider.computed;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.aggregate.CoverageAggregator;
import org.apache.sis.storage.aggregate.MergeStrategy;
import org.apache.sis.util.iso.Names;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DefaultCoverageData;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceProcessor;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

public class CoverageTimeSeriesProvider extends ComputedResourceProvider {

    private final Specification spec;

    //Origin date in Tropical Year System
    private final static Instant TROPICAL_YEAR_ORIGIN = Instant.parse("2000-01-01T00:00:00Z");

    //Duration of a year in Tropical Year System in number of seconds
    private final static double YEAR_SECONDS_DURATION = 31556925445.0;

    public CoverageTimeSeriesProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId, service, param);
        spec = validate(param);
    }

    private Specification validate(ParameterValueGroup param) {
        var name = getDataName().orElse("coverage-time-series");
        return new Specification(name, getSourceDataIds());
    }

    private static GridGeometry addTime(GridGeometry source, Instant start, Duration timeSpan) {
        if (!source.isDefined(GridGeometry.CRS + GridGeometry.EXTENT + GridGeometry.GRID_TO_CRS)) {
            throw new IllegalArgumentException("Input grid geometry is incomplete");
        }

        long timeCell = Period.between(TROPICAL_YEAR_ORIGIN.atOffset(ZoneOffset.UTC).toLocalDate(), start.atOffset(ZoneOffset.UTC).toLocalDate()).getYears();

        final GridExtent ext = source.getExtent();
        var expandedExtent = ext.insertDimension(ext.getDimension(), DimensionNameType.TIME, timeCell, timeCell, true);
        final int targetDim = expandedExtent.getDimension();
        var timeConversionMatrix = Matrices.createIdentity(targetDim + 1);
        // time offset
        timeConversionMatrix.setElement(targetDim - 1, targetDim, TROPICAL_YEAR_ORIGIN.toEpochMilli());
        // time scale
        timeConversionMatrix.setElement(targetDim - 1, targetDim -1, YEAR_SECONDS_DURATION);
        var expandedGrid2Crs = MathTransforms.concatenate(
                MathTransforms.linear(timeConversionMatrix),
                MathTransforms.passThrough(0, source.getGridToCRS(PixelInCell.CELL_CORNER), 1)
        );

        CoordinateReferenceSystem expandedCrs;
        try {
            expandedCrs = CRS.compound(source.getCoordinateReferenceSystem(), CommonCRS.Temporal.JAVA.crs());
        } catch (FactoryException e) {
            throw new IllegalStateException("Cannot add time dimension to data CRS", e);
        }
        return new GridGeometry(expandedExtent, PixelInCell.CELL_CORNER, expandedGrid2Crs, expandedCrs);
    }

    private static OffsetDateTime getStartDate(final GridCoverageResource input) {
        try {
            return input.getIdentifier()
                    .map(GenericName::toString)
                    .map(CoverageTimeSeriesProvider::extractYear)
                    .map(year -> LocalDate.of(year, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC))
                    .orElseThrow(() -> new IllegalArgumentException("No year found in data name"));
        } catch (DataStoreException e) {
            throw new IllegalArgumentException("Cannot get source data name", e);
        }
    }

    private static int extractYear(String s) {
        final Matcher matcher = YEAR_REGEX.matcher(s);
        if (matcher.find()) {
            var year = Integer.parseInt(matcher.group(1));
            if (matcher.find()) throw new IllegalArgumentException("Name ambiguity: multiple potential years in name (4 consecutive digits)");
            return year;
        }
        throw new IllegalArgumentException("No year found in data name");
    }

    private static final Pattern YEAR_REGEX = Pattern.compile("_(\\d{4})_");

    public record Specification(String name, List<Integer> dataIds) {}

    @Override
    protected Data computeData() {
        var aggregate = compute(spec);
        return new DefaultCoverageData(Names.createLocalName(null, null, spec.name()), aggregate, null);
    }

    private static GridCoverageResource compute(Specification spec) {
        final CoverageAggregator agg = new CoverageAggregator();
        // agg.setMergeStrategy(MergeStrategy.selectByTimeThenArea(Duration.ofDays(365)));
        for (Integer dataId : spec.dataIds()) {
            try {
                if (!(DataProviders.getProviderData(dataId).getOrigin() instanceof GridCoverageResource r)) {
                    throw new IllegalArgumentException("Input data is not an image/coverage");
                }
                final OffsetDateTime startDate = getStartDate(r);
                var target = addTime(r.getGridGeometry(), startDate.toInstant(), Duration.between(startDate, startDate.plusYears(1)));
                r = new ResourceProcessor().resample(r, target, null);
                agg.add(r);
            } catch (DataStoreException e) {
                throw new IllegalArgumentException("Cannot add data in aggregate", e);
            } catch (ConfigurationException e) {
                throw new IllegalArgumentException("Cannot acquire data", e);
            }
        }

        try {
            final Resource result = agg.build(Names.createLocalName(null, null, spec.name()));
            final Collection<GridCoverageResource> candidates = DataStores.flatten(result, true, GridCoverageResource.class);
            final Iterator<GridCoverageResource> it = candidates.iterator();
            if (!it.hasNext()) throw new IllegalStateException("Temporal aggregate is not working");
            return it.next();
        } catch (DataStoreException e) {
            throw new IllegalStateException("Cannot find a raster resource in built aggregate");
        }
    }
}
