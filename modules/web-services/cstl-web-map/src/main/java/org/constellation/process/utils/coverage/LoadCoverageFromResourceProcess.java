package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.utils.coverage.LoadCoverageFromResourceDescriptor.COVERAGE;
import static org.constellation.process.utils.coverage.LoadCoverageFromResourceDescriptor.OUTPUT;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCoverageFromResourceProcess extends AbstractCstlProcess  {

    public LoadCoverageFromResourceProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            GridCoverageResource cov = inputParameters.getMandatoryValue(COVERAGE);
            GridCoverage gridCoverage = cov.read(cov.getGridGeometry());

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);
        } catch (DataStoreException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
