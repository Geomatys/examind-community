package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.base.MemoryGridResource;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.apache.sis.storage.netcdf.NetcdfStoreProvider;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.COVERAGE;
import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.FORMAT;
import static org.constellation.process.utils.coverage.SaveResultCoverageDescriptor.OUTPUT;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class SaveResultCoverageProcess extends AbstractCstlProcess  {

    public SaveResultCoverageProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            GridCoverage coverage = inputParameters.getMandatoryValue(COVERAGE);
            String format = inputParameters.getMandatoryValue(FORMAT);

            if (format.equalsIgnoreCase("GTIFF") || format.equalsIgnoreCase("GeoTIFF")) {
                coverage = coverage.forConvertedValues(false);

                final Path f = Files.createTempFile("data", ".tiff");
                StorageConnector cnx = new StorageConnector(f);
                cnx.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                });

                try (DataStore store = new GeoTiffStoreProvider().open(cnx)) {
                    WritableAggregate agg = (WritableAggregate)store;
                    agg.add(new MemoryGridResource(null, coverage, null));
                } catch (UnsupportedStorageException e) {
                    throw new ProcessException("Geotiff storage is not supported for the moment.", this, e);
                } catch (DataStoreException e) {
                    throw new ProcessException("An error occurred during tiff writing.", this, e);
                }

                outputParameters.getOrCreate(OUTPUT).setValue(f);
            } else if (format.equalsIgnoreCase("NETCDF")) {

                final Path f = Files.createTempFile("data", ".netcdf");
                StorageConnector cnx = new StorageConnector(f);
                cnx.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                });

                try (DataStore store = new NetcdfStoreProvider().open(cnx)) {
                    WritableAggregate agg = (WritableAggregate)store;
                    agg.add(new MemoryGridResource(null, coverage, null));
                } catch (UnsupportedStorageException e) {
                    throw new ProcessException("Netcdf storage is not supported for the moment.", this, e);
                } catch (DataStoreException e) {
                    throw new ProcessException("An error occurred during netcdf writing.", this, e);
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
