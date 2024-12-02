package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.NameInProvider;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProviders;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.coverage.bandselect.BandSelectDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.BANDS;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.COVERAGE_LAYER;
import static org.constellation.process.utils.coverage.LoadCoverageDescriptor.OUTPUT;
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
            Integer[] bands = inputParameters.getValue(BANDS);

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

                GridGeometry originGeometry = gcr.getGridGeometry();
                Envelope originEnvelope;
                try {
                    originEnvelope = gcr.getEnvelope().orElse(new GeneralEnvelope(CRS.forCode("urn:ogc:def:crs:OGC:2:84")));
                } catch (FactoryException ex) {
                    throw new ProcessException("CRS doesn't exist : urn:ogc:def:crs:OGC:2:84" , this, ex);
                }

                //SPATIAL EXTENT
                GeneralEnvelope envelope;
                if (spatialExtent != null) {
                    envelope = (GeneralEnvelope) spatialExtent;
                } else {
                    envelope = (GeneralEnvelope) originEnvelope;
                }

                //TEMPORAL EXTENT
                if (temporalExtent != null && temporalExtent.length > 0) {
                    int timeDimensionId = -1;
                    int dimensionSize = originGeometry.getDimension();
                    for (int dimIdx = 0; dimIdx < dimensionSize; dimIdx++) {
                        CoordinateSystemAxis csa = envelope.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(dimIdx);
                        String abbreviation = csa.getAbbreviation().toLowerCase();
                        AxisDirection axisDirection = csa.getDirection();
                        DimensionNameType axisType = originGeometry.getExtent().getAxisType(dimIdx).orElse(null);

                        if (axisType == DimensionNameType.TIME || axisDirection == AxisDirection.FUTURE || abbreviation.equals("time")) {
                            timeDimensionId = dimIdx;
                            break;
                        }
                    }

                    if (timeDimensionId > -1) {

                        TemporalCRS temporalCRS = CRS.getTemporalComponent(envelope.getCoordinateReferenceSystem());
                        if (temporalCRS == null) {
                            throw new ProcessException("No temporal CRS found for axis : " + timeDimensionId, this);
                        }
                        DefaultTemporalCRS defaultTemporalCRS = DefaultTemporalCRS.castOrCopy(temporalCRS);

                        double minVal = originEnvelope.getMinimum(timeDimensionId);
                        double maxVal = originEnvelope.getMaximum(timeDimensionId);

                        double firstValue = 0.0;
                        double secondValue = 0.0;
                        if (temporalExtent.length == 1) { //In case of slice
                            Instant datetime = Instant.parse(temporalExtent[0]);
                            firstValue = defaultTemporalCRS.toValue(datetime);
                            secondValue = firstValue;
                        } else if (temporalExtent.length == 2) { //In case of subset
                            Instant datetime = Instant.parse(temporalExtent[0]);
                            firstValue = defaultTemporalCRS.toValue(datetime);
                            datetime = Instant.parse(temporalExtent[1]);
                            secondValue = defaultTemporalCRS.toValue(datetime);
                        }

                        if ((firstValue < minVal || firstValue > maxVal) && (secondValue < minVal || secondValue > maxVal)) {
                            throw new ProcessException("Subsetting temporal params overlap the source data envelope extent", this, null);
                        }

                        envelope.setRange(timeDimensionId, firstValue, secondValue);
                    }
                }

                GridGeometry gridGeometry = originGeometry.derive().subgrid(envelope).build();
                gridCoverage = gcr.read(gridGeometry);

                //BANDS
                if (bands != null && bands.length > 0) {

                    final ProcessDescriptor coverageBandSelectDesc = BandSelectDescriptor.INSTANCE;
                    final Parameters params = Parameters.castOrWrap(coverageBandSelectDesc.getInputDescriptor().createValue());
                    params.parameter("coverage").setValue(gridCoverage);
                    params.parameter("bands").setValue(bands);
                    final Process process = coverageBandSelectDesc.createProcess(params);
                    gridCoverage = (GridCoverage) process.call().parameter("result").getValue();

                }

            } else {
                throw new ProcessException("Impossible to load this data because it's not a GridCoverageResource (or is null)", this, null);
            }

            outputParameters.getOrCreate(OUTPUT).setValue(gridCoverage);
        } catch (DataStoreException | ConfigurationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
