package org.constellation.process.utils.coverage;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.netcdf.NetcdfStore;
import org.constellation.util.CRSUtilities;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for processes
 *
 * @author Quentin Bialota (Geomatys)
 */
public final class Utils {

    /**
     * Format values form temporal extent
     * @param extent String array containing the temporal extent
     * @return formatted temporal extent
     */
    public static String[] formatTemporalExtent(String[] extent) {
        if (extent != null) {
            // Replace "null" by null
            return Arrays.stream(extent)
                    .map(s -> "null".equalsIgnoreCase(s) ? null : s)
                    .toArray(String[]::new);
        }
        return extent;
    }

    /**
     * Get GridCoverageResource from DataStore
     * @param ds source DataStore
     * @param process Process from which these utils are called
     * @return the GridCoverageResource
     * @throws DataStoreException if an error occurs with the DataStore
     * @throws ProcessException If an error occurs
     */
    public static GridCoverageResource getGridCoverageResource(DataStore ds, Process process) throws DataStoreException, ProcessException {
        if (ds instanceof GeoTiffStore geoTiffStore) {
            // Example: read first coverage from GeoTIFF
            return geoTiffStore.components().getFirst();

        } else if (ds instanceof NetcdfStore netcdfStore) {
            // Example: read first coverage from NetCDF
            Resource resource = netcdfStore.components().iterator().next();
            if (!(resource instanceof GridCoverageResource gcrNc)) {
                throw new ProcessException("The NetCDF resource is not a GridCoverageResource, only support GridCoverage.", process);
            }
            return gcrNc;

        } else {
            throw new ProcessException("Unsupported DataStore type: " + ds.getClass().getName(), process);
        }
    }

    /**
     * Get source Envelope from GridCoverageResource
     * @param gcr source GridCoverageResource
     * @param process Process from which these utils are called
     * @return the Envelope
     * @throws DataStoreException if an error occurs with the DataStore
     * @throws ProcessException If an error occurs
     */
    public static Envelope getSourceEnvelope(GridCoverageResource gcr, Process process) throws DataStoreException, ProcessException {
        try {
            return gcr.getEnvelope().orElse(new GeneralEnvelope(CRS.forCode("urn:ogc:def:crs:OGC:2:84")));
        } catch (FactoryException ex) {
            throw new ProcessException("CRS doesn't exist : urn:ogc:def:crs:OGC:2:84", process, ex);
        }
    }

