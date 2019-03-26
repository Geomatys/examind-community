package org.constellation.process.test.testprocess;

import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.geometry.Envelope;

import static org.constellation.process.test.testprocess.TestDescriptor.*;
import org.geotoolkit.processing.AbstractProcess;


/**
 *
 * @author Theo Zozime
 *
 * Process for testing purposes.
 * Takes 3 optional parameters : a bounding box, and two literal
 * The result is string containing the values of the three parameters
 */
public class TestProcess extends AbstractProcess {


    public TestProcess(final ParameterValueGroup input) {
        super(INSTANCE, input);
    }

    @Override
    protected void execute() throws ProcessException {
        final Envelope envelope = inputParameters.getValue(BBOX_IN);
        final double doubleValue = inputParameters.getValue(DOUBLE_IN);
        final String stringValue = inputParameters.getValue(STRING_IN);

        outputParameters.getOrCreate(STRING_OUT).setValue(envelope.toString() + " " + doubleValue + " " + stringValue);
    }

}
