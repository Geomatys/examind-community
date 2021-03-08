/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package com.examind.process.admin.yamlReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.sis.parameter.DefaultParameterValue;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.opengis.parameter.*;
import org.opengis.util.NoSuchIdentifierException;


import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ProcessFromYamlProcess extends AbstractCstlProcess {

    /**
     * ServiceBusiness used for provider GUI editors data
     */
    @Inject
    private IServiceBusiness serviceBusiness;

    private static final String PROCESS_FACTORY_NAME = "factory name";
    private static final String PROCESS_NAME_PARAMETER = "process name";
    private static final String SERVICE_TYPE = "service type";

    public ProcessFromYamlProcess(ProcessDescriptor desc, ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executing process from yaml reader");

        final String yamlPath = inputParameters.getValue(ProcessFromYamlProcessDescriptor.YAML_PATH);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        File file = new File(yamlPath);
        try {
            // Retrieve and map config from yaml file.
            Map configMap = mapper.readValue(file, Map.class);

            String factoryName = (String) configMap.get(PROCESS_FACTORY_NAME);
            String processName = (String) configMap.get(PROCESS_NAME_PARAMETER);
            String serviceType = (String) configMap.get(SERVICE_TYPE);
            // Setting process name parameter to create the correct process type.
            ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(factoryName, processName);
            ParameterValueGroup in = desc.getInputDescriptor().createValue();

            final List<GeneralParameterDescriptor> descriptors = in.getDescriptor().descriptors();

            for (GeneralParameterDescriptor genParamDesc : descriptors) {
                ParameterDescriptor parameterDescriptor = (ParameterDescriptor) genParamDesc;

                final Class valueClass = parameterDescriptor.getValueClass();

                if (configMap.get(parameterDescriptor.getName().getCode()) != null) {
                    if (valueClass==String.class) {
                        if (configMap.get(parameterDescriptor.getName().getCode()).getClass()==String.class) {
                            final String configValue = (String) configMap.get(parameterDescriptor.getName().getCode());
                            in.parameter(parameterDescriptor.getName().getCode()).setValue(configValue);
                        } else if (configMap.get(parameterDescriptor.getName().getCode()).getClass() == java.util.ArrayList.class) {
                            // Little trick to add multiple time the same value to the process.
                            List<Object> valueList = (List<Object>) configMap.get(parameterDescriptor.getName().getCode());
                            for (Object value : valueList) {
                                ParameterValue<String> parameterValue = new DefaultParameterValue(parameterDescriptor);
                                parameterValue.setValue(value.toString()); // toString here is not redundant as the value might be an Integer for example.
                                in.values().add(parameterValue);
                            }
                        }
                    } else if (valueClass==ServiceProcessReference.class) {
                        if (configMap.get(parameterDescriptor.getName().getCode()).getClass()==LinkedHashMap.class) {
                            LinkedHashMap value = (LinkedHashMap) configMap.get(parameterDescriptor.getName().getCode());
                            Collection<LinkedHashMap> collection = value.values();
                            for (LinkedHashMap linkedValue : collection) {
                                final String type = (String) linkedValue.get("type");
                                final String identifier = (String) linkedValue.get("identifier");
                                ServiceComplete service = serviceBusiness.getServiceByIdentifierAndType(type, identifier);
                                // not null if a service has been found.
                                if (service != null) {
                                    ServiceProcessReference serviceProcessReference = new ServiceProcessReference(service.getId(), service.getType(), service.getIdentifier());
                                    ParameterValue<ServiceProcessReference> parameterValue = new DefaultParameterValue(parameterDescriptor);
                                    parameterValue.setValue(serviceProcessReference);
                                    in.values().add(parameterValue);
                                }
                            }
                        }
                    } else if (valueClass==Boolean.class) {
                        final Boolean configValue = (Boolean) configMap.get(parameterDescriptor.getName().getCode());
                        in.parameter(parameterDescriptor.getName().getCode()).setValue(configValue);
                    }
                }
            }

            Process process = desc.createProcess(in);
            process.call();

        } catch (IOException | NoSuchIdentifierException e) {
            throw new ProcessException("An error occured while executing the ProcessFromYamlProcess", this, e);
        }

        outputParameters.getOrCreate(ProcessFromYamlProcessDescriptor.PROCESS_OUTPUT).setValue(true);
    }
}