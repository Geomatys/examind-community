/*
 *    Examind Community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package com.examind.process.test;

import static com.examind.process.test.MultiThreadFailureDescriptor.THROW_EX;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.DismissProcessException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessListener;
import org.geotoolkit.processing.AbstractProcess;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MultiThreadFailureProcess extends AbstractCstlProcess {

    private static final Logger LOGGER = Logger.getLogger("com.examind.process.test");

    public MultiThreadFailureProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        fireProcessStarted("Start process");
        boolean throwExForFail = inputParameters.getMandatoryValue(THROW_EX);
        boolean willFailFirst;
        boolean willFailAfter;
        List<FallingThread> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            willFailFirst = (i == 2);
            willFailAfter = (i == 3);
            FallingThread t = new FallingThread(i, willFailFirst, willFailAfter, this, throwExForFail);
            t.start();
            threads.add(t);
        }

        boolean running = true;
        while (running) {
            running = false;
            for (FallingThread ft : threads) {
                if (ft.isAlive()) running = true;
            }
        }
        if (throwExForFail) {
            throw new ProcessException("Parent failure", this);
        }

        fireProcessCompleted("Process complete");
    }

    private static class FallingThread extends Thread {
        private final boolean throwExForFail;
        private final int numthread;
        private final boolean fail;
        private final boolean fail2;
        private final AbstractProcess parent;
        
        public FallingThread(int numthread, boolean fail, boolean fail2, AbstractProcess parent, boolean throwExForFail) {
            this.fail = fail;
            this.fail2 = fail2;
            this.parent = parent;
            this.numthread = numthread;
            this.throwExForFail = throwExForFail;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 20; i++) {
                    fireProgressing("Child " + numthread + ": " + i + " iteration.", 1.0f);
                    if (fail && i == 10) {
                        fireProcessFailed("One thread failure", new RuntimeException("Failure in child:" + numthread));
                    } else if (fail2 && i == 15) {
                        fireProcessFailed("One thread second failure", new RuntimeException("Second Failure in child:" + numthread));
                    }
                    waitAndcheckCanceled(1000);
                }
            } catch (Exception ex) {
                LOGGER.warning("Child " + numthread + ": " + ex.getMessage());
            }
        }

        private void waitAndcheckCanceled(int ms) throws ProcessException, InterruptedException {
            for (int i = 0; i < ms; i = i + 100) {
                if (parent.isDimissed()) {
                    throw new DismissProcessException("Process has been dismissed.", parent);
                }
                Thread.sleep(100);
            }
        }

        protected void fireProcessFailed(final CharSequence task, final RuntimeException exception) {
            final ProcessEvent event = new ProcessEvent(parent, task, Float.NaN, exception);
            for (ProcessListener listener : parent.getListeners()) {
                listener.failed(event);
            }
            if (throwExForFail) {
                throw exception;
            }
        }

        protected void fireProgressing(final CharSequence task, final float progress) {
            LOGGER.info(task.toString());
            final ProcessEvent event = new ProcessEvent(parent, task, progress, (ParameterValueGroup)null);
            for (ProcessListener listener : parent.getListeners()) {
                listener.progressing(event);
            }
        }
    }

}
