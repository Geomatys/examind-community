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

    public static final String DATA_NAME = "data";
    private static final String DATA_REMARKS = "The data to save";
    public static final ParameterDescriptor<GridCoverage> DATA = BUILDER
            .addName(DATA_NAME)
            .setRemarks(DATA_REMARKS)
            .setRequired(true)
            .create(GridCoverage.class, null);

    public static final String FORMAT_NAME = "format";
    private static final String FORMAT_REMARKS = "Format used to save the coverage";
    public static final ParameterDescriptor<String> FORMAT = BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String OPTIONS_NAME = "options";
    private static final String OPTIONS_REMARKS = "The file format parameters to be used to create the file(s)";
    public static final ParameterDescriptor<Object> OPTIONS = BUILDER
            .addName(OPTIONS_NAME)
            .setRemarks(OPTIONS_REMARKS)
            .setRequired(false)
            .create(Object.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATA, FORMAT, OPTIONS);

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
