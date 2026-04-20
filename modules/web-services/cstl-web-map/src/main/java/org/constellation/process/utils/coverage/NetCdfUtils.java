/*
 *    Constellation/Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
import org.opengis.referencing.operation.TransformException;
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
 * Utilities for writing NetCDF. Includes phantom axis detection,
 * non-spatial multidimensional handling, regular grid bounds check,
 * and swath/curvilinear auxiliary coordinate formatting.
 *
 * @author Quentin Bialota (Geomatys)
 */
public final class NetCdfUtils {
    /** Axis role deduced from CRS inspection */
    private enum AxisRole { LON, LAT, TIME, VERTICAL, OTHER }

    /**
     * Writes a {@link GridCoverage} to a CF-1.8 compliant NetCDF file using the UCAR library,
     * and streams the output to the provided {@link java.io.OutputStream}.
     *
     * <p>Note: The NetCDF format requires random-access for writing headers and data chunks.
     * This method writes to a temporary file locally and copies the result to the stream.
     *
     * @param coverage     source coverage
     * @param outputStream the stream to write to
     * @throws IOException           on I/O error
     * @throws InvalidRangeException if a UCAR array operation fails
     */
    public static void writeNetcdf(GridCoverage coverage, java.io.OutputStream outputStream)
            throws IOException, InvalidRangeException {
        final Path tempFile = java.nio.file.Files.createTempFile("examind_netcdf_", ".nc");
        try {
            writeNetcdf(coverage, tempFile);
            java.nio.file.Files.copy(tempFile, outputStream);
            outputStream.flush();
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Writes a {@link GridCoverage} to a CF-1.8 compliant NetCDF file using the UCAR library.
     * <p>Detects phantom dimensions, regular vs. swath grids, and writes everything
     * into a properly formatted structure using NetCDF-3.
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

        MathTransform gridToCRS = null;
        boolean isCurvilinear = false;
        try {
            gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CENTER);
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
                isCurvilinear = true;
                for (int i = 0; i < Math.min(nCrsDims, nGridDims); i++) {
                    crsToGrid[i] = i;
                }
            }
        } catch (Exception ignored) { }

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
        final TemporalCRS temporalCRS      = CRS.getTemporalComponent(crs);
        final DefaultTemporalCRS defTmpCRS = temporalCRS != null
                ? DefaultTemporalCRS.castOrCopy(temporalCRS) : null;

        int xIdx = -1, yIdx = -1;

