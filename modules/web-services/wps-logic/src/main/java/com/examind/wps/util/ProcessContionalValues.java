/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package com.examind.wps.util;

import com.examind.process.InputCompleterDescriptor;
import com.examind.wps.api.WPSException;
import static com.examind.wps.util.WPSUtils.fillProcessInputFromRequest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.constellation.process.ProcessUtils;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomays)
 */
public class ProcessContionalValues implements ConditionalValues {
    
    private final ProcessDescriptor descriptor;
    
    public ProcessContionalValues(ProcessDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public Map<String, Set<String>> autocomplete(String version, List<DataInput> dataInputs) throws WPSException {
        
        if (dataInputs == null) {
            dataInputs = new ArrayList<>();
        }
        
        ParameterDescriptorGroup inputDescriptor = descriptor.getInputDescriptor();
        final ParameterValueGroup in = inputDescriptor.createValue();
        
        final List<GeneralParameterDescriptor> processInputDesc = inputDescriptor.descriptors();
        //Fill input process with there default values
        for (final GeneralParameterDescriptor inputGeneDesc : processInputDesc) {

            if (inputGeneDesc instanceof ParameterDescriptor inputDesc) {
                final Object defValue = inputDesc.getDefaultValue();
                if (defValue != null) {
                    in.parameter(inputDesc.getName().getCode()).setValue(defValue);
                }
            }
        }

        // TODO ?
        List<Path> tempFiles = new ArrayList<>();
         
        //Fill process input with data from execute request.
        fillProcessInputFromRequest(descriptor, version, in, dataInputs, processInputDesc, tempFiles);

        //Give input parameter to the process
        final org.geotoolkit.process.Process process = descriptor.createProcess(in);
        
        // extract values
        Map<String, Set<String>> results = new HashMap<>();
        try {
            ParameterValueGroup output = process.call();
            for (GeneralParameterDescriptor desc : descriptor.getOutputDescriptor().descriptors()) {
                String inputName = desc.getName().getCode();
                if (desc.getMaximumOccurs() > 1) {
                    List<?> values = ProcessUtils.getMultipleValues(output, desc);
                    results.put(inputName, values.stream().map(o -> o.toString()).collect(Collectors.toSet()));
                } else {
                    Object value = output.parameter(desc.getName().getCode()).getValue();
                    Set<String> vals = value != null ? Set.of(value.toString()) : Set.of();
                    results.put(inputName, vals);
                }
            }
        } catch (ProcessException ex) {
            throw new WPSException("Error while requesting input completer process.", ex);
        }
        return results;
    }
    
}