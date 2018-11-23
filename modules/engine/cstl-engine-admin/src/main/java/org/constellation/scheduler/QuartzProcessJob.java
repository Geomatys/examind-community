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

import java.net.MalformedURLException;
import java.net.URL;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.client.CapabilitiesException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.quartz.ProcessJob;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import org.opengis.util.NoSuchIdentifierException;
import org.quartz.JobExecutionException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class QuartzProcessJob extends ProcessJob {

    @Override
    protected ProcessDescriptor getProcessDescriptor(String factoryId, String processId) throws JobExecutionException {
        try {
            if (factoryId != null && factoryId.startsWith("http")) {
                final WebProcessingClient client = new WebProcessingClient(new URL(factoryId));
                return new WPSProcessingRegistry(client).getDescriptor(processId);
            } else {
                return ProcessFinder.getProcessDescriptor(factoryId, processId);
            }
        } catch (RuntimeException | MalformedURLException | CapabilitiesException | NoSuchIdentifierException ex) {
            throw new JobExecutionException(ex);
        }
    }



}
