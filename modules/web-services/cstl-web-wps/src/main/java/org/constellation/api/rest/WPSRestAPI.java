/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.api.rest;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.process.Registry;
import org.constellation.dto.service.config.wps.Process;
import org.constellation.dto.process.RegistryList;
import com.examind.wps.util.WPSUtils;
import com.examind.wps.util.WPSConfigurationUtils;
import org.constellation.dto.service.config.wps.Processes;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import org.opengis.metadata.Identifier;
import org.opengis.util.NoSuchIdentifierException;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RESTful API for WPS services configuration.
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class WPSRestAPI extends AbstractRestAPI {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.api.rest");

    @Autowired
    private IServiceBusiness serviceBusiness;

    /**
     * Returns the list of all supported processes for WPS service.
     * @return {code List} of pojo
     */
    @RequestMapping(value="/processes",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getAllProcess() {

        final List<Registry> results = new ArrayList<>();

        for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext();) {
            final ProcessingRegistry processingRegistry = it.next();
            Iterator<? extends Identifier> iterator = processingRegistry
                    .getIdentification().getCitation().getIdentifiers()
                    .iterator();
            final Registry registry = new Registry(iterator.next().getCode());

            final List<org.constellation.dto.process.Process> processes = new ArrayList<>();
            for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                if (WPSUtils.isSupportedProcess(descriptor)) {
                    final org.constellation.dto.process.Process dto = new org.constellation.dto.process.Process(descriptor.getIdentifier().getCode());
                    if (descriptor.getProcedureDescription() != null) {
                        dto.setDescription(descriptor.getProcedureDescription().toString());
                    }
                    processes.add(dto);
                }
            }
            registry.setProcesses(processes);
            if(!processes.isEmpty()) {
                results.add(registry);
            }
        }

        return new ResponseEntity(results, OK);
    }

    /**
     * Returns the list of processes for WPS service
     *
     * @param id the wps service instance identifier
     * @return {List} of pojo
     */
    @RequestMapping(value="/processes/{id}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getProcess(final @PathVariable("id") String id) {
        try {
            final ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration("WPS", id);
            final List<Registry> results = new ArrayList<>();
            Processes servProcesses = context.getProcesses();
            if (Boolean.TRUE.equals(servProcesses.getLoadAll()) ) {
                return getAllProcess();
            } else {
                for (ProcessFactory pFacto : servProcesses.getFactory()) {
                    final Registry registry = new Registry(pFacto.getAutorityCode());
                    final List<org.constellation.dto.process.Process> processes = new ArrayList<>();

                    final ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory(pFacto.getAutorityCode());
                    if (pFacto.getLoadAll()) {
                        for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                            if (WPSUtils.isSupportedProcess(descriptor)) {
                                final org.constellation.dto.process.Process dto = new org.constellation.dto.process.Process(descriptor.getIdentifier().getCode());
                                if (descriptor.getProcedureDescription() != null) {
                                    dto.setDescription(descriptor.getProcedureDescription().toString());
                                }
                                processes.add(dto);
                            }
                        }
                    } else {
                        final List<Process> list = pFacto.getInclude().getProcess();
                        for (Process p : list) {
                            try {
                                final ProcessDescriptor descriptor = processingRegistry.getDescriptor(p.getId());
                                final org.constellation.dto.process.Process dto = new org.constellation.dto.process.Process(descriptor.getIdentifier().getCode());
                                if (descriptor.getProcedureDescription() != null) {
                                    dto.setDescription(descriptor.getProcedureDescription().toString());
                                }
                                processes.add(dto);
                            } catch (NoSuchIdentifierException ex) {
                                LOGGER.log(Level.WARNING, "Unable to find a process named:" + p.getId() + " in factory " + pFacto.getAutorityCode(), ex);
                            }
                        }
                    }
                    registry.setProcesses(processes);
                    if(!processes.isEmpty()) {
                        results.add(registry);
                    }
                }
                return new ResponseEntity(results, OK);
            }
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Add processes list for WPS service.
     *
     * @param id the wps instance identifier
     * @param registries pojo
     */
    @Transactional
    @RequestMapping(value="/processes/{id}",method = PUT,produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity addProcess(final @PathVariable("id") String id, @RequestBody final RegistryList registries) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration("WPS", id);
            context = WPSConfigurationUtils.addProcessToContext(context, registries);

            // save context
            serviceBusiness.configure("WPS", id, null, context);

            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Remove authority for WPS service.
     *
     * @param id the wps instance service identifier
     * @param code the authority code
     */
    @Transactional
    @RequestMapping(value="/processes/{id}/authority/{code}",method = DELETE)
    public ResponseEntity removeAuthority(final @PathVariable("id") String id, final @PathVariable("code") String code) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final ProcessContext context  = (ProcessContext) serviceBusiness.getConfiguration("WPS", id);
            final Processes servProcesses = context.getProcesses();
            if (Boolean.TRUE.equals(servProcesses.getLoadAll()) ) {
                servProcesses.setLoadAll(Boolean.FALSE);

                for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext();) {
                    final ProcessingRegistry processingRegistry = it.next();
                    final String name = processingRegistry
                            .getIdentification().getCitation().getIdentifiers()
                            .iterator().next().getCode();
                    if (!name.equals(code)) {
                        servProcesses.getFactory().add(new ProcessFactory(name, Boolean.TRUE));
                    }
                }

            } else {
                context.removeProcessFactory(code);
            }

            // save context
            serviceBusiness.configure("WPS", id, null, context);

            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Remove single process for WPS service.
     *
     * @param id the wps instance identifier
     * @param code the authority code
     * @param processId the process identifier
     */
    @Transactional
    @RequestMapping(value="/processes/{id}/process/{code}/{pid}",method = DELETE)
    public ResponseEntity removeProcess(final @PathVariable("id") String id, final @PathVariable("code") String code, final @PathVariable("pid") String processId) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration("WPS", id);

            context = WPSConfigurationUtils.removeProcessFromContext(context, code, processId);

            // save context
            serviceBusiness.configure("WPS", id, null, context);

            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/processes/external",method = GET,produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getExternalProcessList(@RequestParam(name="wpsUrl") final String url) {
        try {
            final List<org.constellation.dto.process.Process> processes = new ArrayList<>();
            final WebProcessingClient client = new WebProcessingClient(new URL(url));
            WPSProcessingRegistry registry = new WPSProcessingRegistry(client);
            for(final ProcessDescriptor desc : registry.getDescriptors()){
                if(desc.getIdentifier() == null) continue;
                final String identifier = desc.getIdentifier().getCode();
                final String title = desc.getDisplayName() != null ? desc.getDisplayName().toString() : "";
                final String description = desc.getProcedureDescription() != null ? desc.getProcedureDescription().toString() : "";
                final org.constellation.dto.process.Process proc = new org.constellation.dto.process.Process(identifier,title,description);
                processes.add(proc);
            }
            return new ResponseEntity(processes, OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }
}
