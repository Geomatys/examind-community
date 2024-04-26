package org.constellation.process.dynamic.galaxy;

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

import java.nio.file.Path;

/**
 * ProcessDescriptor for deploying a Galaxy workflow.
 *
 * @author Quentin BIALOTA (Geomatys).
 *
 */
public class DeployGalaxyDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "galaxy.deploy";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Register a new workflow hosted by a galaxy server in examind");

    public static final String GALAXY_WORKFLOW_ID_NAME = "galaxy.workflow.id";
    private static final String GALAXY_WORKFLOW_ID_REMARKS = "The galaxy workflow id (can be found in URL)";
    public static final ParameterDescriptor<String> GALAXY_WORKFLOW_ID = BUILDER
            .addName(GALAXY_WORKFLOW_ID_NAME)
            .setRemarks(GALAXY_WORKFLOW_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String PROCESS_DESCRIPTION_NAME = "configuration";
    private static final String PROCESS_DESCRIPTION_REMARKS = "A document describing the input / output of the workflow to deploy.";
//    public static final ParameterDescriptor<ProcessDescription> PROCESS_DESCRIPTION = BUILDER
//            .addName(PROCESS_DESCRIPTION_NAME)
//            .setRemarks(PROCESS_DESCRIPTION_REMARKS)
//            .setRequired(false)
//            .create(ProcessDescription.class, null);

    public static final ParameterDescriptor<Path> PROCESS_DESCRIPTION = BUILDER
        .addName(PROCESS_DESCRIPTION_NAME)
        .setRemarks(PROCESS_DESCRIPTION_REMARKS)
        .setRequired(false)
        .create(Path.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(GALAXY_WORKFLOW_ID, PROCESS_DESCRIPTION);


    public static final String PROCESS_ID_NAME = "process.id";
    private static final String PROCESS_ID_REMARKS = "The assigned identifier of the deployed workflow.";
    public static final ParameterDescriptor<String> PROCESS_ID = BUILDER
            .addName(PROCESS_ID_NAME)
            .setRemarks(PROCESS_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(PROCESS_ID);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeployGalaxyDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeployGalaxy(this, input);
    }

}
