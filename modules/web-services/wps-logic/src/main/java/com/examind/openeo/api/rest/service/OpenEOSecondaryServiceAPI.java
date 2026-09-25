/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.openeo.api.rest.service;

import com.examind.openeo.api.rest.process.OpenEOProcessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.constellation.api.ProviderType;
import org.constellation.api.TaskState;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.process.Task;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.constellation.security.SecurityManagerHolder;
import org.geotoolkit.openeo.dto.ResponseMessage;
import org.geotoolkit.openeo.dto.process.LogEntries;
import org.geotoolkit.openeo.dto.process.LogEntry;
import org.geotoolkit.openeo.dto.process.Process;
import org.geotoolkit.openeo.dto.service.CreateServiceBody;
import org.geotoolkit.openeo.dto.service.Service;
import org.geotoolkit.openeo.dto.service.ServiceList;
import org.geotoolkit.openeo.dto.service.UpdateServiceBody;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * openEO Secondary Web Services REST API ({@code POST/GET/PATCH/DELETE /services}).
 *
 * <p>A secondary service publishes an already-completed process graph's result as a standing
 * WMS or WCS instance, backed by the {@link OpenEOProcessService#runProcessGraphToFile} synchronous
 * execution pipeline. {@code GET /service_types} stays in {@code OpenEOCapabilitiesAPI}.</p>
 *
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Secondary-Services">OpenEO Doc</a>
 */
@RestController
public class OpenEOSecondaryServiceAPI {

    private static final Logger LOGGER = Logger.getLogger("com.examind.openeo.api.rest");

    /**
     * {@link Task#getType()} value used to persist secondary services in the same
     * {@code admin.task} table already used for openEO batch jobs (owner-scoped, no new table).
     */
    private static final String SECONDARY_SERVICE_TASK_TYPE = "openeo-secondary-service";

    private static final List<String> SUPPORTED_TYPES = List.of("wms", "wcs");

    @Autowired
    private IProcessBusiness processBusiness;

    @Autowired
    private IUserBusiness userBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private OpenEOProcessService openEOProcessService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Integer getCurrentUserId() throws ConstellationException {
        String login = SecurityManagerHolder.getInstance().getCurrentUserLogin();
        return userBusiness.findOne(login)
                .orElseThrow(() -> new ConstellationException("No user found for login " + login))
                .getId();
    }

    private boolean isOwnedByCurrentUser(Task task) throws ConstellationException {
        return task != null
                && SECONDARY_SERVICE_TASK_TYPE.equals(task.getType())
                && task.getOwner() != null
                && task.getOwner().equals(getCurrentUserId());
    }

    private SecondaryServiceRecord readRecord(Task task) throws ConstellationException {
        try {
            return objectMapper.readValue(task.getTaskOutput(), SecondaryServiceRecord.class);
        } catch (JsonProcessingException ex) {
            throw new ConstellationException("Cannot deserialize secondary service " + task.getIdentifier(), ex);
        }
    }

    private void writeRecord(Task task, SecondaryServiceRecord record) throws ConstellationException {
        try {
            task.setTaskOutput(objectMapper.writeValueAsString(record));
        } catch (JsonProcessingException ex) {
            throw new ConstellationException("Cannot serialize secondary service " + task.getIdentifier(), ex);
        }
    }

    private Service toServiceDto(Task task) throws ConstellationException {
        SecondaryServiceRecord record = readRecord(task);
        return new Service()
                .id(task.getIdentifier())
                .title(record.getTitle())
                .description(record.getDescription())
                .url(record.getUrl())
                .type(record.getType())
                .enabled(record.isEnabled())
                .process(record.getProcess())
                .configuration(record.getConfiguration())
                .created(Instant.ofEpochMilli(task.getDateStart()).toString());
    }

    /**
     * The provider and data id produced by {@link #registerResultAsData}.
     */
    private record RegisteredData(Integer providerId, Integer dataId) {}

    /**
     * Registers a job-result file (GeoTIFF or NetCDF) as a new Examind provider/data, following
     * the same {@code data-store} recipe already used for tests (see
     * {@code TestEnvironment#createTifProvider}/{@code createNCProvider}).
     */
    private RegisteredData registerResultAsData(Path resultFile, Integer owner) throws ConstellationException {
        String[] nameSplit = resultFile.getFileName().toString().split("\\.");
        String ext = nameSplit[nameSplit.length - 1];
        String group;
        if (ext.equalsIgnoreCase("tif") || ext.equalsIgnoreCase("tiff")) {
            group = "GeoTIFF";
        } else if (ext.equalsIgnoreCase("nc") || ext.equalsIgnoreCase("netcdf")) {
            group = "NetCDF";
        } else {
            throw new ConstellationException("Unsupported result file format: " + ext);
        }

        try {
            final String providerIdentifier = "openeo-service-" + UUID.randomUUID();
            final DataProviderFactory dsFactory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = dsFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dsFactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup(group);
            config.parameter("location").setValue(resultFile.toUri().toURL());

            Integer providerId = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
            providerBusiness.createOrUpdateData(providerId, null, true, false, owner);
            List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(providerId);
            if (dataIds.isEmpty()) {
                throw new ConstellationException("No data produced by provider " + providerIdentifier);
            }
            dataBusiness.acceptData(dataIds.getFirst(), owner, false);
            return new RegisteredData(providerId, dataIds.getFirst());
        } catch (ConstellationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConstellationException("Error while registering result file as data: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates a secondary WMS/WCS service from a process graph, running it synchronously
     * and publishing the resulting file as a standing service (snapshot, not live).
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services", "openeo/{serviceId:.+}/services/"}, method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity createService(@PathVariable("serviceId") String serviceId, @RequestBody final CreateServiceBody body) {
        String type = body.getType() == null ? null : body.getType().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(type)) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Unsupported service type : " + body.getType() + ". Only WMS and WCS are supported.", List.of()),
                    HttpStatus.BAD_REQUEST);
        }

        Integer providerId = null;
        Integer examindServiceId = null;
        try {
            Integer owner = getCurrentUserId();
            Process process = objectMapper.convertValue(body.getProcess(), Process.class);

            Path resultFile = openEOProcessService.runProcessGraphToFile(serviceId, process);
            RegisteredData registered = registerResultAsData(resultFile, owner);
            providerId = registered.providerId();
            Integer dataId = registered.dataId();

            String openeoId = UUID.randomUUID().toString();
            examindServiceId = serviceBusiness.create(type, "openeo-" + openeoId, null, null, owner);
            serviceBusiness.start(examindServiceId);
            Integer layerId = layerBusiness.add(dataId, null, null, "openeo-" + openeoId, body.getTitle(), examindServiceId, null);

            String url = Application.getProperty(AppProperty.CSTL_URL) + "WS/" + type + "/openeo-" + openeoId;
            boolean enabled = body.getEnabled() == null || body.getEnabled();

            SecondaryServiceRecord record = new SecondaryServiceRecord();
            record.setTitle(body.getTitle());
            record.setDescription(body.getDescription());
            record.setType(type);
            record.setEnabled(enabled);
            record.setProcess(body.getProcess());
            record.setConfiguration(body.getConfiguration());
            record.setUrl(url);
            record.setExamindServiceId(examindServiceId);
            record.setExamindLayerId(layerId);
            record.setExamindProviderId(providerId);

            Task task = new Task();
            task.setIdentifier(openeoId);
            task.setType(SECONDARY_SERVICE_TASK_TYPE);
            task.setOwner(owner);
            task.setDateStart(System.currentTimeMillis());
            task.setState(TaskState.SUCCEED.name());
            writeRecord(task, record);
            processBusiness.addTask(task);

            if (!enabled) {
                serviceBusiness.stop(examindServiceId);
            }

            Service response = toServiceDto(task);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("openeo/" + serviceId + "/services/" + openeoId));
            headers.add("OpenEO-Identifier", openeoId);
            return new ResponseEntity(response, headers, HttpStatus.CREATED);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while creating openEO secondary service", ex);
            // best-effort cleanup of whatever was already provisioned before the failure
            if (examindServiceId != null) {
                try { serviceBusiness.delete(examindServiceId); } catch (Exception ignore) {}
            }
            if (providerId != null) {
                try { providerBusiness.removeProvider(providerId); } catch (Exception ignore) {}
            }
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Lists the current user's secondary services.
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services", "openeo/{serviceId:.+}/services/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getServices() {
        try {
            List<Service> services = new ArrayList<>();
            for (Task task : processBusiness.getTasksByType(SECONDARY_SERVICE_TASK_TYPE)) {
                if (isOwnedByCurrentUser(task)) {
                    services.add(toServiceDto(task));
                }
            }
            return new ResponseEntity(new ServiceList().services(services), HttpStatus.OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Describes a single secondary service.
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services/{id}", "openeo/{serviceId:.+}/services/{id}/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getService(@PathVariable("id") String id) {
        try {
            Task task = processBusiness.getTask(id);
            if (!isOwnedByCurrentUser(task)) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServiceNotFound", "Info : The service with the id : " + id + " was not found.", List.of()),
                        HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity(toServiceDto(task), HttpStatus.OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Logs for a secondary service. The backing process graph is a completed-job snapshot
     * (see class javadoc): there is no ongoing execution and no persistent log trail, only the
     * one-shot task's terminal state/message, so at most a single log entry is returned (none if
     * the task recorded no message, e.g. a plain success with nothing to report).
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services/{id}/logs", "openeo/{serviceId:.+}/services/{id}/logs/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getServiceLogs(@PathVariable("id") String id) {
        try {
            Task task = processBusiness.getTask(id);
            if (!isOwnedByCurrentUser(task)) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServiceNotFound", "Info : The service with the id : " + id + " was not found.", List.of()),
                        HttpStatus.NOT_FOUND);
            }
            LogEntries logs = new LogEntries();
            if (task.getMessage() != null) {
                LogEntry entry = new LogEntry();
                entry.setId(task.getIdentifier());
                entry.setLevel(TaskState.FAILED.name().equals(task.getState()) ? "error" : "info");
                entry.setMessage(task.getMessage());
                Long time = task.getDateEnd() != null ? task.getDateEnd() : task.getDateStart();
                if (time != null) {
                    entry.setTime(Instant.ofEpochMilli(time).toString());
                }
                logs.addLogsItem(entry);
            }
            return new ResponseEntity(logs, HttpStatus.OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Updates title/description/enabled on a secondary service. The backing process graph is a
     * completed-job snapshot (see class javadoc), so {@code process}/{@code type} are not editable.
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services/{id}", "openeo/{serviceId:.+}/services/{id}/"}, method = PATCH, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity updateService(@PathVariable("id") String id, @RequestBody final UpdateServiceBody body) {
        try {
            Task task = processBusiness.getTask(id);
            if (!isOwnedByCurrentUser(task)) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServiceNotFound", "Info : The service with the id : " + id + " was not found.", List.of()),
                        HttpStatus.NOT_FOUND);
            }

            SecondaryServiceRecord record = readRecord(task);
            if (body.getTitle() != null) {
                record.setTitle(body.getTitle());
            }
            if (body.getDescription() != null) {
                record.setDescription(body.getDescription());
            }
            if (body.getEnabled() != null && body.getEnabled() != record.isEnabled()) {
                record.setEnabled(body.getEnabled());
                if (body.getEnabled()) {
                    serviceBusiness.start(record.getExamindServiceId());
                } else {
                    serviceBusiness.stop(record.getExamindServiceId());
                }
            }

            writeRecord(task, record);
            processBusiness.updateTask(task);

            return new ResponseEntity(toServiceDto(task), HttpStatus.OK);
        } catch (ConstellationException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a secondary service and everything it provisioned (layer, WMS/WCS instance, provider/data).
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/services/{id}", "openeo/{serviceId:.+}/services/{id}/"}, method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity deleteService(@PathVariable("id") String id) {
        try {
            Task task = processBusiness.getTask(id);
            if (!isOwnedByCurrentUser(task)) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServiceNotFound", "Info : The service with the id : " + id + " was not found.", List.of()),
                        HttpStatus.NOT_FOUND);
            }

            SecondaryServiceRecord record = readRecord(task);
            if (record.getExamindLayerId() != null) {
                layerBusiness.remove(record.getExamindLayerId());
            }
            if (record.getExamindServiceId() != null) {
                serviceBusiness.delete(record.getExamindServiceId());
            }
            if (record.getExamindProviderId() != null) {
                providerBusiness.removeProvider(record.getExamindProviderId());
            }
            processBusiness.deleteTask(id);

            return new ResponseEntity(HttpStatus.NO_CONTENT);
        } catch (ConstellationException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
