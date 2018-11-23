/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2018, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.scheduler;

import java.util.UUID;
import static org.geotoolkit.processing.quartz.ProcessJob.KEY_PROCESS;
import org.geotoolkit.processing.quartz.ProcessJobDetail;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class QuartzProcessJobDetails extends ProcessJobDetail {

    public QuartzProcessJobDetails(final String factoryId, final String processId, final ParameterValueGroup parameters){
        this(factoryId+"."+processId+"-"+UUID.randomUUID(), null, factoryId, processId, parameters);
    }

    public QuartzProcessJobDetails(final org.geotoolkit.process.Process process){
        this(createJobName(process), null, extractProcessID(process), extractFactoryName(process), process.getInput() );
        getJobDataMap().put(KEY_PROCESS, process);
    }

    public QuartzProcessJobDetails(final String name, final String group, final String factoryId, final String processId, final ParameterValueGroup parameters) {
        super(name, group, factoryId, processId, parameters, QuartzProcessJob.class);
    }

}
