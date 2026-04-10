package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
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
import java.util.Arrays;
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
        final List<SampleDimension>     sds = coverage.getSampleDimensions();

        final int nCrsDims  = cs.getDimension();
        final int nGridDims = ge.getDimension();

        // ---- Detect phantom CRS dimensions -----------------------------------
        // A CRS dimension is "phantom" when its row in the gridToCRS matrix has
        // all-zero coefficients in the grid-dimension columns (it is a constant
        // value that does not vary with any grid cell — e.g. ECMWF's "expver").
        // Such axes carry no spatial/temporal information and must be skipped so
        // that the NetCDF writer does not try to call ge.getSize(i) for an index
        // that does not exist in the GridExtent.
        //
        // crsToGrid[crsIdx] = corresponding grid dimension index, or -1 if phantom.
        final int[] crsToGrid = new int[nCrsDims];
        Arrays.fill(crsToGrid, -1);

        final MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
        if (gridToCRS instanceof LinearTransform lt) {
            final Matrix matrix = lt.getMatrix();
            for (int crsIdx = 0; crsIdx < nCrsDims; crsIdx++) {
                for (int gridIdx = 0; gridIdx < nGridDims; gridIdx++) {
                    if (matrix.getElement(crsIdx, gridIdx) != 0.0) {
                        crsToGrid[crsIdx] = gridIdx;
                        break;
                    }
                }
            }
        } else {
            // Non-linear transform: assume 1-1 mapping up to the grid dimension count.
            for (int i = 0; i < Math.min(nCrsDims, nGridDims); i++) {
                crsToGrid[i] = i;
            }
        }

        // activeCrsDims[i] = CRS dimension index for the i-th NetCDF dimension
        // (phantom CRS dims are excluded).
        final int[] activeCrsDims;
        {
            int count = 0;
            for (int v : crsToGrid) if (v >= 0) count++;
            activeCrsDims = new int[count];
            int k = 0;
            for (int i = 0; i < nCrsDims; i++) {
                if (crsToGrid[i] >= 0) activeCrsDims[k++] = i;
            }
        }
        final int nDims = activeCrsDims.length;

        // ---- Classify every active CRS axis ----------------------------------
        final AxisRole[]  roles       = new AxisRole[nDims];
        final String[]    dimNames    = new String[nDims];   // NetCDF dimension/variable names
        final int[]       dimSizes    = new int[nDims];      // number of cells along each axis
        final double[][]  coordValues = new double[nDims][]; // cell-centre coordinate values

        // Identify the temporal CRS once (needed for time→epoch conversion)
        final TemporalCRS temporalCRS       = CRS.getTemporalComponent(crs);
        final DefaultTemporalCRS defTmpCRS  = temporalCRS != null
                ? DefaultTemporalCRS.castOrCopy(temporalCRS) : null;

        for (int i = 0; i < nDims; i++) {
            final int crsIdx  = activeCrsDims[i];
            final int gridIdx = crsToGrid[crsIdx];

            final CoordinateSystemAxis axis = cs.getAxis(crsIdx);
            final AxisDirection        dir  = axis.getDirection();
            final DimensionNameType    dnt  = ge.getAxisType(gridIdx).orElse(null);

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

            // --- size from GridExtent (grid dimension index) ---
            dimSizes[i] = (int) ge.getSize(gridIdx);

            // --- cell-centre coordinate values (envelope index = CRS index) ---
            final double min  = env.getMinimum(crsIdx);
            final double max  = env.getMaximum(crsIdx);
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

        // ---- Find spatial axes -----------------------------------------------
        int xIdx = -1, yIdx = -1;
        for (int i = 0; i < nDims; i++) {
            if (roles[i] == AxisRole.LON) xIdx = i;
            if (roles[i] == AxisRole.LAT) yIdx = i;
        }
        if (xIdx < 0 || yIdx < 0) {
            throw new IOException("Cannot locate horizontal (lon/lat) axes in the coverage CRS");
        }
        // Grid dimension indices for the two spatial axes (needed to build slice extents)
        final int gridXIdx = crsToGrid[activeCrsDims[xIdx]];
        final int gridYIdx = crsToGrid[activeCrsDims[yIdx]];

        // ---- Non-spatial dimensions ------------------------------------------
        // Each non-spatial dimension requires its own render() call because
        // coverage.render(null) only works when all extra dimensions are already
        // reduced to a single cell.
        final List<Integer> nonSpatialPositions = new ArrayList<>();
        for (int i = 0; i < nDims; i++) {
            if (i != xIdx && i != yIdx) nonSpatialPositions.add(i);
        }
        int totalSlices = 1;
        for (int pos : nonSpatialPositions) totalSlices *= dimSizes[pos];

        // ---- Data type from first slice --------------------------------------
        final DataType ncDataType = toNcDataType(
                renderSlice(coverage, ge, nGridDims, gridXIdx, gridYIdx,
                        nonSpatialPositions, activeCrsDims, crsToGrid, dimSizes,
                        new int[nonSpatialPositions.size()])
                        .getSampleModel().getDataType());

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
            final Variable.Builder<?> vb = writerBuilder.addVariable(dimNames[i], DataType.DOUBLE, List.of(dimensions.get(i)));
            addCfCoordAttributes(vb, roles[i], cs.getAxis(activeCrsDims[i]), crs);
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

            // -- Write band data (ND-aware: one render() call per 2D slice) ----
            // Locate X and Y positions in the CF-ordered dimension array
            int cfXPos = -1, cfYPos = -1;
            for (int p = 0; p < cfOrder.length; p++) {
                if (cfOrder[p] == xIdx) cfXPos = p;
                if (cfOrder[p] == yIdx) cfYPos = p;
            }

            final int width  = dimSizes[xIdx];
            final int height = dimSizes[yIdx];

            // Precompute row-major strides over the CF-ordered shape
            final int[] shape   = computeShape(cfOrder, dimSizes);
            final long[] strides = new long[nDims];
            strides[nDims - 1] = 1;
            for (int p = nDims - 2; p >= 0; p--) strides[p] = strides[p + 1] * shape[p + 1];

            for (int b = 0; b < sds.size(); b++) {
                final Variable v    = writer.findVariable(dataVarNames[b]);
                final Array    data = Array.factory(ncDataType, shape);

                // Iterate over every non-spatial slice combination
                for (int sliceFlat = 0; sliceFlat < totalSlices; sliceFlat++) {

                    // Decode sliceFlat → per-non-spatial-dimension indices
                    final int[] nsIndices = new int[nonSpatialPositions.size()];
                    int rem = sliceFlat;
                    for (int k = nonSpatialPositions.size() - 1; k >= 0; k--) {
                        nsIndices[k] = rem % dimSizes[nonSpatialPositions.get(k)];
                        rem          /= dimSizes[nonSpatialPositions.get(k)];
                    }

                    // Render the 2D spatial slice for this non-spatial combination
                    final RenderedImage sliceImg = renderSlice(coverage, ge, nGridDims,
                            gridXIdx, gridYIdx, nonSpatialPositions, activeCrsDims,
                            crsToGrid, dimSizes, nsIndices);
                    final java.awt.image.Raster raster  = sliceImg.getData();
                    final int                   originX = sliceImg.getMinX();
                    final int                   originY = sliceImg.getMinY();

                    // Build the CF-ordered multi-index for this non-spatial slice,
                    // then sweep through the spatial pixels
                    final int[] idx = new int[nDims];
                    for (int k = 0; k < nonSpatialPositions.size(); k++) {
                        final int activeDimPos = nonSpatialPositions.get(k);
                        for (int p = 0; p < cfOrder.length; p++) {
                            if (cfOrder[p] == activeDimPos) { idx[p] = nsIndices[k]; break; }
                        }
                    }

                    for (int row = 0; row < height; row++) {
                        idx[cfYPos] = row;
                        for (int col = 0; col < width; col++) {
                            idx[cfXPos] = col;
                            long flat = 0;
                            for (int p = 0; p < nDims; p++) flat += (long) idx[p] * strides[p];
                            final int px = originX + col;
                            final int py = originY + row;
                            switch (ncDataType) {
                                case BYTE:   data.setByte  ((int) flat, (byte)  raster.getSample      (px, py, b)); break;
                                case SHORT:  data.setShort ((int) flat, (short) raster.getSample      (px, py, b)); break;
                                case INT:    data.setInt   ((int) flat,         raster.getSample      (px, py, b)); break;
                                case FLOAT:  data.setFloat ((int) flat,         raster.getSampleFloat (px, py, b)); break;
                                case DOUBLE: data.setDouble((int) flat,         raster.getSampleDouble(px, py, b)); break;
                                default:     data.setFloat ((int) flat,         raster.getSampleFloat (px, py, b)); break;
                            }
                        }
                    }
                }

                writer.write(v, data);
            }
        }
    }

    /**
     * Renders a 2D spatial slice of the coverage by pinning all non-spatial dimensions
     * to the cell specified by {@code nsIndices}.
     *
     * @param coverage             the N-D coverage to slice
     * @param ge                   the coverage's GridExtent
     * @param nGridDims            total number of grid dimensions
     * @param gridXIdx             grid dimension index of the longitude axis
     * @param gridYIdx             grid dimension index of the latitude axis
     * @param nonSpatialPositions  active-dim positions (0..nDims-1) that are not spatial
     * @param activeCrsDims        mapping from active-dim index to CRS dimension index
     * @param crsToGrid            mapping from CRS dimension index to grid dimension index
     * @param dimSizes             size of each active dimension
     * @param nsIndices            slice index for each non-spatial position
     * @return a 2D RenderedImage for the requested slice
     */
    private static RenderedImage renderSlice(GridCoverage coverage, GridExtent ge,
            int nGridDims, int gridXIdx, int gridYIdx,
            List<Integer> nonSpatialPositions, int[] activeCrsDims, int[] crsToGrid,
            int[] dimSizes, int[] nsIndices) {

        final long[] low  = new long[nGridDims];
        final long[] high = new long[nGridDims];
        for (int i = 0; i < nGridDims; i++) {
            low[i]  = ge.getLow(i);
            high[i] = ge.getHigh(i);
        }
        // Pin each non-spatial dimension to its requested slice cell
        for (int k = 0; k < nonSpatialPositions.size(); k++) {
            final int activeDimPos = nonSpatialPositions.get(k);
            final int gridIdx      = crsToGrid[activeCrsDims[activeDimPos]];
            final long sliceCell   = ge.getLow(gridIdx) + nsIndices[k];
            low[gridIdx]  = sliceCell;
            high[gridIdx] = sliceCell;
        }
        return coverage.render(new GridExtent(null, low, high, true));
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
