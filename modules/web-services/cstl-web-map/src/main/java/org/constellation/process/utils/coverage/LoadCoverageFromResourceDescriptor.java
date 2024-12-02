package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class LoadCoverageFromResourceDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "coverage.loadFromResource";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Load a coverage resource");

    public static final String COVERAGE_NAME = "coverage";
    private static final String COVERAGE_REMARKS = "The coverage to load";
    public static final ParameterDescriptor<GridCoverageResource> COVERAGE = BUILDER
            .addName(COVERAGE_NAME)
            .setRemarks(COVERAGE_REMARKS)
            .setRequired(true)
            .create(GridCoverageResource.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(COVERAGE);

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
    public LoadCoverageFromResourceDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new LoadCoverageFromResourceDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new LoadCoverageFromResourceProcess(this, input);
    }
}
