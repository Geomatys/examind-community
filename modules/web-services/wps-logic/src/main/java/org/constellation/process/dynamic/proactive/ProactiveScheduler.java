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
package org.constellation.process.dynamic.proactive;

import org.constellation.process.dynamic.proactive.model.Job;
import org.constellation.process.dynamic.proactive.model.Identifier;
import org.constellation.process.dynamic.proactive.model.Workflow;
import org.constellation.process.dynamic.proactive.model.TaskResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.sis.util.logging.Logging;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ProactiveScheduler {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.process.proactive");

    private final RestTemplate restTemplate;

    private final String prLogin;

    private final String prPassword;

    private final String prUrl;

    private final String workflowDir;

    public ProactiveScheduler(String url, String login, String pwd, String workflowDir) {
        this.prLogin = login;
        this.prPassword = pwd;
        this.prUrl = url;
        this.workflowDir = workflowDir;

        LOGGER.info("proactive application URL set to : " + prLogin + ":********@" + prUrl);

        restTemplate =  new RestTemplate();
    }

    public String login() throws ProActiveException {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("username", prLogin);
            params.add("password", prPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity(params ,headers);
            ResponseEntity<String> res = restTemplate.postForEntity(prUrl + "rest/scheduler/login", entity, String.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Failed to login:" + res.getBody());
            }
            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.info(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public String getLocalWorkflow(String name) throws IOException {
        Path p = Paths.get(workflowDir, name + ".xml");
        return IOUtilities.toString(p);
    }


    public Workflow getWorkflow(String sessionId, String name) throws ProActiveException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<Workflow[]> res = restTemplate.exchange(prUrl + "rest/studio/workflows", HttpMethod.GET, entity, Workflow[].class);
            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Unable to load workflows:" + res.getBody());
            }

            for (Workflow w : res.getBody()) {
                if (w.getName().equals(name)) {
                    return w;
                }
            }
            throw new ProActiveException("Workflow " + name + " not found");
            //return workflows[index]['xml']
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public Integer submitJob(String sessionId, File jobFile) throws ProActiveException {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();

            HttpHeaders fileHeader = new HttpHeaders();
            fileHeader.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<FileSystemResource> filePart = new HttpEntity<>(new FileSystemResource(jobFile), fileHeader);
            bodyMap.add("file", filePart);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

            // files = {'file': ('job.xml', job, 'application/xml')}
            //  res = requests.post("%s" % self.base_url, headers=headers, files=files)
            ResponseEntity<Identifier> res = restTemplate.postForEntity(prUrl + "rest/scheduler/submit", requestEntity, Identifier.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Failed to submit job:" + res.getBody());
            }

            return res.getBody().getId();

        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public Boolean cancelJob(String sessionId, Integer jobId) throws ProActiveException {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<Boolean> res = restTemplate.exchange(prUrl + "rest/scheduler/jobs/" + jobId + "/kill/", HttpMethod.PUT, entity, Boolean.class);

            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Failed to kill job:" + res.getBody());
            }
            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public Job getJob(String sessionId, Integer jobId) throws ProActiveException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<Job> res = restTemplate.exchange(prUrl + "rest/scheduler/jobs/" + jobId, HttpMethod.GET, entity, Job.class);
            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Failed to retrieve job:" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public TaskResult getTaskResult(String sessionId, Integer jobId, String taskName) throws ProActiveException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<TaskResult> res = restTemplate.exchange(prUrl + "rest/scheduler/jobs/" + jobId + "tasks/" + taskName + "/result", HttpMethod.GET, entity, TaskResult.class);
            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Could not fetch task result:" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public Object getTaskResultValue(String sessionId, Integer jobId, String taskName) throws ProActiveException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("sessionid", sessionId);
            HttpEntity entity = new HttpEntity(headers);
            LOGGER.fine("GET TASK RESULT: " + prUrl + "rest/scheduler/jobs/" + jobId + "/tasks/" + taskName + "/result/value");
            ResponseEntity<String> res = restTemplate.exchange(prUrl + "rest/scheduler/jobs/" + jobId + "/tasks/" + taskName + "/result/value", HttpMethod.GET, entity, String.class);
            if (!res.getStatusCode().equals(HttpStatus.OK)) {
                throw new ProActiveException("Could not fetch task result value:" + res.getBody());
            }

            return res.getBody();
        } catch (HttpStatusCodeException ex) {
            LOGGER.warning(ex.getResponseBodyAsString());
            throw new ProActiveException(ex, ex.getResponseBodyAsString());
        }
    }

    public static Integer[] getJobProgress(Job job) {
        return new Integer[] {job.getJobInfo().getNumberOfFinishedTasks(), job.getJobInfo().getTotalNumberOfTasks()};
    }

    public static Float getTaskProgress(Job job, String taskId) {
        return job.getTasks().get(taskId).getTaskInfo().getProgress();
    }

    public static String setWorkflowVariable(String workflow, String name, String value) throws ProActiveException {
        StringWriter sw = new StringWriter();
        try {
            final Document doc = NodeUtilities.getDocumentFromString(workflow);

            NodeList elements = doc.getElementsByTagName("variable");
            boolean found = false;
            for (int i = 0; i < elements.getLength(); i++) {
                Node v = elements.item(i);

                if (v.getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                    v.getAttributes().getNamedItem("value").setNodeValue(value);
                    found = true;
                }
            }
            if (!found) {
                throw new ProActiveException("set_workflow_variable : Variable " + name + " not found");
            }
            NodeUtilities.writerNode(doc, sw);
        } catch (TransformerException | ParserConfigurationException | SAXException | IOException ex) {
            throw new ProActiveException(ex);
        }
        return sw.toString();
    }

    public static String getWorkflowVariable(String workflow, String name) throws ProActiveException {
        try {
            final Document doc = NodeUtilities.getDocumentFromString(workflow);
            NodeList elements = doc.getElementsByTagName("variable");
            for (int i = 0; i < elements.getLength(); i++) {
                Node v = elements.item(i);

                if (v.getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                    return v.getAttributes().getNamedItem("value").getNodeValue();
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new ProActiveException(ex);
        }
        throw new ProActiveException("get_workflow_variable : Variable " + name + " not found");
    }



}
