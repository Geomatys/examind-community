/*
 *    Examind community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.process.admin.yamlReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.opengis.parameter.*;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.processing.ForwardProcessListener;

public class ProcessFromYamlProcess extends AbstractCstlProcess {

    /**
     * ServiceBusiness used for OGC service reference
     */
    @Autowired(required = false)
    private IServiceBusiness serviceBusiness;

    private static final String PROCESS_FACTORY_NAME = "factory_name";
    private static final String PROCESS_NAME_PARAMETER = "process_name";

    public ProcessFromYamlProcess(ProcessDescriptor desc, ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executing process from yaml reader");

        final Path yamlPath = inputParameters.getValue(ProcessFromYamlProcessDescriptor.YAML_PATH);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            // Retrieve and map config from yaml file.
            Map configMap = mapper.readValue(yamlPath.toFile(), Map.class);

            String factoryName = (String) configMap.get(PROCESS_FACTORY_NAME);
            String processName = (String) configMap.get(PROCESS_NAME_PARAMETER);
            // Setting process name parameter to create the correct process type.
            ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(factoryName, processName);
            ParameterValueGroup in = desc.getInputDescriptor().createValue();

            final List<GeneralParameterDescriptor> descriptors = in.getDescriptor().descriptors();

            for (GeneralParameterDescriptor genParamDesc : descriptors) {
                handleParameter(genParamDesc, configMap, in);
            }

            Process process = desc.createProcess(in);
            process.addListener(new ForwardProcessListener(this, 0, 100));
            ParameterValueGroup results = process.call();
            String jsonResults = ParamUtilities.writeParameterJSON(results, true);
            outputParameters.getOrCreate(ProcessFromYamlProcessDescriptor.PROCESS_OUTPUT).setValue(jsonResults);

        } catch (IOException | NoSuchIdentifierException e) {
            throw new ProcessException("An error occured while executing the ProcessFromYamlProcess", this, e);
        }
    }

    private void handleParameter(GeneralParameterDescriptor genParamDesc, Map configMap, ParameterValueGroup in) throws ProcessException  {
        final String paramCode  = genParamDesc.getName().getCode();
        if (genParamDesc instanceof ParameterDescriptor paramDesc) {
            final Class valueClass = paramDesc.getValueClass();
            final Object paramValue = configMap.get(paramCode);

            if (paramValue == null) return ;
            final Class paramClass = paramValue.getClass();
            if (valueClass == String.class) {
                if (paramClass == String.class) {
                    final String configValue = (String) paramValue;
                    in.parameter(paramCode).setValue(configValue);
                } else if (List.class.isAssignableFrom(paramClass)) {
                    // Little trick to add multiple time the same value to the process.
                    List<Object> valueList = (List<Object>) paramValue;
                    for (Object value : valueList) {
                        ParameterValue<String> parameterValue = paramDesc.createValue();
                        parameterValue.setValue(value.toString()); // toString here is not redundant as the value might be an Integer for example.
                        in.values().add(parameterValue);
                    }
                }
            // special case for a know type. TODO hande this generically
            } else if (valueClass == ServiceProcessReference.class) {
                if (paramClass == LinkedHashMap.class) {
                    if (serviceBusiness == null) throw new ProcessException("Service reference not available", this);
                    LinkedHashMap value = (LinkedHashMap) paramValue;
                    Collection<LinkedHashMap> collection = value.values();
                    for (LinkedHashMap linkedValue : collection) {
                        final String type = (String) linkedValue.get("type");
                        final String identifier = (String) linkedValue.get("identifier");
                        ServiceComplete service = serviceBusiness.getServiceByIdentifierAndType(type, identifier);
                        // not null if a service has been found.
                        if (service != null) {
                            ServiceProcessReference serviceProcessReference = new ServiceProcessReference(service.getId(), service.getType(), service.getIdentifier());
                            ParameterValue<ServiceProcessReference> parameterValue = paramDesc.createValue();
                            parameterValue.setValue(serviceProcessReference);
                            in.values().add(parameterValue);
                        }
                    }
                }
            } else if (valueClass == Boolean.class) {
                final Boolean configValue = (Boolean) paramValue;
                in.parameter(paramCode).setValue(configValue);
            } else if (valueClass == Double.class) {
                final Double configValue = (Double) paramValue;
                in.parameter(paramCode).setValue(configValue);
            } else {
                throw new ProcessException("Parameter type not yet handled:" + valueClass.getName(), this);
            }
        } else if (genParamDesc instanceof ParameterDescriptorGroup paramDesGrp) {
            final List<GeneralParameterDescriptor> descriptors = paramDesGrp.descriptors();
            boolean more = true;
            int cpt = 0;
            while (more) {
                final Object subParamValue;
                if (configMap.containsKey(paramCode)) {
                    subParamValue = configMap.get(paramCode);
                    // only one occurence
                    more = false;
                } else {
                    subParamValue = configMap.get(paramCode+ '_' + cpt);
                }
                if (subParamValue instanceof Map subConfigMap) {
                    ParameterValueGroup subIn  = in.addGroup(paramCode);
                    for (GeneralParameterDescriptor paramDesc : descriptors) {
                        handleParameter(paramDesc, subConfigMap, subIn);
                    }
                } else if (subParamValue != null) {
                    throw new ProcessException("Malformed yaml, expecting a group for:" + paramCode, this);
                } else {
                    more = false;
                }
                cpt++;
            }
        }
    }
}
