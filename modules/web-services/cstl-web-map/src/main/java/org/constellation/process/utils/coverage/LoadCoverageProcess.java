package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.NameInProvider;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProviders;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.BANDS;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.COVERAGE_LAYER;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.OUTPUT;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.PROPERTIES;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.SERVICE;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.SPATIAL_EXTENT;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.TEMPORAL_EXTENT;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCoverageProcess extends AbstractCstlProcess  {

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    public LoadCoverageProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            String serviceName = inputParameters.getMandatoryValue(SERVICE);
            String coverageName = inputParameters.getMandatoryValue(COVERAGE_LAYER);
            Envelope spatialExtent = inputParameters.getValue(SPATIAL_EXTENT);
            String[] temporalExtent = inputParameters.getValue(TEMPORAL_EXTENT);
            String[] bands = inputParameters.getValue(BANDS);

            //TODO : Properties are not used for the moment as examind does not support STAC Queries, and data selection by properties.
            Map properties = inputParameters.getValue(PROPERTIES);

            Integer dataId;
            try {
                //Find Service ID with its name
                Integer serviceId = serviceBusiness.getServiceIdByIdentifierAndType("wcs",serviceName);

                if (serviceId == null) {
                    throw new ProcessException("Impossible to load the coverage, service not found :" + serviceName + " (the service needs to be a wcs)", this, null);
                }

                //Find Data ID with in the layers
                NameInProvider nameInProvider = layerBusiness.getFullLayerName(serviceId, coverageName, null, null);

                if (nameInProvider == null) {
                    throw new ProcessException("Impossible to load the coverage, data / layer not found :" + coverageName + " (the coverage needs to be in the service specified)", this, null);
                }
                dataId = nameInProvider.dataId;

            } catch (ConstellationException ex) {
                throw new ProcessException("Impossible to load this data, no data named :" + coverageName, this, null);
            }

            var data = DataProviders.getProviderData(dataId);
            if (data == null) throw new ProcessException("Impossible to load provider data with id :" + dataId, this, null);
            var res = data.getOrigin();

            GridCoverage gridCoverage = null;
            if (res instanceof GridCoverageResource gcr) {
                temporalExtent = Utils.formatTemporalExtent(temporalExtent);

                GridGeometry originGeometry = gcr.getGridGeometry();
                Envelope originEnvelope = Utils.getSourceEnvelope(gcr, this);
                GridGeometry gridGeometry = Utils.generateResultGridGeometry(originEnvelope, originGeometry, spatialExtent, temporalExtent, this);

                gridCoverage = gcr.read(gridGeometry);
                gridCoverage = Utils.selectBands(bands, gridCoverage);

            } else {
                throw new ProcessException("Impossible to load this data because it's not a GridCoverageResource (or is null)", this, null);
            }

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);
        } catch (DataStoreException | ConfigurationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
