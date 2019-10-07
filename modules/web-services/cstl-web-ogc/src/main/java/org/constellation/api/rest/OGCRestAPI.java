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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.business.ILayerBusiness;
import org.constellation.ws.IWSEngine;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.ServiceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.NotRunningServiceException;
import org.constellation.ogc.configuration.OGCConfigurer;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class OGCRestAPI {

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private ILayerBusiness layerBusiness;

    @Inject
    private IWSEngine wsengine;

    /**
     * Find and returns a i18n service {@link Instance}.
     *
     * @param serviceType The type of the service.
     * @param id the service identifier.
     * @param lang the language of current user
     *
     *
     * @return an {@link Instance} instance
     */
    @RequestMapping(value="/OGC/{spec}/{id}/{lang}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getInstance(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id,
            final @PathVariable("lang") String lang) {
        try {
            return new ResponseEntity(serviceBusiness.getI18nInstance(serviceType, id, lang), OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Find and returns a service {@link Instance}.
     *
     * @param serviceType The type of the service.
     * @param id the service identifier
     *
     * @return an {@link Instance} instance
     */
    @RequestMapping(value="/OGC/{spec}/{id}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getInstance(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            Integer serviceId = serviceBusiness.getServiceIdByIdentifierAndType(serviceType, id);
            return new ResponseEntity(getConfigurer(serviceType).getInstance(serviceId), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns list of service {@link Instance}(s) related to the {@link OGCConfigurer}
     * implementation.
     *
     * @param serviceType The type of the service.
     * @return the {@link Instance} list
     *
     */
    @RequestMapping(value="/OGC/{spec}/all", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getInstances(final @PathVariable("spec") String serviceType) {
        try {
            final OGCConfigurer configurer = getConfigurer(serviceType);
            final Set<Instance> instances = new HashSet<>();
            final Map<Integer, ServiceStatus> statusMap = serviceBusiness.getStatus(serviceType);
            for (final Integer key : statusMap.keySet()) {
                instances.add(configurer.getInstance(key));
            }
            return new ResponseEntity(new InstanceReport(instances), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    // TODO move elsewhere / rename
    @RequestMapping(value="/OGC/list", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listService() {
        try {
            final ServiceReport response = new ServiceReport(wsengine.getRegisteredServices());
            return new ResponseEntity(response, OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Creates a new service instance.
     *
     * @param spec      The type of the service.
     * @param metadata  The service metadata (can be null)
     *
     */
    @RequestMapping(value="/OGC/{spec}", method=PUT, consumes = MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addInstance(final @PathVariable("spec") String spec, final @RequestBody Details metadata) {
        try {
            Integer service = serviceBusiness.getServiceIdByIdentifierAndType(spec, metadata.getIdentifier());
            if(service != null) {
                return new ResponseEntity(AcknowlegementType.failure("Instance already created"), OK);
            }

            serviceBusiness.create(spec, metadata.getIdentifier(), null, metadata, null);

            return new ResponseEntity(AcknowlegementType.success(spec.toUpperCase() + " service \"" + metadata.getIdentifier() + "\" successfully created."), CREATED);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Starts a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/start", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity start(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceType, id);
            serviceBusiness.start(s.getId());
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" successfully started."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Stops a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/stop", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity stop(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceType, id);
            serviceBusiness.stop(s.getId());
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" successfully stopped."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Restarts a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/restart", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity restart(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceType, id);
            serviceBusiness.restart(s.getId());
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" successfully restarted."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Renames a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The current service identifier.
     * @param newId       The new service identifier.
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/rename", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity rename(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id, final @RequestParam String newId) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceType, id);
            serviceBusiness.rename(s.getId(), newId);
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" successfully renamed."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Deletes a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier.
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}", method=DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity delete(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            Integer sid = serviceBusiness.getServiceIdByIdentifierAndType(serviceType, id);
            layerBusiness.removeForService(sid);
            serviceBusiness.ensureExistingInstance(serviceType, id);
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceType, id);
            serviceBusiness.delete(s.getId());
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" successfully deleted."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns the configuration object of a service instance.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier.
     *
     * @return a configuration {@link Object} (depending on implementation)
     */
    @RequestMapping(value="/OGC/{spec}/{id}/config", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getConfiguration(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            return new ResponseEntity(serviceBusiness.getConfiguration(serviceType, id), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Updates a service instance configuration object.
     *
     * @param serviceType The type of the service.
     * @param id    the service identifier
     * @param configuration the service configuration
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/config", method=POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity setConfigurationJson(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id, @RequestBody AbstractConfigurationObject configuration) {
        try {

            final Details details = serviceBusiness.getInstanceDetails(serviceType, id, null);
            serviceBusiness.configure(serviceType, id, details, configuration);
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" configuration successfully updated."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns a service instance metadata.
     *
     * @param serviceType The type of the service.
     * @param id the service identifier
     * @param lang
     * @return
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/metadata/{lang}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMetadata(final @PathVariable("spec") String serviceType,
                               final @PathVariable("id") String id,
                               final @PathVariable("lang") String lang) {
        try {
            return new ResponseEntity(serviceBusiness.getInstanceDetails(serviceType, id, lang), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Updates a service instance metadata.
     *
     * @param serviceType The type of the service.
     * @param id          The service identifier
     * @param metadata    The service metadata
     *
     */
    @RequestMapping(value="/OGC/{spec}/{id}/metadata", method=POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity setMetadata(final @PathVariable("spec") String serviceType, final @PathVariable("id") String id, final @RequestBody Details metadata) {
        try {
            serviceBusiness.ensureExistingInstance(serviceType, id);
            final Object config = serviceBusiness.getConfiguration(serviceType, id);
            serviceBusiness.configure(serviceType, id, metadata, config);
            return new ResponseEntity(AcknowlegementType.success(serviceType.toUpperCase() + " service \"" + id + "\" details successfully updated."), OK);
        } catch (Throwable ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns the {@link OGCConfigurer} instance from its {@link Specification}.
     *
     * @throws NotRunningServiceException if the service is not activated or if an error
     * occurred during its startup
     */
    private OGCConfigurer getConfigurer(final String specification) throws NotRunningServiceException {
        final Specification spec = Specification.fromShortName(specification);
        if (!spec.supported()) {
            throw new IllegalArgumentException(specification + " is not a valid OGC service.");
        }
        return (OGCConfigurer) wsengine.newInstance(spec);
    }
}
