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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.client.CapabilitiesException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author guilhem
 */
public class Util {

    public static ParameterValueGroup readTaskParametersFromJSON(final TaskParameter taskParameter, final ProcessDescriptor processDesc)
            throws ConfigurationException {

        final ParameterDescriptorGroup idesc = processDesc.getInputDescriptor();

        ParameterValueGroup params;
        try {
            params = (ParameterValueGroup) ParamUtilities.readParameterJSON(taskParameter.getInputs(), idesc);
        } catch (IOException e) {
            throw new ConfigurationException("Fail to parse input parameter as JSON : "+e.getMessage(), e);
        }

        return params;
    }

    public static ProcessDescriptor getDescriptor(final String authority, final String code) throws ConstellationException {
        final ProcessDescriptor desc;
        try {
            if(authority.startsWith("http")) {
                final WebProcessingClient client = new WebProcessingClient(new URL(authority));
                desc = new WPSProcessingRegistry(client).getDescriptor(code);
            }else {
                desc = ProcessFinder.getProcessDescriptor(authority, code);
            }
        } catch (NoSuchIdentifierException ex) {
            throw new ConstellationException("No Process for id : {" + authority + "}"+code+" has been found");
        } catch (RuntimeException | MalformedURLException | CapabilitiesException  ex) {
            throw new ConstellationException(ex);
        }
        if(desc == null){
            throw new ConstellationException("No Process for id : {" + authority + "}"+code+" has been found");
        }
        return desc;
    }
}
