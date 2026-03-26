package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for writting NetCDF
 *
 * @author Quentin Bialota (Geomatys)
 */
public final class NetCdfUtils {
    /**
     * Axis role deduced from CRS inspection
     */
    private enum AxisRole { LON, LAT, TIME, VERTICAL, OTHER }

    /**
     * Writes a {@link GridCoverage} to a CF-1.8 compliant NetCDF-4 file using the UCAR library.
     *
     * <p>The output file will contain:
     * <ul>
     *   <li>Global attributes: {@code Conventions = "CF-1.8"}, {@code history}.</li>
     *   <li>One 1-D coordinate variable per CRS axis (lat, lon, time, depth/height, …)
     *       with CF-standard attributes ({@code standard_name}, {@code units}, {@code axis}).</li>
     *   <li>One data variable per {@link SampleDimension} (band), with dimensions ordered
     *       following CF convention: {@code (time?, z?, lat, lon, …)}</li>
     * </ul>
     *
     * <p><strong>Important:</strong> NetCDF-4 writing requires the native {@code libnetcdf}
     * C library at runtime (loaded via JNI by the UCAR {@code Nc4Iosp}).
     *
     * @param coverage   source coverage — packed (non-converted) values are used
     * @param outputFile target path (created / overwritten)
     * @throws IOException           on I/O error or missing native libnetcdf
     * @throws InvalidRangeException if a UCAR array operation fails
     */
    public static void writeNetcdf(GridCoverage coverage, Path outputFile)
            throws IOException, InvalidRangeException {

        // Always work with packed (non-converted) values to keep the original data type.
        coverage = coverage.forConvertedValues(false);

        final CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        final CoordinateSystem          cs  = crs.getCoordinateSystem();
        final GridGeometry              gg  = coverage.getGridGeometry();
        final GridExtent                ge  = gg.getExtent();
        final GeneralEnvelope           env = new GeneralEnvelope(gg.getEnvelope());
        final RenderedImage             img = coverage.render(null);
        final List<SampleDimension>     sds = coverage.getSampleDimensions();

        final int nDims = cs.getDimension();

        // ---- Classify every CRS axis ----------------------------------------
        final AxisRole[]  roles       = new AxisRole[nDims];
        final String[]    dimNames    = new String[nDims];   // NetCDF dimension/variable names
        final int[]       dimSizes    = new int[nDims];      // number of cells along each axis
        final double[][]  coordValues = new double[nDims][]; // cell-centre coordinate values

        // Identify the temporal CRS once (needed for time→epoch conversion)
        final TemporalCRS temporalCRS       = CRS.getTemporalComponent(crs);
        final DefaultTemporalCRS defTmpCRS  = temporalCRS != null
                ? DefaultTemporalCRS.castOrCopy(temporalCRS) : null;

        for (int i = 0; i < nDims; i++) {
            final CoordinateSystemAxis axis = cs.getAxis(i);
            final AxisDirection        dir  = axis.getDirection();
            final DimensionNameType    dnt  = ge.getAxisType(i).orElse(null);

            // --- classify ---
            if (dir == AxisDirection.EAST || dir == AxisDirection.WEST) {
                roles[i]    = AxisRole.LON;
                dimNames[i] = "lon";
            } else if (dir == AxisDirection.NORTH || dir == AxisDirection.SOUTH) {
                roles[i]    = AxisRole.LAT;
                dimNames[i] = "lat";
            } else if (dnt == DimensionNameType.TIME
                    || dir == AxisDirection.FUTURE
                    || dir == AxisDirection.PAST
                    || axis.getAbbreviation().toLowerCase().contains("t")) {
                roles[i]    = AxisRole.TIME;
                dimNames[i] = "time";
            } else if (dir == AxisDirection.UP || dir == AxisDirection.DOWN
                    || dnt == DimensionNameType.VERTICAL) {
                roles[i]    = AxisRole.VERTICAL;
                dimNames[i] = (dir == AxisDirection.DOWN) ? "depth" : "height";
            } else {
                roles[i]    = AxisRole.OTHER;
                dimNames[i] = "dim_" + i;
            }

            // --- size from GridExtent ---
            dimSizes[i] = (int) ge.getSize(i);

            // --- cell-centre coordinate values ---
            final double min  = env.getMinimum(i);
            final double max  = env.getMaximum(i);
            final int    size = dimSizes[i];
            final double step = (size > 1) ? (max - min) / size : 0.0;

            final double[] vals = new double[size];
            if (roles[i] == AxisRole.LAT) {
                // image row 0 = northernmost cell
                for (int k = 0; k < size; k++) {
                    vals[k] = max - (k + 0.5) * step;
                }
            } else if (roles[i] == AxisRole.TIME && defTmpCRS != null) {
                // Convert CRS temporal values → seconds since Unix epoch
                for (int k = 0; k < size; k++) {
                    double crsVal = min + (k + 0.5) * step;
                    Instant instant = defTmpCRS.toInstant(crsVal);
                    vals[k] = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
                }
            } else {
                for (int k = 0; k < size; k++) {
                    vals[k] = min + (k + 0.5) * step;
                }
            }
            coordValues[i] = vals;
        }

        // ---- Data type from image sample model -------------------------------
        final DataType ncDataType = toNcDataType(img.getSampleModel().getDataType());

        // ---- Build CF dimension order for data variables ---------------------
        // CF convention: T, Z, Y, X  (then any "other" axes appended)
        final int[] cfOrder = buildCfDimOrder(roles, nDims);

        // ---- Open / write the NetCDF-4 file ----------------------------------
        NetcdfFormatWriter.Builder writerBuilder =
                NetcdfFormatWriter.createNewNetcdf3(outputFile.toString());

        // -- Global attributes --
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.8"));
        writerBuilder.addAttribute(new Attribute("history",
                Instant.now() + " - Written by Examind using UCAR cdm-core"));

        // -- Dimensions --
        final List<Dimension> dimensions = new ArrayList<>(nDims);
        for (int i = 0; i < nDims; i++) {
            dimensions.add(writerBuilder.addDimension(dimNames[i], dimSizes[i]));
        }

        // -- Coordinate variables (one per axis) --
        final List<Variable.Builder<?>> coordVarBuilders = new ArrayList<>(nDims);
        for (int i = 0; i < nDims; i++) {
            final DataType type = roles[i] == AxisRole.TIME ? DataType.DOUBLE : DataType.DOUBLE;
            final Variable.Builder<?> vb = writerBuilder.addVariable(dimNames[i], type, List.of(dimensions.get(i)));

            addCfCoordAttributes(vb, roles[i], cs.getAxis(i), crs);
            coordVarBuilders.add(vb);
        }

        // -- Data variables (one per SampleDimension / band) --
        final List<Dimension> dataDimsOrdered = new ArrayList<>(nDims);
        for (int idx : cfOrder) {
            dataDimsOrdered.add(dimensions.get(idx));
        }

        final List<Variable.Builder<?>> dataVarBuilders = new ArrayList<>(sds.size());
        final String[] dataVarNames = new String[sds.size()];
        for (int b = 0; b < sds.size(); b++) {
            final SampleDimension sd = sds.get(b);
            final String rawName = (sd.getName() != null)
                    ? sd.getName().tip().toString()
                    : "band_" + b;
            final String varName = NetcdfFileFormat.makeValidNetcdfObjectName(
                    rawName.replaceAll("[^A-Za-z0-9_]", "_"));
            dataVarNames[b] = varName;

            final Variable.Builder<?> vb = writerBuilder.addVariable(varName, ncDataType, dataDimsOrdered);

            sd.getBackground().ifPresent(bg ->
                    vb.addAttribute(new Attribute("_FillValue",
                            ncDataType == DataType.FLOAT || ncDataType == DataType.DOUBLE
                                    ? bg.doubleValue() : bg.longValue())));

            // CF requires: long_name and/or standard_name on data variables
            vb.addAttribute(new Attribute("long_name", rawName));

            dataVarBuilders.add(vb);
        }

        // ---- BUILD THE WRITER (creates the file format) ----
        try (NetcdfFormatWriter writer = writerBuilder.build()) {


            // -- Write coordinate data --
            for (int i = 0; i < nDims; i++) {
                final Variable cv  = writer.findVariable(dimNames[i]);
                final Array    arr = Array.makeFromJavaArray(coordValues[i]);
                writer.write(cv, arr);
            }

            // -- Write band data --
            final java.awt.image.Raster raster  = img.getData();
            final int                   originX = img.getMinX();
            final int                   originY = img.getMinY();

            // Locate the x/y axis indices (used to drive pixel loops)
            int xIdx = -1, yIdx = -1;
            for (int i = 0; i < nDims; i++) {
                if (roles[i] == AxisRole.LON) xIdx = i;
                if (roles[i] == AxisRole.LAT) yIdx = i;
            }
            if (xIdx < 0 || yIdx < 0) {
                throw new IOException("Cannot locate horizontal (lon/lat) axes in the coverage CRS");
            }

            final int width  = dimSizes[xIdx];
            final int height = dimSizes[yIdx];

            for (int b = 0; b < sds.size(); b++) {
                final Variable v     = writer.findVariable(dataVarNames[b]);
                final int[]    shape = computeShape(cfOrder, dimSizes);
                final Array    data  = Array.factory(ncDataType, shape);

                // Build a flat index over the ordered (CF) dimensions.
                // For axes that are not x or y we iterate over size 1 or their actual size.
                writePixelData(data, raster, originX, originY, width, height,
                        b, cfOrder, dimSizes, xIdx, yIdx, ncDataType);

                writer.write(v, data);
            }
        }
    }

