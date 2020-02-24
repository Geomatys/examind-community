/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.process.service;

import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;


/**
 * Process used for testing the progress bar.
 * It will make several pause will increasing the progress.
 *
 * @author Guilhem Legal (Geomatys).
 */
public class ProgressTest extends AbstractCstlProcess {


    public ProgressTest(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }


    @Override
    protected void execute() throws ProcessException {

        try {
            fireProcessStarted("Start process");
            waitAndcheckCanceled(2000);
            fireProgressing("step 1", 20f, false);
            waitAndcheckCanceled(5000);
            fireProgressing("step 2", 40f, false);
            waitAndcheckCanceled(5000);
            fireProgressing("step 3", 60f, false);
            waitAndcheckCanceled(5000);
            fireProgressing("step 4", 80f, false);
            waitAndcheckCanceled(5000);
            fireProgressing("end", 100f, false);
            fireProcessCompleted("Process complete");

        } catch (InterruptedException ex) {
            throw new ProcessException("Process have been interrupted", this);
        }
    }

    private void waitAndcheckCanceled(int ms) throws ProcessException, InterruptedException {
        for (int i = 0; i<ms; i = i + 100) {
            stopIfDismissed();
            Thread.sleep(100);
        }
    }
}
