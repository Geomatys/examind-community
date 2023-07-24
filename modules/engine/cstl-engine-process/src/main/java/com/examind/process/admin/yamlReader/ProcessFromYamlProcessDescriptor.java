package com.examind.process.admin.yamlReader;

import com.examind.process.admin.AdminProcessDescriptor;
import java.nio.file.Path;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.Process;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

public class ProcessFromYamlProcessDescriptor extends AbstractProcessDescriptor implements AdminProcessDescriptor {

    /**Process name : addition */
    public static final String NAME = "yamlReader";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    public static final String DATA_FOLDER_NAME = "yaml path";
    public static final String DATA_FOLDER_DESC = "yaml path";
    public static final ParameterDescriptor<Path> YAML_PATH = PARAM_BUILDER
            .addName(DATA_FOLDER_NAME)
            .setRemarks(DATA_FOLDER_DESC)
            .setRequired(true)
            .create(Path.class, null);

    public static final ParameterDescriptorGroup INPUT_DESC = PARAM_BUILDER.addName("InputParameters").createGroup(YAML_PATH);

    public static final String PROCESS_OUTPUT_NAME = "out";
    public static final String PROCESS_OUTPUT_DESC = "JSON value of the process output";
    public static final ParameterDescriptor<String> PROCESS_OUTPUT = PARAM_BUILDER
            .addName(PROCESS_OUTPUT_NAME)
            .setRemarks(PROCESS_OUTPUT_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptorGroup OUTPUT_DESC = PARAM_BUILDER.addName("OutputParameters").createGroup(PROCESS_OUTPUT);

    public ProcessFromYamlProcessDescriptor() {
        super(NAME,
                ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Read a Yaml to create a process."),
                INPUT_DESC,
                OUTPUT_DESC);
    }

    @Override
    public Process createProcess(ParameterValueGroup parameterValueGroup) {
        return new ProcessFromYamlProcess(this, parameterValueGroup);
    }
}
