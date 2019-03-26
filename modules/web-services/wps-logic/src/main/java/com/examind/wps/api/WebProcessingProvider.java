package com.examind.wps.api;

import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Describes a type of {@link WebProcessingComponent}, and allow for creation of
 * components of the described type by providing a managed configuration.
 *
 * The aim is that configuration contains information about a set of processes
 * to handle in the WPS, and the component created using {@link #open(org.opengis.parameter.ParameterValueGroup)
 * }
 * will be in charge of the processes lifecycle.
 *
 * Note: The provider is identified by its configuration, through
 * {@link #getOpenParameters() }.
 *
 * @author Alexis Manin (Geomatys)
 */
public interface WebProcessingProvider {

    /**
     * Create a new component using given configuration.
     *
     * @param config A configuration compliant with this provider {@link #getOpenParameters()
     * }.
     * @return A new component managing a set of processes depicted in input
     * configuration.
     */
    WebProcessingComponent open(final ParameterValueGroup config);

    /**
     * get a detailed description of the configuration needed to initialize a
     * new set of processes.
     *
     * @return Description of the required configuration to open a new
     * component.
     */
    ParameterDescriptorGroup getOpenParameters();
}
