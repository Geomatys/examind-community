/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.api;

import java.util.Date;
import java.util.concurrent.Callable;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConstellationSchedulerException;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface CstlScheduler {

    void start() throws ConstellationSchedulerException;

    void addJobListener(CstlJobListener Listener) throws ConstellationSchedulerException;

    void scheduleJobNow(String title, Integer taskParameterId, Integer userId, Callable<ParameterValueGroup> process) throws ConstellationSchedulerException;

    void scheduleJobNow(String title, Integer taskParameterId, Integer userId, TaskParameter process) throws ConstellationSchedulerException;

    String scheduleCronJob(String title, Integer taskParameterId, Integer userId, String cronExp, Date endDate, TaskParameter taskParameter) throws ConstellationSchedulerException;

    void interrupt(String key) throws ConstellationSchedulerException; // TODO param type

    boolean deleteJob(String key) throws ConstellationSchedulerException; // TODO param type

    void shutdown(boolean b) throws ConstellationSchedulerException;
}
