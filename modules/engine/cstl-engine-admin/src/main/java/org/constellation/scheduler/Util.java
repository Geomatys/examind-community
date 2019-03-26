/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.opengis.parameter.InvalidParameterValueException;
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
        } catch (InvalidParameterValueException | MalformedURLException | CapabilitiesException ex) {
            throw new ConstellationException(ex);
        }
        if(desc == null){
            throw new ConstellationException("No Process for id : {" + authority + "}"+code+" has been found");
        }
        return desc;
    }
}
