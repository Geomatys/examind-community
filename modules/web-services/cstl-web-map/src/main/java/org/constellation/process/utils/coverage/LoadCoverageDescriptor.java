package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCoverageDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "coverage.load";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Load a coverage resource from a WCS service (serviceID)");

    public static final String SERVICE_NAME = "serviceId";
    private static final String SERVICE_REMARKS = "The service Id where the coverage is";
    public static final ParameterDescriptor<String> SERVICE = BUILDER
            .addName(SERVICE_NAME)
            .setRemarks(SERVICE_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String COVERAGE_NAME = "layerId";
    private static final String COVERAGE_REMARKS = "The coverage Id to load";
    public static final ParameterDescriptor<String> COVERAGE = BUILDER
            .addName(COVERAGE_NAME)
            .setRemarks(COVERAGE_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String COVERAGE_LAYER_NAME = "coverageLayerId";
    private static final String COVERAGE_LAYER_REMARKS = "The coverage layer Id to load";
    public static final ParameterDescriptor<String> COVERAGE_LAYER = BUILDER
            .addName(COVERAGE_LAYER_NAME)
            .setRemarks(COVERAGE_LAYER_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String SPATIAL_EXTENT_NAME = "spatial_extent";
    private static final String SPATIAL_EXTENT_REMARKS = "The bounding box / spatial extent to load";
    public static final ParameterDescriptor<Envelope> SPATIAL_EXTENT = BUILDER
            .addName(SPATIAL_EXTENT_NAME)
            .setRemarks(SPATIAL_EXTENT_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);

    public static final String TEMPORAL_EXTENT_NAME = "temporal_extent";
    private static final String TEMPORAL_EXTENT_REMARKS = "The temporal extent to load (2 values (interval))";
    public static final ParameterDescriptor<String[]> TEMPORAL_EXTENT = BUILDER
            .addName(TEMPORAL_EXTENT_NAME)
            .setRemarks(TEMPORAL_EXTENT_REMARKS)
            .setRequired(false)
            .create(String[].class, null);

    public static final String BANDS_NAME = "bands";
    private static final String BANDS_REMARKS = "Bands to load";
    public static final ParameterDescriptor<Integer[]> BANDS = BUILDER
            .addName(BANDS_NAME)
            .setRemarks(BANDS_REMARKS)
            .setRequired(false)
            .create(Integer[].class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(SERVICE, COVERAGE_LAYER, SPATIAL_EXTENT, TEMPORAL_EXTENT, BANDS);

    public static final String OUTPUT_NAME = "result";
    private static final String OUTPUT_REMARKS = "The GridCoverage loaded";
    public static final ParameterDescriptor<GridCoverage> OUTPUT = BUILDER
            .addName(OUTPUT_NAME)
            .setRemarks(OUTPUT_REMARKS)
            .setRequired(true)
            .create(GridCoverage.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUTPUT);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public LoadCoverageDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new LoadCoverageDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new LoadCoverageProcess(this, input);
    }
}
