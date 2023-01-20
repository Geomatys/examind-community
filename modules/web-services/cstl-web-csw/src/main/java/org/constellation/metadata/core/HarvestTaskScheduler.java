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

package org.constellation.metadata.core;

import org.constellation.exception.ConstellationException;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMailBusiness;
import org.constellation.dto.service.config.csw.HarvestTask;
import org.constellation.dto.service.config.csw.HarvestTasks;
import org.constellation.metadata.harvest.CatalogueHarvester;
import org.constellation.ws.CstlServiceException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class HarvestTaskScheduler {

    /**
     * use for debugging purpose
     */
    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata");

    /**
     * The name of the harvest task file
     */
    private static final String HARVEST_TASK_FILE_NAME =  "HarvestTask.xml";

    /**
     * Mail service bean. Inject via SpringHelper because HarvestTaskScheduler is not
     * in Spring context.
     */
    private final IMailBusiness mailService = SpringHelper.getBean(IMailBusiness.class).orElse(null);

    /**
     * A unMarshaller to get object from harvested resource.
     */
    private final MarshallerPool marshallerPool;

    private final Path configDir;

    /**
     * A list of scheduled Task (used in close method).
     */
    private final List<Timer> schreduledTask = new ArrayList<>();

    private final CatalogueHarvester catalogueHarvester;

    public HarvestTaskScheduler(final Path configDir, final CatalogueHarvester catalogueHarvester) {
        MarshallerPool candidate = null;
        try {
            candidate = new MarshallerPool(JAXBContext.newInstance(HarvestTasks.class), null);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        this.marshallerPool     = candidate;
        this.configDir          = configDir;
        this.catalogueHarvester = catalogueHarvester;
        initializeHarvestTask();
    }

    /**
     * Restore all the periodic Harvest task from the configuration file "HarvestTask.xml".
     *
     * @param configDirectory The configuration directory containing the file "HarvestTask.xml"
     */
    private void initializeHarvestTask() {
        try {
            // we get the saved harvest task file
            final Path f = configDir.resolve(HARVEST_TASK_FILE_NAME);
            if (Files.exists(f)) {
                HarvestTasks tasks = readHarvestTask(f);
                if (tasks != null) {
                    final Timer t = new Timer();
                    for (HarvestTask task : tasks.getTask()) {
                        final AsynchronousHarvestTask at = new AsynchronousHarvestTask(task.getSourceURL(),
                                task.getResourceType(),
                                task.getMode(),
                                task.getEmails());
                        //we look for the time passed since the last harvest
                        final long time = System.currentTimeMillis() - task.getLastHarvest();

                        long delay = 2000;
                        if (time < task.getPeriod()) {
                            delay = task.getPeriod() - time;
                        }

                        t.scheduleAtFixedRate(at, delay, task.getPeriod());
                        schreduledTask.add(t);
                    }
                }
            } else {
                LOGGER.info("no Harvest task found (optionnal)");
            }

        } catch (JAXBException e) {
            LOGGER.info("JAXB Exception while unmarshalling the file HarvestTask.xml");
        } catch (IOException e) {
            LOGGER.info("IO Exception while reading the file HarvestTask.xml");
        }
    }

    /**
     * Save a periodic Harvest task into the specific configuration file "HarvestTask.xml".
     * This is made in order to restore the task when the server is shutdown and then restart.
     *
     * @param sourceURL  The URL of the source to harvest.
     * @param resourceType The type of the resource.
     * @param mode The type of the source: 0 for a single record (ex: an xml file) 1 for a CSW service.
     * @param emails A list of mail addresses to contact when the Harvest is done.
     * @param period The time between each Harvest.
     * @param lastHarvest The time of the last task launch.
     */
    private void saveSchreduledHarvestTask(final String sourceURL, final String resourceType, final int mode, final List<String> emails, final long period, final long lastHarvest) {
        final HarvestTask newTask = new HarvestTask(sourceURL, resourceType, mode, emails, period, lastHarvest);
        final Path f              = configDir.resolve(HARVEST_TASK_FILE_NAME);
        try {

            HarvestTasks tasks = new HarvestTasks();
            //read existing
            if (Files.exists(f)) {
                tasks = readHarvestTask(f);
            }

            if (tasks != null) {
                //add task
                tasks.addTask(newTask);
                writeHarvestTask(f, tasks);
            }

        } catch (IOException ex) {
            LOGGER.severe("unable to create a file for schreduled harvest task");
        } catch (JAXBException ex) {
            LOGGER.severe("A JAXB exception occurs when trying to marshall the shreduled harvest task");
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Update the Harvest task file by recording the last Harvest date of a task.
     * This is made in order to avoid the systematic launch of all the task when the CSW start.
     *
     * @param sourceURL Used as the task identifier.
     * @param lastHarvest a long representing the last time where the task was launch
     */
    private void updateSchreduledHarvestTask(final String sourceURL, final long lastHarvest) {
        final Path f              = configDir.resolve(HARVEST_TASK_FILE_NAME);
        try {
            if (Files.exists(f)) {
                final HarvestTasks tasks = readHarvestTask(f);
                if (tasks != null) {
                    final HarvestTask task   = tasks.getTaskFromSource(sourceURL);
                    if (task != null) {
                        task.setLastHarvest(lastHarvest);
                        writeHarvestTask(f, tasks);
                    }
                }
            } else {
                LOGGER.severe("There is no Harvest task file to update");
            }

        } catch (JAXBException ex) {
            LOGGER.severe("A JAXB exception occurs when trying to marshall the shreduled harvest task (update)");
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (IOException ex) {
            LOGGER.severe("A IO exception occurs when trying to read the shreduled harvest task (update)");
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private HarvestTasks readHarvestTask(Path f) throws IOException, JAXBException {
        HarvestTasks tasks = null;
        try (InputStream stream = Files.newInputStream(f)) {
            final Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();
            final Object obj = unmarshaller.unmarshal(stream);
            marshallerPool.recycle(unmarshaller);
            if (obj instanceof HarvestTasks) {
                tasks = (HarvestTasks) obj;
            } else {
                LOGGER.severe("Bad data type for file HarvestTask.xml");
            }
        }
        return tasks;
    }

    private void writeHarvestTask(Path f, HarvestTasks tasks) throws IOException, JAXBException {
        //write replacing existing
        try (OutputStream output = Files.newOutputStream(f, CREATE, WRITE, TRUNCATE_EXISTING)) {
            final Marshaller marshaller = marshallerPool.acquireMarshaller();
            marshaller.marshal(tasks, output);
            marshallerPool.recycle(marshaller);
        }
    }

    public void newAsynchronousHarvestTask(long period, final String sourceURL, final String resourceType, final int mode, final List<String> emails) {
        final Timer t = new Timer();
        final TimerTask harvestTask = new AsynchronousHarvestTask(sourceURL, resourceType, mode, emails);
        //we launch only once the harvest
        if (period == 0) {
            t.schedule(harvestTask, 1000);

            //we launch the harvest periodically
        } else {
            t.scheduleAtFixedRate(harvestTask, 1000, period);
            schreduledTask.add(t);
            saveSchreduledHarvestTask(sourceURL, resourceType, mode, emails, period, System.currentTimeMillis() + 1000);
        }
    }

    public void destroy() {
        for (Timer t : schreduledTask) {
            t.cancel();
        }
    }

    /**
     * A task launching an harvest periodically.
     */
    class AsynchronousHarvestTask extends TimerTask {

        /**
         * The harvest mode SINGLE(0) or CATALOGUE(1)
         */
        private final int mode;

        /**
         * The URL of the data source.
         */
        private final String sourceURL;

        /**
         * The type of the resource (for single mode).
         */
        private final String resourceType;

        /**
         * A list of email addresses.
         */
        private final List<String> emails = new ArrayList<>();

        /**
         * Build a new Timer which will Harvest the source periodically.
         *
         */
        public AsynchronousHarvestTask(final String sourceURL, final String resourceType, final int mode, final List<String> emails) {
            this.sourceURL    = sourceURL;
            this.mode         = mode;
            this.resourceType = resourceType;
            if (emails != null) {
                this.emails.addAll(emails);
            }
        }

        /**
         * This method is launch when the timer expire.
         */
        @Override
        public void run() {
            LOGGER.log(Level.INFO, "launching harvest on:{0}", sourceURL);
            try {
                int[] results;
                if (mode == 0) {
                    results = catalogueHarvester.harvestSingle(sourceURL, resourceType);
                } else {
                    results = catalogueHarvester.harvestCatalogue(sourceURL);
                }

                updateSchreduledHarvestTask(sourceURL, System.currentTimeMillis());
                /*

                 TODO does we have to send a HarvestResponseType or a custom report to the mails addresses?

                 TransactionSummaryType summary = new TransactionSummaryType(results[0],
                                                                            results[1],
                                                                            results[2], null);
                 TransactionResponseType transactionResponse = new TransactionResponseType(summary, null, "2.0.2");
                 HarvestResponseType response = new HarvestResponseType(transactionResponse);
                 */

                final StringBuilder report = new StringBuilder("Harvest report:\n");
                report.append("From: ").append(sourceURL).append('\n');
                report.append("Inserted: ").append(results[0]).append('\n');
                report.append("Updated: ").append(results[1]).append('\n');
                report.append("Deleted: ").append(results[2]).append('\n');
                report.append("at ").append(new Date(System.currentTimeMillis()));

                mailService.send("Harvest report", report.toString(), emails);
            } catch (CstlServiceException ex) {
                LOGGER.log(Level.SEVERE, "Constellation exception:{0}", ex.getMessage());
            } catch (ConstellationException ex) {
                LOGGER.log(Level.SEVERE, "ConstellationException exception:{0}", ex.getMessage());
            }
        }
    }
}
