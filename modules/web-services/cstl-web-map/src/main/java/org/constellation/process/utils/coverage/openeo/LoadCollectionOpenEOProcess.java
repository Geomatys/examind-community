package org.constellation.process.utils.coverage.openeo;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.parameter.Parameters;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.utils.coverage.LoadCoverageDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

import java.util.Map;

import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.BANDS;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.EXTERNAL_STAC_URL;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.EXTERNAL_STAC_CUSTOM_PROCESS;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.COVERAGE_LAYER;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.OUTPUT;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.PROPERTIES;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.SERVICE;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.SPATIAL_EXTENT;
import static org.constellation.process.utils.coverage.openeo.LoadCollectionOpenEODescriptor.TEMPORAL_EXTENT;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCollectionOpenEOProcess extends AbstractCstlProcess  {

    public LoadCollectionOpenEOProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        String serviceName = inputParameters.getMandatoryValue(SERVICE);
        String externalStacURL = inputParameters.getValue(EXTERNAL_STAC_URL);
        String externalStacCustomProcess = inputParameters.getValue(EXTERNAL_STAC_CUSTOM_PROCESS);
        String coverageName = inputParameters.getMandatoryValue(COVERAGE_LAYER);
        Envelope spatialExtent = inputParameters.getValue(SPATIAL_EXTENT);
        String[] temporalExtent = inputParameters.getValue(TEMPORAL_EXTENT);
        String[] bands = inputParameters.getValue(BANDS);
        Map properties = inputParameters.getValue(PROPERTIES);

        if (externalStacURL != null && !externalStacURL.isEmpty()) {
            final ProcessDescriptor loadStacDesc = LoadStacDescriptor.INSTANCE;
            final Parameters paramsLoad = Parameters.castOrWrap(loadStacDesc.getInputDescriptor().createValue());

            String stacUrl = externalStacURL + (externalStacURL.endsWith("/") ? "" : "/") + "collections/" + coverageName;

            paramsLoad.parameter(LoadStacDescriptor.STAC_URL.getName().getCode()).setValue(stacUrl);
            paramsLoad.parameter(LoadStacDescriptor.SPATIAL_EXTENT.getName().getCode()).setValue(spatialExtent);
            paramsLoad.parameter(LoadStacDescriptor.TEMPORAL_EXTENT.getName().getCode()).setValue(temporalExtent);
            paramsLoad.parameter(LoadStacDescriptor.BANDS.getName().getCode()).setValue(bands);
            paramsLoad.parameter(LoadStacDescriptor.PROPERTIES.getName().getCode()).setValue(properties);

            if (externalStacCustomProcess != null && !externalStacCustomProcess.isEmpty()) {
                paramsLoad.parameter(LoadStacDescriptor.EXTERNAL_STAC_CUSTOM_PROCESS.getName().getCode()).setValue(externalStacCustomProcess);
            }

            final Process loadProcess = loadStacDesc.createProcess(paramsLoad);
            GridCoverage gridCoverage = (GridCoverage) loadProcess.call().parameter("result").getValue();

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);
        } else {
            final ProcessDescriptor loadCoverageDesc = LoadCoverageDescriptor.INSTANCE;
            final Parameters paramsLoad = Parameters.castOrWrap(loadCoverageDesc.getInputDescriptor().createValue());
            paramsLoad.parameter(LoadCoverageDescriptor.SERVICE.getName().getCode()).setValue(serviceName);
            paramsLoad.parameter(LoadCoverageDescriptor.COVERAGE_LAYER.getName().getCode()).setValue(coverageName);
            paramsLoad.parameter(LoadCoverageDescriptor.SPATIAL_EXTENT.getName().getCode()).setValue(spatialExtent);
            paramsLoad.parameter(LoadCoverageDescriptor.TEMPORAL_EXTENT.getName().getCode()).setValue(temporalExtent);
            paramsLoad.parameter(LoadCoverageDescriptor.BANDS.getName().getCode()).setValue(bands);
            paramsLoad.parameter(LoadCoverageDescriptor.PROPERTIES.getName().getCode()).setValue(properties);

            final Process loadProcess = loadCoverageDesc.createProcess(paramsLoad);
            GridCoverage gridCoverage = (GridCoverage) loadProcess.call().parameter("result").getValue();

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);
        }
    }
}