    /**
     * Generate Result GridGeometry
     * @param sourceEnvelope Envelope of source coverage
     * @param sourceGeometry Geometry of source coverage
     * @param spatialExtent Spatial Extent / Bbox wanted
     * @param temporalExtent Temporal Extent wanted
     * @param process Process from which these utils are called
     * @return a GridGeometry
     */
    public static GridGeometry generateResultGridGeometry(Envelope sourceEnvelope, GridGeometry sourceGeometry,
                                                          Envelope spatialExtent, String[] temporalExtent, Process process) throws ProcessException {

        CoordinateReferenceSystem crs = sourceEnvelope.getCoordinateReferenceSystem();
        SingleCRS horizontal = CRS.getHorizontalComponent(crs);

        //SPATIAL EXTENT
        GeneralEnvelope envelope = new GeneralEnvelope(sourceEnvelope);
        if (spatialExtent != null) {
            GeneralEnvelope bbox = new GeneralEnvelope(spatialExtent);

            // Change CRS if bbox CRS and data CRS are not the same
            if (horizontal != CRS.getHorizontalComponent(bbox.getCoordinateReferenceSystem())) {
                try {
                    bbox = CRSUtilities.reprojectWithNoInfinity(bbox, horizontal);
                } catch (TransformException ex) {
                    throw new ProcessException("Error while reprojecting bbox CRS to data CRS.", process, ex);
                }
            }

            // Get the dimensions for the horizontal part (bbox will only be horizontal)
            int dimensions = Math.min(bbox.getDimension(), sourceEnvelope.getDimension());
            for (int i = 0; i < dimensions; i++) {
                // For each axis, get the "best" extent
                // If bbox is smaller than data => bbox
                // If data is smaller than bbox => data
                double min = Math.max(bbox.getMinimum(i), sourceEnvelope.getMinimum(i));
                double max = Math.min(bbox.getMaximum(i), sourceEnvelope.getMaximum(i));
                envelope.setRange(i, min, max);
            }
        }

        //TEMPORAL EXTENT
        if (temporalExtent != null && temporalExtent.length > 0) {
            int dimIdx = 0;
            int timeDimensionId = -1;
            List<SingleCRS> crsList = CRS.getSingleComponents(crs);
            for (SingleCRS singleCRS : crsList) {
                for (int crsDim = 0; crsDim < singleCRS.getCoordinateSystem().getDimension(); crsDim++) {
                    CoordinateSystemAxis csa = singleCRS.getCoordinateSystem().getAxis(crsDim);
                    String abbreviation = csa.getAbbreviation().toLowerCase();
                    AxisDirection axisDirection = csa.getDirection();
                    DimensionNameType axisType = sourceGeometry.getExtent().getAxisType(dimIdx).orElse(null);

                    if (axisType == DimensionNameType.TIME || axisDirection == AxisDirection.FUTURE || abbreviation.equals("time")) {
                        timeDimensionId = dimIdx;
                    }
                    dimIdx++;
                }
            }

            if (timeDimensionId > -1) {
                TemporalCRS temporalCRS = CRS.getTemporalComponent(envelope.getCoordinateReferenceSystem());
                if (temporalCRS == null) {
                    throw new ProcessException("No temporal CRS found for axis : " + timeDimensionId, process);
                }
                DefaultTemporalCRS defaultTemporalCRS = DefaultTemporalCRS.castOrCopy(temporalCRS);

                double minVal = sourceEnvelope.getMinimum(timeDimensionId);
                double maxVal = sourceEnvelope.getMaximum(timeDimensionId);

                double firstValue = 0.0;
                double secondValue = 0.0;
                if (temporalExtent.length == 1) { //In case of slice
                    if (temporalExtent[0] == null) {
                        throw new ProcessException("Temporal extent for a slice cannot be null", process);
                    }
                    Instant datetime = Instant.parse(temporalExtent[0]);
                    firstValue = defaultTemporalCRS.toValue(datetime);
                    secondValue = firstValue;
                } else if (temporalExtent.length == 2) { //In case of subset
                    if (temporalExtent[0] != null) {
                        Instant datetime = Instant.parse(temporalExtent[0]);
                        firstValue = defaultTemporalCRS.toValue(datetime);
                    } else {
                        firstValue = minVal;
                    }

                    if (temporalExtent[1] != null) {
                        Instant datetime = Instant.parse(temporalExtent[1]);
                        secondValue = defaultTemporalCRS.toValue(datetime);
                    } else {
                        secondValue = maxVal;
                    }
                }

                double finalFirstValue = Math.max(firstValue, minVal);
                double finalSecondValue = Math.min(secondValue, maxVal);

                if (finalFirstValue > finalSecondValue) {
                    throw new ProcessException("Subsetting temporal params do not overlap the source data envelope extent", process);
                }

                envelope.setRange(timeDimensionId, finalFirstValue, finalSecondValue);
            }
        }

        return sourceGeometry.derive().subgrid(envelope).build();
        // return new ResourceProcessor().resample(gcr, gridGeometry, null);
    }

    /**
     * Select Bands in a GridCoverage
     * @param bands list of bands as String
     * @param gridCoverage source GridCoverage
     * @return result GridCoverage (we return source gridCoverage if no bands are given)
     */
    public static GridCoverage selectBands(String[] bands, GridCoverage gridCoverage) {
        if (bands != null && bands.length > 0) {
            List<Integer> ids = new ArrayList<>();
            int i = 0;
            for(SampleDimension sm : gridCoverage.getSampleDimensions()) {
                if(ArrayUtils.contains(bands, sm.getName().toString())) {
                    ids.add(i);
                }
                i++;
            }

            // In case where properties are directly bands id
            for(String band : bands) {
                try {
                    int p = Integer.parseInt(band);
                    ids.add(p);
                } catch (NumberFormatException ex) {
                    // Do Nothing
                }
            }

            int[] bandsInt = ArrayUtils.toPrimitive(ids.toArray(new Integer[0]));
            GridCoverageProcessor processor = new GridCoverageProcessor();
            return processor.selectSampleDimensions(gridCoverage, bandsInt);
        }
        // No band specified, we return the source GridCoverage
        return gridCoverage;
    }
}
