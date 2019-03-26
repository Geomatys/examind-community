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
package com.examind.wps.util;

import java.util.Iterator;
import org.constellation.dto.process.Registry;
import org.constellation.dto.process.RegistryList;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;

/**
 *
 * @author guilhem
 */
public class WPSConfigurationUtils {

    public static ProcessContext addProcessToContext(final ProcessContext context, final RegistryList registries) {

        if (Boolean.TRUE.equals(context.getProcesses().getLoadAll())) {
            // WPS already contains all the registries, nothing to do
        } else {
            for (Registry registry : registries.getRegistries()) {
                ProcessFactory factory = context.getProcessFactory(registry.getName());
                if (factory != null) {
                    if (Boolean.TRUE.equals(factory.getLoadAll())) {
                        //WPS already contains all the process of this registry, nothing to do
                    } else {
                        for (org.constellation.dto.process.Process process : registry.getProcesses()) {
                            if (factory.getInclude().contains(process.getId())) {
                                // WPS already contain the process
                            } else {
                                factory.getInclude().add(process);
                            }
                        }
                    }
                } else {
                    factory = new ProcessFactory(registry.getName(), false);
                    for (org.constellation.dto.process.Process process : registry.getProcesses()) {
                        factory.getInclude().add(process);
                    }
                    context.getProcessFactories().add(factory);
                }
            }
        }
        return context;
    }
    public static ProcessContext removeProcessFromContext(final ProcessContext context, final String authority, final String processId) {
        if (Boolean.TRUE.equals(context.getProcesses().getLoadAll())) {
            context.getProcesses().setLoadAll(Boolean.FALSE);

            for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext();) {
                final ProcessingRegistry processingRegistry = it.next();
                final String name = processingRegistry
                        .getIdentification().getCitation().getIdentifiers()
                        .iterator().next().getCode();
                if (!name.equals(authority)) {
                    if (isSupportedFactory(processingRegistry)) {
                        context.getProcessFactories().add(new ProcessFactory(name, Boolean.TRUE));
                    }
                } else {
                    final ProcessFactory newFactory = new ProcessFactory(name, Boolean.FALSE);
                    for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                        final String pid = descriptor.getIdentifier().getCode();
                        if (!pid.equals(processId) && com.examind.wps.util.WPSUtils.isSupportedProcess(descriptor)) {
                            newFactory.getInclude().add(pid);
                        }
                    }
                    context.getProcessFactories().add(newFactory);
                }
            }
        } else {
            final ProcessFactory factory = context.getProcessFactory(authority);
            if (factory != null) {
                if (Boolean.TRUE.equals(factory.getLoadAll())) {
                    factory.setLoadAll(Boolean.FALSE);
                    final ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory(authority);
                    for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                        final String pid = descriptor.getIdentifier().getCode();
                        if (!pid.equals(processId) && com.examind.wps.util.WPSUtils.isSupportedProcess(descriptor)) {
                            factory.getInclude().add(pid);
                        }
                    }
                } else {
                    factory.getInclude().remove(processId);
                }
            }
        }
        return context;
    }

    private static boolean isSupportedFactory(final ProcessingRegistry processingRegistry) {
        for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
            if (WPSUtils.isSupportedProcess(descriptor)) {
                return true;
            }
        }
        return false;
    }
}
