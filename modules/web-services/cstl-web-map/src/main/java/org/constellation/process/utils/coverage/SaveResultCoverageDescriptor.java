package org.constellation.process.utils.coverage;

import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

import java.nio.file.Path;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class SaveResultCoverageDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "coverage.save_result";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Save a coverage in a specified format");

    public static final String COVERAGE_NAME = "coverage";
    private static final String COVERAGE_REMARKS = "The coverage to save";
    public static final ParameterDescriptor<GridCoverage> COVERAGE = BUILDER
            .addName(COVERAGE_NAME)
            .setRemarks(COVERAGE_REMARKS)
            .setRequired(true)
            .create(GridCoverage.class, null);

    public static final String FORMAT_NAME = "format";
    private static final String FORMAT_REMARKS = "Format used to save the coverage";
    public static final ParameterDescriptor<String> FORMAT = BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(COVERAGE, FORMAT);

    public static final String OUTPUT_NAME = "result";
    private static final String OUTPUT_REMARKS = "Result";
    public static final ParameterDescriptor<Path> OUTPUT = BUILDER
            .addName(OUTPUT_NAME)
            .setRemarks(OUTPUT_REMARKS)
            .setRequired(true)
            .create(Path.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUTPUT);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public SaveResultCoverageDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new SaveResultCoverageDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new SaveResultCoverageProcess(this, input);
    }
}