    /**
     * Returns the dimension indices reordered following CF convention: T, Z, Y, X, others.
     */
    private static int[] buildCfDimOrder(AxisRole[] roles, int nDims) {
        final List<Integer> tDims     = new ArrayList<>();
        final List<Integer> zDims     = new ArrayList<>();
        final List<Integer> yDims     = new ArrayList<>();
        final List<Integer> xDims     = new ArrayList<>();
        final List<Integer> otherDims = new ArrayList<>();

        for (int i = 0; i < nDims; i++) {
            switch (roles[i]) {
                case TIME     -> tDims.add(i);
                case VERTICAL -> zDims.add(i);
                case LAT      -> yDims.add(i);
                case LON      -> xDims.add(i);
                default       -> otherDims.add(i);
            }
        }

        final List<Integer> ordered = new ArrayList<>();
        ordered.addAll(tDims);
        ordered.addAll(zDims);
        ordered.addAll(yDims);
        ordered.addAll(xDims);
        ordered.addAll(otherDims);

        return ordered.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Computes the shape array for an N-D UCAR array given the CF-ordered dimension indices.
     */
    private static int[] computeShape(int[] cfOrder, int[] dimSizes) {
        final int[] shape = new int[cfOrder.length];
        for (int i = 0; i < cfOrder.length; i++) {
            shape[i] = dimSizes[cfOrder[i]];
        }
        return shape;
    }

    /**
     * Fills the flat UCAR {@link Array} with pixel data from the raster.
     *
     * <p>The array is shaped according to {@code cfOrder}. This method handles the general
     * N-dimensional case by treating all non-spatial axes as having a single "slice" at index 0
     * (the coverage has already been subsetted to the requested extent).
     */
    private static void writePixelData(Array data,
                                       java.awt.image.Raster raster,
                                       int originX, int originY,
                                       int width, int height,
                                       int band,
                                       int[] cfOrder, int[] dimSizes,
                                       int xIdx, int yIdx,
                                       DataType ncDataType) {

        // Locate the position of the X and Y axes within cfOrder
        int cfXPos = -1, cfYPos = -1;
        for (int p = 0; p < cfOrder.length; p++) {
            if (cfOrder[p] == xIdx) cfXPos = p;
            if (cfOrder[p] == yIdx) cfYPos = p;
        }

        // Strides in the flat 1-D array
        final int nDims   = cfOrder.length;
        final int[] shape = computeShape(cfOrder, dimSizes);

        // Precompute strides (row-major)
        final long[] strides = new long[nDims];
        strides[nDims - 1] = 1;
        for (int p = nDims - 2; p >= 0; p--) {
            strides[p] = strides[p + 1] * shape[p + 1];
        }

        // Iterate: for non-spatial dims, size is ≥ 1 (coverage already sliced).
        // We stride through all combinations to fill the flat array.
        final int[] idx = new int[nDims]; // current multi-index in CF order
        final long totalNonSpatial = data.getSize() / ((long) width * height);

        // Outer loop: all non-spatial positions (time, z, other)
        for (long ns = 0; ns < totalNonSpatial; ns++) {
            // Decode ns into non-spatial multi-index
            long rem = ns;
            for (int p = 0; p < nDims; p++) {
                if (p == cfXPos || p == cfYPos) {
                    idx[p] = 0; // will be overridden in inner loops
                    continue;
                }
                idx[p] = (int) (rem / nonSpatialStride(p, cfXPos, cfYPos, shape));
                rem    = rem  %  nonSpatialStride(p, cfXPos, cfYPos, shape);
            }

            for (int row = 0; row < height; row++) {
                idx[cfYPos] = row;
                for (int col = 0; col < width; col++) {
                    idx[cfXPos] = col;

                    // Compute flat offset
                    long flat = 0;
                    for (int p = 0; p < nDims; p++) {
                        flat += (long) idx[p] * strides[p];
                    }

                    final int px = originX + col;
                    final int py = originY + row;

                    switch (ncDataType) {
                        case BYTE:
                            data.setByte  ((int) flat, (byte) raster.getSample      (px, py, band)); break;
                        case SHORT:
                            data.setShort ((int) flat, (short) raster.getSample     (px, py, band)); break;
                        case INT:
                            data.setInt   ((int) flat,          raster.getSample     (px, py, band)); break;
                        case FLOAT:
                            data.setFloat ((int) flat,          raster.getSampleFloat (px, py, band)); break;
                        case DOUBLE:
                            data.setDouble((int) flat,          raster.getSampleDouble(px, py, band)); break;
                        default:
                            data.setFloat ((int) flat,          raster.getSampleFloat (px, py, band)); break;
                    }
                }
            }
        }
    }

    /**
     * Computes the stride for iterating over non-spatial axis {@code p} within the CF-ordered
     * shape, skipping the X and Y positions.
     */
    private static long nonSpatialStride(int p, int cfXPos, int cfYPos, int[] shape) {
        long s = 1;
        for (int q = p + 1; q < shape.length; q++) {
            if (q == cfXPos || q == cfYPos) continue;
            s *= shape[q];
        }
        return Math.max(s, 1);
    }

    /**
     * Attaches CF-1.8 coordinate attributes to a variable builder.
     *
     * @param vb   the variable builder to annotate
     * @param role the classified axis role
     * @param axis the CRS axis (unit string retrieved from here)
     * @param crs  the full CRS (unused currently, reserved for future grid_mapping)
     */
    private static void addCfCoordAttributes(Variable.Builder<?> vb,
                                              AxisRole role,
                                              CoordinateSystemAxis axis,
                                              CoordinateReferenceSystem crs) {
        switch (role) {
            case LON -> {
                vb.addAttribute(new Attribute("standard_name", "longitude"));
                vb.addAttribute(new Attribute("long_name",     "longitude"));
                vb.addAttribute(new Attribute("units",         "degrees_east"));
                vb.addAttribute(new Attribute("axis",          "X"));
            }
            case LAT -> {
                vb.addAttribute(new Attribute("standard_name", "latitude"));
                vb.addAttribute(new Attribute("long_name",     "latitude"));
                vb.addAttribute(new Attribute("units",         "degrees_north"));
                vb.addAttribute(new Attribute("axis",          "Y"));
            }
            case TIME -> {
                vb.addAttribute(new Attribute("standard_name", "time"));
                vb.addAttribute(new Attribute("long_name",     "time"));
                vb.addAttribute(new Attribute("units",         "seconds since 1970-01-01T00:00:00Z"));
                vb.addAttribute(new Attribute("calendar",      "gregorian"));
                vb.addAttribute(new Attribute("axis",          "T"));
            }
            case VERTICAL -> {
                final String unitStr = axis.getUnit() != null ? axis.getUnit().toString() : "m";
                final boolean isDown = axis.getDirection() == AxisDirection.DOWN;
                vb.addAttribute(new Attribute("standard_name", isDown ? "depth" : "height"));
                vb.addAttribute(new Attribute("long_name",     isDown ? "depth" : "height"));
                vb.addAttribute(new Attribute("units",         unitStr));
                vb.addAttribute(new Attribute("positive",      isDown ? "down" : "up"));
                vb.addAttribute(new Attribute("axis",          "Z"));
            }
            default -> {
                // Generic: at least give a long_name from the CRS axis name
                vb.addAttribute(new Attribute("long_name", axis.getName().getCode()));
            }
        }
    }

    /**
     * Maps a {@link DataBuffer} type constant to the corresponding UCAR {@link DataType}.
     *
     * @param dataBufferType one of the {@code DataBuffer.TYPE_*} constants
     * @return the closest NetCDF-4 compatible data type (defaults to {@code FLOAT})
     */
    public static DataType toNcDataType(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:   return DataType.BYTE;
            case DataBuffer.TYPE_SHORT:  return DataType.SHORT;
            case DataBuffer.TYPE_USHORT: return DataType.USHORT;
            case DataBuffer.TYPE_INT:    return DataType.INT;
            case DataBuffer.TYPE_FLOAT:  return DataType.FLOAT;
            case DataBuffer.TYPE_DOUBLE: return DataType.DOUBLE;
            default:                     return DataType.FLOAT;
        }
    }
}
