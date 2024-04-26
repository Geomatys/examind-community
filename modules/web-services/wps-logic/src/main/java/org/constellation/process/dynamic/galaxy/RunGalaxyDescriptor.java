package org.constellation.process.dynamic.galaxy;

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.dynamic.AbstractDynamicDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

import java.util.ArrayList;
import java.util.List;

/**
 * ProcessDescriptor for running a Galaxy workflow.
 *
 * @author Quentin BIALOTA (Geomatys).
 *
 */
public class RunGalaxyDescriptor extends AbstractDynamicDescriptor {

    public static final String NAME = "galaxy.run";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Run a Galaxy process in examind");

    public static final String WORKFLOW_ID_NAME = "workflow.id";
    private static final String WORKFLOW_ID_REMARKS = "The galaxy workflow id (can be found in URL)";
    public static final ParameterDescriptor<String> WORKFLOW_ID = BUILDER
            .addName(WORKFLOW_ID_NAME)
            .setRemarks(WORKFLOW_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public RunGalaxyDescriptor(String name) {
        super(name, ABSTRACT);
    }

    @Override
    public final ParameterDescriptorGroup getInputDescriptor() {
        List<GeneralParameterDescriptor> inputs = new ArrayList<>(dynamicInput);
        inputs.add(WORKFLOW_ID);
        return BUILDER.addName("InputParameters").setRequired(true).createGroup(inputs.toArray(new GeneralParameterDescriptor[inputs.size()]));
    }

    @Override
    public final ParameterDescriptorGroup getOutputDescriptor() {
        return BUILDER.addName("OutputParameters").setRequired(true).createGroup(dynamicOutput.toArray(new GeneralParameterDescriptor[dynamicOutput.size()]));
    }

    @Override
    public final AbstractCstlProcess createProcess(final ParameterValueGroup input) {
        return new RunGalaxy(this, input);
    }
}
