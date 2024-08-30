package org.constellation.process.dynamic.galaxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.constellation.process.dynamic.galaxy.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Galaxy Scheduler for workflows, invocations, jobs and results
 * @author Quentin BIALOTA (Geomatys)
 */
public class GalaxyScheduler {
    private static final Logger LOGGER = Logger.getLogger("org.constellation.process.galaxy");

    private final RestTemplate restTemplate;

    private final String galaxyUrl;

    private final String galaxyAccessKey;

    public GalaxyScheduler(String url, String accessKey) {
        this.galaxyUrl = url;
        this.galaxyAccessKey = accessKey;

        LOGGER.info("galaxy application URL set to : " + galaxyUrl);

        restTemplate =  new RestTemplate();
    }

    /**
     * Get Galaxy workflow information
     * @param workflowId workflow identifier in galaxy
     * @return workflow information
     * @throws GalaxyException
     */
    public Workflow getWorkflow(String workflowId) throws GalaxyException {
        try {
            ResponseEntity<Workflow> res = restTemplate.getForEntity(galaxyUrl + "/api/workflows/" + workflowId + "?key=" + galaxyAccessKey, Workflow.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new GalaxyException("Unable to load workflow:" + res.getBody());
            }

            Workflow w = res.getBody();
            return w;
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Invoke Galaxy workflow
     * @param workflowId workflow identifier in galaxy
     * @param jsonBody parameters for the workflow in a json format
     * @return invocation identifier in galaxy
     * @throws GalaxyException
     */
    public String invokeWorkflow(String workflowId, String jsonBody) throws GalaxyException {
        try {
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<InvocationIdentifier[]> res = restTemplate.postForEntity(galaxyUrl + "/api/workflows/" + workflowId + "/invocations?key=" + galaxyAccessKey, requestEntity, InvocationIdentifier[].class);

            InvocationIdentifier assignedId = res.getBody()[0];
            if (!res.getStatusCode().equals(HttpStatus.OK) || assignedId == null) {
                throw new GalaxyException("Failed to invoke workflow:" + assignedId);
            }
            return assignedId.getId();

        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Get Galaxy invocation information
     * @param workflowId workflow identifier in galaxy
     * @param invocationId invocation identifier in galaxy
     * @return invocation information
     * @throws GalaxyException
     */
    public Invocation getInvocation(String workflowId, String invocationId) throws GalaxyException {
        try {
                ResponseEntity<Invocation> res = restTemplate.getForEntity(galaxyUrl + "/api/workflows/" + workflowId + "/invocations/" + invocationId + "?key=" + galaxyAccessKey, Invocation.class);

                if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new GalaxyException("Failed to retrieve invocation:" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Cancel Galaxy invocation
     * @param workflowId workflow identifier in galaxy
     * @param invocationId invocation identifier in galaxy
     * @throws GalaxyException
     */
    public void cancelInvocation(String workflowId, String invocationId) throws GalaxyException {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity entity = new HttpEntity(headers);

            restTemplate.delete(galaxyUrl + "/api/workflows/" + workflowId + "/invocations/" + invocationId + "?key=" + galaxyAccessKey, HttpMethod.GET, entity);

        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Get Galaxy Step/Job summary for a given invocation
     * @param workflowId workflow identifier in galaxy
     * @param invocationId invocation identifier in galaxy
     * @return summary of the steps / jobs
     * @throws GalaxyException
     */
    public StepJobSummary getInvocationStepJobSummary(String workflowId, String invocationId) throws GalaxyException {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<List<StepJobState>> res = restTemplate.exchange(galaxyUrl + "/api/workflows/" + workflowId + "/invocations/" + invocationId + "/step_jobs_summary?key=" + galaxyAccessKey, HttpMethod.GET, entity, new ParameterizedTypeReference<List<StepJobState>>() {});
            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new GalaxyException("Failed to retrieve invocation step jobs summary:" + res.getBody());
            }

            return new StepJobSummary(res.getBody());
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Get Galaxy Job information
     * Note : A job is a "unitary" process launched by Galaxy, see {{@code @Step}} for a step
     * @param jobId job identifier in galaxy
     * @return job information
     * @throws GalaxyException
     */
    public Job getJob(String jobId) throws GalaxyException {
        try {
            ResponseEntity<Job> res = restTemplate.getForEntity(galaxyUrl + "/api/jobs/" + jobId + "?key=" + galaxyAccessKey, Job.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new GalaxyException("Failed to retrieve job:" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Get Galaxy Step information
     * Note : A step is a stage in a galaxy process. A step can contain one or more jobs (see {{@code Job}})
     * @param workflowId workflow identifier in galaxy
     * @param invocationId invocation identifier in galaxy
     * @param stepId stepId step identifier in galaxy
     * @return step information
     * @throws GalaxyException
     */
    public Step getStep(String workflowId, String invocationId, String stepId) throws GalaxyException {
        try {
            ResponseEntity<Step> res = restTemplate.getForEntity(galaxyUrl + "/api/workflows/" + workflowId + "/invocations/" + invocationId + "/steps/" + stepId + "?key=" + galaxyAccessKey, Step.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new GalaxyException("Failed to retrieve invocation step :" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new GalaxyException(ex, ex.getResponseBodyAsString());
        }
    }

    /**
     * Returns an Url to the file (csv, json, ...) containing a workflow output dataset
     * @param datasetId dataset identifier in galaxy
     * @return URL of the dataset to download
     */
    public String getOutputDataset(String datasetId) {
        return galaxyUrl + "/api/datasets/" + datasetId + "/display";
    }

    /**
     * Returns an Url to the zip file containing a collection of workflow output datasets
     * @param datasetCollectionId dataset collection identifier in galaxy
     * @return URL of the dataset collection to download
     */
    public String getOutputDatasetCollection(String datasetCollectionId) {
        return galaxyUrl + "/api/dataset_collections/" + datasetCollectionId + "/download";
    }

    /**
     * Create an {{@code @InvocationRequest}} object with a map
     * List of supported keys :
     * - new_history_name (String)
     * - history_id (String)
     * - use_cached_job (boolean)
     * - parameters_normalized (boolean)
     * - batch (boolean)
     * - require_exact_tool_versions (boolean)
     * - replacement_params (JSON String)
     * - inputs (JSON String)
     * - parameters (JSON String)
     * @param data Map of inputs data for invocation
     * @return an invocation request
     * @throws GalaxyException exception when a json is badly formatted (json can be found in "inputs", "replacement_params" and "parameters")
     */
    public InvocationRequest createInvocationRequest(Map<String, Object> data) throws GalaxyException {
        InvocationRequest request = new InvocationRequest();

        for (Map.Entry<String, Object> e : data.entrySet()) {
            String identifier = e.getKey();
            Object value = e.getValue();

            if(identifier.equalsIgnoreCase("new_history_name"))
            {
                request.setNewHistoryName((String) value);
            }
            else if (identifier.equalsIgnoreCase("history_id"))
            {
                request.setHistoryId((String) value);
            }
            else if (identifier.equalsIgnoreCase("use_cached_job"))
            {
                request.setUseCachedJob(Boolean.parseBoolean((String) value)); //value is always a string, so we need to cast
            }
            else if (identifier.equalsIgnoreCase("parameters_normalized"))
            {
                request.setParametersNormalized(Boolean.parseBoolean((String) value)); //value is always a string, so we need to cast
            }
            else if (identifier.equalsIgnoreCase("batch"))
            {
                request.setBatch(Boolean.parseBoolean((String) value)); //value is always a string, so we need to cast
            }
            else if (identifier.equalsIgnoreCase("require_exact_tool_versions"))
            {
                request.setRequireExactToolVersions(Boolean.parseBoolean((String) value)); //value is always a string, so we need to cast
            }
            else if (identifier.equalsIgnoreCase("replacement_params"))
            {
                ObjectMapper replacementParamsMapper = new ObjectMapper();
                try {
                    Map<String, Object> result = replacementParamsMapper.readValue((String) value, Map.class);
                    request.setReplacementParams(result);
                } catch (JsonProcessingException ex) {
                    throw new GalaxyException("Failed to parse json for parameters :" + ex.getMessage());
                }
            }
            else if (identifier.equalsIgnoreCase("inputs"))
            {
                ObjectMapper inputsMapper = new ObjectMapper();
                TypeFactory typeFactory = inputsMapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(Map.class, String.class, Map.class);
                try {
                    Map<String, Map<String, Object>> result = inputsMapper.readValue((String) value, mapType);
                    request.setInputs(result);
                } catch (JsonProcessingException ex) {
                    throw new GalaxyException("Failed to parse json for parameters :" + ex.getMessage());
                }
            }
            else if (identifier.equalsIgnoreCase("parameters"))
            {
                ObjectMapper parametersMapper = new ObjectMapper();
                TypeFactory typeFactory = parametersMapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(Map.class, String.class, Map.class);
                try {
                    Map<String, Map<String, Object>> result = parametersMapper.readValue((String) value, mapType);
                    request.setParameters(result);
                } catch (JsonProcessingException ex) {
                    throw new GalaxyException("Failed to parse json for parameters :" + ex.getMessage());
                }
            }
        }
        return request;
    }

}
