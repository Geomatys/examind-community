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

package org.constellation.process.dynamic;

import org.constellation.process.ChainProcessRetriever;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConstellationException;
import org.constellation.process.dynamic.cwl.RunCWLDescriptor;
import org.constellation.process.dynamic.docker.RunDockerDescriptor;
import org.constellation.process.dynamic.proactive.RunProactiveDescriptor;
import org.geotoolkit.process.ProcessingRegistry;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Element;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExamindDynamicProcessFactory implements ProcessingRegistry {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.process");

    /**Factory name*/
    public static final String NAME = "examind-dynamic";
    public static final DefaultServiceIdentification IDENTIFICATION;

    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    @Override
    public List<ProcessDescriptor> getDescriptors() {
        return new ArrayList<>(getAllDescriptors().values());
    }

    @Override
    public List<String> getNames() {
        return new ArrayList<>(getAllDescriptors().keySet());
    }

    @Override
    public ProcessDescriptor getDescriptor(String name) throws NoSuchIdentifierException {
        ProcessDescriptor desc = getAllDescriptors().get(name);
        if (desc == null) {
            throw new NoSuchIdentifierException("Unable to find a process:" + name, name);
        }
        return desc;
    }

    private Map<String, ProcessDescriptor> getAllDescriptors() {
        final Map<String, ProcessDescriptor> descriptors = new HashMap<>();
        descriptors.putAll(findDynamicDescriptors());
        descriptors.putAll(findChainDescriptors());
        return descriptors;
    }

    private Map<String, ProcessDescriptor> findDynamicDescriptors() {
        final Map<String, ProcessDescriptor> descriptors = new HashMap<>();
        final ParameterBuilder BUILDER = new ParameterBuilder();
        try {
            final List<Chain> chains = ChainProcessRetriever.getChainModels();
            for (Chain chain : chains) {
                AbstractDynamicDescriptor dynDesc = null;
                for (Element element : chain.getElements()) {
                    if (element instanceof ElementProcess) {
                        ElementProcess ep = (ElementProcess) element;

                        // TODO find a way to make this generic
                        if (ep.getCode().startsWith("docker.run")) {
                            dynDesc = new RunDockerDescriptor(ep.getCode());
                        } else if (ep.getCode().startsWith("proactive.run")) {
                            dynDesc = new RunProactiveDescriptor(ep.getCode());
                        } else if (ep.getCode().startsWith("cwl.run")) {
                            dynDesc = new RunCWLDescriptor(ep.getCode());
                        }
                    }
                }
                if (dynDesc != null) {
                    for (Parameter in : chain.getInputs()) {
                        dynDesc.addNewInput(new ExtendedParameterDescriptor(in.getCode(), null, in.getRemarks(), in.getMinOccurs(), in.getMaxOccurs(), in.getType().getRealClass(), null, null, null));
                    }
                    for (Parameter out : chain.getOutputs()) {
                        dynDesc.addNewOutput(new ExtendedParameterDescriptor(out.getCode(), null, out.getRemarks(), out.getMinOccurs(), out.getMaxOccurs(), out.getType().getRealClass(), null, null, null));
                    }
                    descriptors.put(dynDesc.getIdentifier().getCode(), dynDesc);
                }


            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Exception while retrieving chain process", ex);
        }
        return descriptors;
    }


    private Map<String, ProcessDescriptor> findChainDescriptors() {
        final Map<String, ProcessDescriptor> descriptors = new HashMap<>();
        try {
            final List<ProcessDescriptor> chains = ChainProcessRetriever.getChainDescriptors(NAME);
            for (ProcessDescriptor chain : chains) {
                descriptors.put(chain.getIdentifier().getCode(), chain);
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Exception while retrieving chain process", ex);
        }
        return descriptors;
    }
}
