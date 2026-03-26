package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.DATA;
import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.FORMAT;
import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.OPTIONS;
import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.OUTPUT;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class SaveResultCoverageProcess extends AbstractCstlProcess {

    public SaveResultCoverageProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            GridCoverage coverage = inputParameters.getMandatoryValue(DATA);
            String format = inputParameters.getMandatoryValue(FORMAT);

            // Not used for the moment
            // TODO : take into account "Options"
            Object options = inputParameters.getValue(OPTIONS);

            if (format.equalsIgnoreCase("GTIFF") || format.equalsIgnoreCase("GeoTIFF")) {
                coverage = coverage.forConvertedValues(false);

                final Path f = Files.createTempFile("data", ".tiff");

                try (final GeoTiffStore iowriter = (GeoTiffStore) DataStores.openWritable(f, "GeoTIFF")) {
                    iowriter.append(coverage, null);
                } catch (UnsupportedStorageException e) {
                    throw new ProcessException("Geotiff storage is not supported for the moment. " + e.getLocalizedMessage(), this, e);
                } catch (DataStoreException e) {
                    throw new ProcessException("An error occurred during tiff writing. " + e.getLocalizedMessage(), this, e);
                }

                outputParameters.getOrCreate(OUTPUT).setValue(f);

            } else if (format.equalsIgnoreCase("NETCDF")) {

                final Path f = Files.createTempFile("data", ".nc");
                try {
                    writeNetcdf(coverage, f);
                } catch (InvalidRangeException e) {
                    throw new ProcessException("An error occurred during NetCDF writing: " + e.getLocalizedMessage(), this, e);
                }
                outputParameters.getOrCreate(OUTPUT).setValue(f);

            } else {
                throw new ProcessException("No existing / supported format named : " + format + ", format supported : GTIFF, NETCDF", this, null);
            }

        } catch (IOException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }

    /**
     * Writes a {@link GridCoverage} to a NetCDF-4 file using the UCAR library directly.
     *
     * <p>The file will contain:
     * <ul>
     *   <li>{@code lat} — 1-D latitude coordinate variable (degrees_north)</li>
     *   <li>{@code lon} — 1-D longitude coordinate variable (degrees_east)</li>
     *   <li>One data variable per {@link SampleDimension} (band), named after the band or
     *       {@code band_N} when no name is available, with dimensions {@code [lat, lon]}.</li>
     * </ul>
     *
     * @param coverage the source coverage (packed / non-converted values are used)
     * @param outputFile the NetCDF output file path (will be overwritten if it exists)
     * @throws IOException           on I/O error
     * @throws InvalidRangeException if a UCAR array write fails (should not happen in practice)
     */
    @SuppressWarnings("deprecation")  // NetcdfFileWriter and Variable.addAttribute() are deprecated in cdm-core 5.5+
    private void writeNetcdf(GridCoverage coverage, Path outputFile)
            throws IOException, InvalidRangeException {


        // Use packed (non-converted) values to preserve the original data type.
        coverage = coverage.forConvertedValues(false);

        final GridGeometry gridGeometry  = coverage.getGridGeometry();
        final GridExtent   gridExtent    = gridGeometry.getExtent();
        final RenderedImage image        = coverage.render(null);
        final List<SampleDimension> bands = coverage.getSampleDimensions();

        // Grid dimensions: we assume a 2-D grid with axes ordered (column=x, row=y).
        // GridGeometry axis indices: 0 = x (longitude), 1 = y (latitude)
        final int width  = (int) gridExtent.getSize(0);   // number of columns / longitudes
        final int height = (int) gridExtent.getSize(1);   // number of rows / latitudes

        // Build lat / lon coordinate arrays from the envelope.
        final GeneralEnvelope envelope = new GeneralEnvelope(gridGeometry.getEnvelope());
        final double minLon = envelope.getMinimum(0);
        final double maxLon = envelope.getMaximum(0);
        final double minLat = envelope.getMinimum(1);
        final double maxLat = envelope.getMaximum(1);

        // Cell-centre coordinates
        final double lonStep = (width  > 1) ? (maxLon - minLon) / width  : 0.0;
        final double latStep = (height > 1) ? (maxLat - minLat) / height : 0.0;

        final float[] lonValues = new float[width];
        for (int i = 0; i < width; i++) {
            lonValues[i] = (float) (minLon + (i + 0.5) * lonStep);
        }
        // Latitude is stored top-to-bottom in the rendered image (row 0 = maxLat).
        final float[] latValues = new float[height];
        for (int j = 0; j < height; j++) {
            latValues[j] = (float) (maxLat - (j + 0.5) * latStep);
        }

        // Determine the UCAR DataType from the image's sample model.
        final DataType ncDataType = toNcDataType(image.getSampleModel().getDataType());

        try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf4, outputFile.toString(), null)) {

            // -------- Define dimensions --------
            final Dimension dimLat = writer.addDimension(null, "lat", height);
            final Dimension dimLon = writer.addDimension(null, "lon", width);

            final List<Dimension> latDims = List.of(dimLat);
            final List<Dimension> lonDims = List.of(dimLon);
            final List<Dimension> dataDims = List.of(dimLat, dimLon);

            // -------- Define coordinate variables --------
            final Variable varLat = writer.addVariable(null, "lat", DataType.FLOAT, latDims);
            varLat.addAttribute(new ucar.nc2.Attribute("units", "degrees_north"));
            varLat.addAttribute(new ucar.nc2.Attribute("long_name", "latitude"));
            varLat.addAttribute(new ucar.nc2.Attribute("standard_name", "latitude"));
            varLat.addAttribute(new ucar.nc2.Attribute("axis", "Y"));

            final Variable varLon = writer.addVariable(null, "lon", DataType.FLOAT, lonDims);
            varLon.addAttribute(new ucar.nc2.Attribute("units", "degrees_east"));
            varLon.addAttribute(new ucar.nc2.Attribute("long_name", "longitude"));
            varLon.addAttribute(new ucar.nc2.Attribute("standard_name", "longitude"));
            varLon.addAttribute(new ucar.nc2.Attribute("axis", "X"));

            // -------- Define one variable per band --------
            final List<Variable> dataVars = new ArrayList<>(bands.size());
            for (int b = 0; b < bands.size(); b++) {
                final SampleDimension sd = bands.get(b);
                final String bandName = sd.getName() != null
                        ? sd.getName().tip().toString().replaceAll("[^A-Za-z0-9_]", "_")
                        : "band_" + b;
                final Variable v = writer.addVariable(null, bandName, ncDataType, dataDims);
                // Propagate no-data value when available.
                sd.getBackground().ifPresent(bg ->
                        v.addAttribute(new ucar.nc2.Attribute("_FillValue",
                                ncDataType == DataType.FLOAT || ncDataType == DataType.DOUBLE
                                        ? bg.doubleValue()
                                        : bg.longValue())));
                dataVars.add(v);
            }

            // -------- Write file header --------
            writer.create();

            // -------- Write coordinate data --------
            writer.write(varLat, Array.makeFromJavaArray(latValues));
            writer.write(varLon, Array.makeFromJavaArray(lonValues));

            // -------- Write band data --------
            for (int b = 0; b < bands.size(); b++) {
                final Variable v      = dataVars.get(b);
                final int[]    shape  = new int[]{height, width};
                final Array    ncData = Array.factory(ncDataType, shape);

                // Obtain the raster once (getData() is potentially expensive).
                final java.awt.image.Raster raster = image.getData();
                final int originX = image.getMinX();
                final int originY = image.getMinY();

                // Read pixel values row by row from the rendered image,
                // using the correct accessor method for each numeric type.
                for (int row = 0; row < height; row++) {
                    for (int col = 0; col < width; col++) {
                        final int idx = row * width + col;
                        final int px  = originX + col;
                        final int py  = originY + row;
                        switch (ncDataType) {
                            case BYTE:
                                ncData.setByte  (idx, (byte)  raster.getSample     (px, py, b)); break;
                            case SHORT:
                                ncData.setShort (idx, (short) raster.getSample     (px, py, b)); break;
                            case INT:
                                ncData.setInt   (idx,          raster.getSample     (px, py, b)); break;
                            case FLOAT:
                                ncData.setFloat (idx,          raster.getSampleFloat (px, py, b)); break;
                            case DOUBLE:
                                ncData.setDouble(idx,          raster.getSampleDouble(px, py, b)); break;
                            default:
                                ncData.setFloat (idx,          raster.getSampleFloat (px, py, b)); break;
                        }
                    }
                }
                writer.write(v, ncData);
            }
        }
    }

    /**
     * Maps a {@link DataBuffer} type constant to the corresponding UCAR {@link DataType}.
     *
     * @param dataBufferType one of the {@code DataBuffer.TYPE_*} constants
     * @return the closest NetCDF-3 compatible data type (defaults to {@code FLOAT})
     */
    private static DataType toNcDataType(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:   return DataType.BYTE;
            case DataBuffer.TYPE_SHORT:  return DataType.SHORT;
            case DataBuffer.TYPE_USHORT: return DataType.SHORT;   // NetCDF-3 has no unsigned short
            case DataBuffer.TYPE_INT:    return DataType.INT;
            case DataBuffer.TYPE_FLOAT:  return DataType.FLOAT;
            case DataBuffer.TYPE_DOUBLE: return DataType.DOUBLE;
            default:                     return DataType.FLOAT;
        }
    }
}