        for (int i = 0; i < nDims; i++) {
            final int crsIdx  = activeCrsDims[i];
            final int gridIdx = crsToGrid[crsIdx];

            final CoordinateSystemAxis axis = cs.getAxis(crsIdx);
            final AxisDirection        dir  = axis.getDirection();
            final DimensionNameType    dnt  = ge.getAxisType(gridIdx).orElse(null);

            if (dir == AxisDirection.EAST || dir == AxisDirection.WEST) {
                roles[i] = AxisRole.LON; dimNames[i] = "lon"; xIdx = i;
            } else if (dir == AxisDirection.NORTH || dir == AxisDirection.SOUTH) {
                roles[i] = AxisRole.LAT; dimNames[i] = "lat"; yIdx = i;
            } else if (dnt == DimensionNameType.TIME || dir == AxisDirection.FUTURE || dir == AxisDirection.PAST || axis.getAbbreviation().toLowerCase().contains("t")) {
                roles[i] = AxisRole.TIME; dimNames[i] = "time";
            } else if (dir == AxisDirection.UP || dir == AxisDirection.DOWN || dnt == DimensionNameType.VERTICAL) {
                roles[i] = AxisRole.VERTICAL; dimNames[i] = (dir == AxisDirection.DOWN) ? "depth" : "height";
            } else {
                roles[i] = AxisRole.OTHER; dimNames[i] = "dim_" + i;
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
                final boolean rowZeroIsNorth = isRowZeroNorth(gg, crsIdx, size, nCrsDims);
                for (int k = 0; k < size; k++) {
                    vals[k] = rowZeroIsNorth ? max - (k + 0.5) * step : min + (k + 0.5) * step;
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

        if (xIdx < 0 || yIdx < 0) {
            throw new IOException("Cannot locate horizontal (lon/lat) axes in the coverage CRS");
        }

        final int crsXIdx  = activeCrsDims[xIdx];
        final int crsYIdx  = activeCrsDims[yIdx];
        final int gridXIdx = crsToGrid[crsXIdx];
        final int gridYIdx = crsToGrid[crsYIdx];
        final int width    = dimSizes[xIdx];
        final int height   = dimSizes[yIdx];

        // Create the non-spatial iterations configuration
        final List<Integer> nonSpatialPositions = new ArrayList<>();
        for (int i = 0; i < nDims; i++) {
            if (i != xIdx && i != yIdx) nonSpatialPositions.add(i);
        }
        int totalSlices = 1;
        for (int pos : nonSpatialPositions) totalSlices *= dimSizes[pos];

        final RenderedImage firstSlice = renderSlice(coverage, ge, nGridDims, gridXIdx, gridYIdx,
                nonSpatialPositions, activeCrsDims, crsToGrid, dimSizes, new int[nonSpatialPositions.size()]);
        final DataType ncDataType = toNcDataType(firstSlice.getSampleModel().getDataType());

        if (isCurvilinear && gridToCRS != null) {
            writeCurvilinear(outputFile, gridToCRS, crs, cs, ge, sds, nDims, nCrsDims, activeCrsDims, roles,
                    dimNames, dimSizes, coordValues, xIdx, yIdx, gridXIdx, gridYIdx, crsXIdx, crsYIdx, width, height,
                    nonSpatialPositions, totalSlices, ncDataType, coverage, nGridDims, crsToGrid);
        } else {
            writeRegular(outputFile, crs, cs, ge, sds, nDims, activeCrsDims, roles,
                    dimNames, dimSizes, coordValues, xIdx, yIdx, gridXIdx, gridYIdx, width, height,
                    nonSpatialPositions, totalSlices, ncDataType, coverage, nGridDims, crsToGrid);
        }
    }

    // =========================================================================
    // Regular grid
    // =========================================================================

    private static void writeRegular(Path outputFile, CoordinateReferenceSystem crs, CoordinateSystem cs, GridExtent ge,
            List<SampleDimension> sds, int nDims, int[] activeCrsDims, AxisRole[] roles, String[] dimNames, int[] dimSizes,
            double[][] coordValues, int xIdx, int yIdx, int gridXIdx, int gridYIdx, int width, int height,
            List<Integer> nonSpatialPositions, int totalSlices, DataType ncDataType, GridCoverage coverage, int nGridDims, int[] crsToGrid)
            throws IOException, InvalidRangeException {

        final int[] cfOrder = buildCfDimOrder(roles, nDims);

        NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.createNewNetcdf3(outputFile.toString());
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.8"));
        writerBuilder.addAttribute(new Attribute("history", Instant.now() + " - Written by Examind using UCAR cdm-core"));

        // -- Dimensions --
        final List<Dimension> dimensions = new ArrayList<>(nDims);
        for (int i = 0; i < nDims; i++) {
            dimensions.add(writerBuilder.addDimension(dimNames[i], dimSizes[i]));
        }

        // -- Coordinate variables (one per axis) --
        for (int i = 0; i < nDims; i++) {
            final Variable.Builder<?> vb = writerBuilder.addVariable(dimNames[i], DataType.DOUBLE, List.of(dimensions.get(i)));
            addCfCoordAttributes(vb, roles[i], cs.getAxis(activeCrsDims[i]));
        }

        // -- Data variables (one per SampleDimension / band) --
        final List<Dimension> dataDimsOrdered = new ArrayList<>(nDims);
        for (int idx : cfOrder) dataDimsOrdered.add(dimensions.get(idx));

        final String[] dataVarNames = new String[sds.size()];
        for (int b = 0; b < sds.size(); b++) {
            final SampleDimension sd = sds.get(b);
            final String rawName = (sd.getName() != null) ? sd.getName().tip().toString() : "band_" + b;
            final String varName = NetcdfFileFormat.makeValidNetcdfObjectName(rawName.replaceAll("[^A-Za-z0-9_]", "_"));
            dataVarNames[b] = varName;

            final Variable.Builder<?> vb = writerBuilder.addVariable(varName, ncDataType, dataDimsOrdered);
            sd.getBackground().ifPresent(bg -> vb.addAttribute(new Attribute("_FillValue",
                    ncDataType == DataType.FLOAT || ncDataType == DataType.DOUBLE ? bg.doubleValue() : bg.longValue())));
            vb.addAttribute(new Attribute("long_name", rawName));
        }

        // ---- BUILD THE WRITER (creates the file format) ----
        try (NetcdfFormatWriter writer = writerBuilder.build()) {

            // -- Write coordinate data --
            for (int i = 0; i < nDims; i++) {
                writer.write(writer.findVariable(dimNames[i]), Array.makeFromJavaArray(coordValues[i]));
            }

            // -- Write band data (ND-aware: one render() call per 2D slice) ----
            // Locate X and Y positions in the CF-ordered dimension array
            int cfXPos = -1, cfYPos = -1;
            for (int p = 0; p < cfOrder.length; p++) {
                if (cfOrder[p] == xIdx) cfXPos = p;
                if (cfOrder[p] == yIdx) cfYPos = p;
            }

            final int[] shape = computeShape(cfOrder, dimSizes);
            final long[] strides = new long[nDims];
            strides[nDims - 1] = 1;
            for (int p = nDims - 2; p >= 0; p--) strides[p] = strides[p + 1] * shape[p + 1];

            for (int b = 0; b < sds.size(); b++) {
                final Variable v = writer.findVariable(dataVarNames[b]);
                final Array data = Array.factory(ncDataType, shape);

                // Iterate over every non-spatial slice combination
                for (int sliceFlat = 0; sliceFlat < totalSlices; sliceFlat++) {

                    // Decode sliceFlat → per-non-spatial-dimension indices
                    final int[] nsIndices = new int[nonSpatialPositions.size()];
                    int rem = sliceFlat;
                    for (int k = nonSpatialPositions.size() - 1; k >= 0; k--) {
                        nsIndices[k] = rem % dimSizes[nonSpatialPositions.get(k)];
                        rem /= dimSizes[nonSpatialPositions.get(k)];
                    }

                    // Render the 2D spatial slice for this non-spatial combination
                    final RenderedImage sliceImg = renderSlice(coverage, ge, nGridDims, gridXIdx, gridYIdx,
                            nonSpatialPositions, activeCrsDims, crsToGrid, dimSizes, nsIndices);
                    final java.awt.image.Raster raster = sliceImg.getData();
                    final int originX = sliceImg.getMinX();
                    final int originY = sliceImg.getMinY();

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

    // =========================================================================
    // Curvilinear (swath) grid
    // =========================================================================

    private static void writeCurvilinear(Path outputFile, MathTransform mt, CoordinateReferenceSystem crs, CoordinateSystem cs,
            GridExtent ge, List<SampleDimension> sds, int nDims, int nCrsDims, int[] activeCrsDims, AxisRole[] roles,
            String[] dimNames, int[] dimSizes, double[][] coordValues, int xIdx, int yIdx, int gridXIdx, int gridYIdx,
            int crsXIdx, int crsYIdx, int width, int height, List<Integer> nonSpatialPositions, int totalSlices,
            DataType ncDataType, GridCoverage coverage, int nGridDims, int[] crsToGrid)
            throws IOException, InvalidRangeException {

        NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.createNewNetcdf3(outputFile.toString());
        writerBuilder.addAttribute(new Attribute("Conventions", "CF-1.8"));
        writerBuilder.addAttribute(new Attribute("history", Instant.now() + " - Written by Examind using UCAR cdm-core"));

        final int[] cfOrder = buildCfDimOrder(roles, nDims);

        final List<Dimension> nonSpatialDims = new ArrayList<>();
        for (int idx : cfOrder) {
            if (roles[idx] == AxisRole.LAT || roles[idx] == AxisRole.LON) continue;
            final Dimension d = writerBuilder.addDimension(dimNames[idx], dimSizes[idx]);
            nonSpatialDims.add(d);
            final Variable.Builder<?> vb = writerBuilder.addVariable(dimNames[idx], DataType.DOUBLE, List.of(d));
            addCfCoordAttributes(vb, roles[idx], cs.getAxis(activeCrsDims[idx]));
        }

        final Dimension njDim = writerBuilder.addDimension("nj", height);
        final Dimension niDim = writerBuilder.addDimension("ni", width);
        final List<Dimension> njNi = List.of(njDim, niDim);

        final Variable.Builder<?> latAuxVb = writerBuilder.addVariable("lat", DataType.FLOAT, njNi);
        latAuxVb.addAttribute(new Attribute("standard_name", "latitude"));
        latAuxVb.addAttribute(new Attribute("long_name", "latitude"));
        latAuxVb.addAttribute(new Attribute("units", "degrees_north"));

        final Variable.Builder<?> lonAuxVb = writerBuilder.addVariable("lon", DataType.FLOAT, njNi);
        lonAuxVb.addAttribute(new Attribute("standard_name", "longitude"));
        lonAuxVb.addAttribute(new Attribute("long_name", "longitude"));
        lonAuxVb.addAttribute(new Attribute("units", "degrees_east"));

        final List<Dimension> dataDims = new ArrayList<>(nonSpatialDims);
        dataDims.add(njDim);
        dataDims.add(niDim);

        final int nsCount = nonSpatialDims.size();
        final int[] dataShape = new int[nsCount + 2];
        int nsIndexShape = 0;
        for (int idx : cfOrder) {
            if (roles[idx] == AxisRole.LAT || roles[idx] == AxisRole.LON) continue;
            dataShape[nsIndexShape++] = dimSizes[idx];
        }
        dataShape[nsCount] = height;
        dataShape[nsCount + 1] = width;

        final String[] dataVarNames = new String[sds.size()];
        for (int b = 0; b < sds.size(); b++) {
            final SampleDimension sd = sds.get(b);
            final String rawName = (sd.getName() != null) ? sd.getName().tip().toString() : "band_" + b;
            final String varName = NetcdfFileFormat.makeValidNetcdfObjectName(rawName.replaceAll("[^A-Za-z0-9_]", "_"));
            dataVarNames[b] = varName;

            final Variable.Builder<?> vb = writerBuilder.addVariable(varName, ncDataType, dataDims);
            sd.getBackground().ifPresent(bg -> vb.addAttribute(new Attribute("_FillValue",
                    ncDataType == DataType.FLOAT || ncDataType == DataType.DOUBLE ? bg.doubleValue() : bg.longValue())));
            vb.addAttribute(new Attribute("long_name", rawName));
            vb.addAttribute(new Attribute("coordinates", "lat lon"));
        }

        try (NetcdfFormatWriter writer = writerBuilder.build()) {
            for (int idx : cfOrder) {
                if (roles[idx] == AxisRole.LAT || roles[idx] == AxisRole.LON) continue;
                writer.write(writer.findVariable(dimNames[idx]), Array.makeFromJavaArray(coordValues[idx]));
            }

            final float[] latVals = new float[height * width];
            final float[] lonVals = new float[height * width];
            try {
                computeCurvilinearCoords(mt, ge, nGridDims, nCrsDims, gridXIdx, gridYIdx, crsXIdx, crsYIdx, width, height, latVals, lonVals);
            } catch (TransformException e) {
                throw new IOException("Failed to evaluate localization grid transform: " + e.getMessage(), e);
            }
            final int[] shape2D = {height, width};
            writer.write(writer.findVariable("lat"), Array.factory(DataType.FLOAT, shape2D, latVals));
            writer.write(writer.findVariable("lon"), Array.factory(DataType.FLOAT, shape2D, lonVals));

            final long[] dataStrides = new long[dataShape.length];
            dataStrides[dataShape.length - 1] = 1;
            for (int p = dataShape.length - 2; p >= 0; p--) dataStrides[p] = dataStrides[p + 1] * dataShape[p + 1];

            for (int b = 0; b < sds.size(); b++) {
                final Variable v = writer.findVariable(dataVarNames[b]);
                final Array data = Array.factory(ncDataType, dataShape);

                for (int sliceFlat = 0; sliceFlat < totalSlices; sliceFlat++) {
                    final int[] nsIndices = new int[nonSpatialPositions.size()];
                    int rem = sliceFlat;
                    for (int k = nonSpatialPositions.size() - 1; k >= 0; k--) {
                        nsIndices[k] = rem % dimSizes[nonSpatialPositions.get(k)];
                        rem /= dimSizes[nonSpatialPositions.get(k)];
                    }

                    final RenderedImage sliceImg = renderSlice(coverage, ge, nGridDims, gridXIdx, gridYIdx,
                            nonSpatialPositions, activeCrsDims, crsToGrid, dimSizes, nsIndices);
                    final java.awt.image.Raster raster = sliceImg.getData();
                    final int originX = sliceImg.getMinX();
                    final int originY = sliceImg.getMinY();

                    final int[] loopDataIdx = new int[dataShape.length];
                    int nsIdx = 0;
                    for (int idx : cfOrder) {
                        if (roles[idx] == AxisRole.LAT || roles[idx] == AxisRole.LON) continue;
                        int listIndex = nonSpatialPositions.indexOf(idx);
                        loopDataIdx[nsIdx++] = nsIndices[listIndex];
                    }

                    long baseFlat = 0;
                    for (int p = 0; p < nsCount; p++) baseFlat += (long) loopDataIdx[p] * dataStrides[p];

                    for (int row = 0; row < height; row++) {
                        for (int col = 0; col < width; col++) {
                            long flat = baseFlat + (long) row * width + col;
                            int px = originX + col, py = originY + row;

                            switch (ncDataType) {
                                case BYTE:   data.setByte((int)flat, (byte)raster.getSample(px, py, b)); break;
                                case SHORT:  data.setShort((int)flat, (short)raster.getSample(px, py, b)); break;
                                case INT:    data.setInt((int)flat, raster.getSample(px, py, b)); break;
                                case FLOAT:  data.setFloat((int)flat, raster.getSampleFloat(px, py, b)); break;
                                case DOUBLE: data.setDouble((int)flat, raster.getSampleDouble(px, py, b)); break;
                                default:     data.setFloat((int)flat, raster.getSampleFloat(px, py, b)); break;
                            }
                        }
                    }
                }
                writer.write(v, data);
            }
        }
    }

    private static void computeCurvilinearCoords(MathTransform mt, GridExtent ge, int nGridDims, int nCrsDims,
            int gridXIdx, int gridYIdx, int crsXIdx, int crsYIdx, int width, int height,
            float[] latOut, float[] lonOut) throws TransformException {

        final double[] srcRow = new double[width * nGridDims];
        final double[] dstRow = new double[width * nCrsDims];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int base = col * nGridDims;
                for (int d = 0; d < nGridDims; d++) srcRow[base + d] = ge.getLow(d) + 0.5;
                srcRow[base + gridYIdx] = ge.getLow(gridYIdx) + row + 0.5;
                srcRow[base + gridXIdx] = ge.getLow(gridXIdx) + col + 0.5;
            }
            mt.transform(srcRow, 0, dstRow, 0, width);

            for (int col = 0; col < width; col++) {
                final int base = col * nCrsDims;
                latOut[row * width + col] = (float) dstRow[base + crsYIdx];
                lonOut[row * width + col] = (float) dstRow[base + crsXIdx];
            }
        }
    }

    private static boolean isRowZeroNorth(GridGeometry gg, int crsLatAxis, int latSize, int nCrsDims) {
        try {
            final MathTransform mt = gg.getGridToCRS(PixelInCell.CELL_CENTER);
            final GridExtent    ge = gg.getExtent();
            final int nGridDims = ge.getDimension();

            final double[] firstRow = new double[nGridDims];
            final double[] lastRow  = new double[nGridDims];
            for (int d = 0; d < nGridDims; d++) {
                firstRow[d] = ge.getLow(d) + 0.5;
                lastRow[d]  = ge.getLow(d) + 0.5;
            }

            int gridLatAxis = -1;
            if (mt instanceof LinearTransform lt) {
                for (int gridIdx = 0; gridIdx < nGridDims; gridIdx++) {
                    if (lt.getMatrix().getElement(crsLatAxis, gridIdx) != 0.0) {
                        gridLatAxis = gridIdx; break;
                    }
                }
            }
            if (gridLatAxis >= 0) lastRow[gridLatAxis] = ge.getLow(gridLatAxis) + latSize - 0.5;
            else lastRow[crsLatAxis] = ge.getLow(crsLatAxis) + latSize - 0.5;

            final double[] crsFirst = new double[nCrsDims];
            final double[] crsLast  = new double[nCrsDims];
            mt.transform(firstRow, 0, crsFirst, 0, 1);
            mt.transform(lastRow,  0, crsLast,  0, 1);

            return crsFirst[crsLatAxis] > crsLast[crsLatAxis];
        } catch (Exception e) {
            return true;
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
    private static RenderedImage renderSlice(GridCoverage coverage, GridExtent ge, int nGridDims, int gridXIdx, int gridYIdx,
            List<Integer> nonSpatialPositions, int[] activeCrsDims, int[] crsToGrid, int[] dimSizes, int[] nsIndices) {
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
     */
    private static void addCfCoordAttributes(Variable.Builder<?> vb, AxisRole role, CoordinateSystemAxis axis) {
        switch (role) {
            case LON -> {
                vb.addAttribute(new Attribute("standard_name", "longitude"));
                vb.addAttribute(new Attribute("long_name", "longitude"));
                vb.addAttribute(new Attribute("units", "degrees_east"));
                vb.addAttribute(new Attribute("axis", "X"));
            }
            case LAT -> {
                vb.addAttribute(new Attribute("standard_name", "latitude"));
                vb.addAttribute(new Attribute("long_name", "latitude"));
                vb.addAttribute(new Attribute("units", "degrees_north"));
                vb.addAttribute(new Attribute("axis", "Y"));
            }
            case TIME -> {
                vb.addAttribute(new Attribute("standard_name", "time"));
                vb.addAttribute(new Attribute("long_name", "time"));
                vb.addAttribute(new Attribute("units", "seconds since 1970-01-01T00:00:00Z"));
                vb.addAttribute(new Attribute("calendar", "gregorian"));
                vb.addAttribute(new Attribute("axis", "T"));
            }
            case VERTICAL -> {
                final String unitStr = axis.getUnit() != null ? axis.getUnit().toString() : "m";
                final boolean isDown = axis.getDirection() == AxisDirection.DOWN;
                vb.addAttribute(new Attribute("standard_name", isDown ? "depth" : "height"));
                vb.addAttribute(new Attribute("long_name", isDown ? "depth" : "height"));
                vb.addAttribute(new Attribute("units", unitStr));
                vb.addAttribute(new Attribute("positive", isDown ? "down" : "up"));
                vb.addAttribute(new Attribute("axis", "Z"));
            }
            default -> vb.addAttribute(new Attribute("long_name", axis.getName().getCode()));
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
