package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                    NetCdfUtils.writeNetcdf(coverage, f);
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
}
