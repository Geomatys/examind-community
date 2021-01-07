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
package com.examind.repository.filesystem;

import static com.examind.repository.filesystem.FileSystemUtilities.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.process.TaskParameter;
import org.constellation.dto.process.TaskParameterWithOwnerName;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.TaskParameterRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemTaskParameterRepository extends AbstractFileSystemRepository implements TaskParameterRepository {

    private final Map<Integer, TaskParameterWithOwnerName> byId = new HashMap<>();
    private final Map<String, List<TaskParameterWithOwnerName>> byType = new HashMap<>();
    private final Map<String, Map<String, Map<String, List<TaskParameterWithOwnerName>>>> byAuthCodeName = new HashMap<>();
    private final List<TaskParameterWithOwnerName> programmedTask = new ArrayList<>();

    public FileSystemTaskParameterRepository() {
        super(TaskParameterWithOwnerName.class);
        load();
    }

    private void load() {
        try {
            Path taskParamDir = getDirectory(TASK_PARAM_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(taskParamDir)) {
                for (Path taskParamFile : directoryStream) {
                    TaskParameterWithOwnerName taskParam = (TaskParameterWithOwnerName) getObjectFromPath(taskParamFile, pool);
                    byId.put(taskParam.getId(), taskParam);

                    if (byType.containsKey(taskParam.getType())) {
                        byType.get(taskParam.getType()).add(taskParam);
                    } else {
                        List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                        tasks.add(taskParam);
                        byType.put(taskParam.getType(), tasks);
                    }

                    if (byAuthCodeName.containsKey(taskParam.getProcessAuthority())) {
                        Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = byAuthCodeName.get(taskParam.getProcessAuthority());
                        if (byCode.containsKey(taskParam.getProcessCode())) {
                            Map<String, List<TaskParameterWithOwnerName>> byName = byCode.get(taskParam.getProcessCode());
                            if (byName.containsKey(taskParam.getName())) {
                                byName.get(taskParam.getName()).add(taskParam);
                            } else {
                                List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                                tasks.add(taskParam);
                                byName.put(taskParam.getName(), tasks);
                            }

                        } else {
                            List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                            tasks.add(taskParam);
                            Map<String, List<TaskParameterWithOwnerName>> byName = new HashMap<>();
                            byName.put(taskParam.getName(), tasks);
                            byCode.put(taskParam.getProcessCode(), byName);
                        }
                    } else {
                        List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                        tasks.add(taskParam);
                        Map<String, List<TaskParameterWithOwnerName>> named = new HashMap<>();
                        named.put(taskParam.getName(), tasks);
                        Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = new HashMap<>();
                        byCode.put(taskParam.getProcessCode(), named);
                        byAuthCodeName.put(taskParam.getProcessAuthority(), byCode);
                    }

                    if (taskParam.getTrigger() != null) {
                        programmedTask.add(taskParam);
                    }
                    incCurrentId(taskParam);
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        return byId.containsKey(id);
    }

    @Override
    public TaskParameter get(Integer uuid) {
        return byId.get(uuid);
    }

    @Override
    public List<? extends TaskParameter> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<? extends TaskParameter> findAllByType(String type) {
        if (byType.containsKey(type)) {
            return new ArrayList<>(byType.get(type));
        }
        return new ArrayList<>();
    }

    @Override
    public List<? extends TaskParameter> findAllByNameAndProcess(String name, String authority, String code) {
        if (byAuthCodeName.containsKey(authority)) {
            if (byAuthCodeName.get(authority).containsKey(code)) {
                return new ArrayList<>(byAuthCodeName.get(authority).get(code).get(name));
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<? extends TaskParameter> findProgrammedTasks() {
        return programmedTask;
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(TaskParameter task) {
        if (task != null) {
            TaskParameterWithOwnerName taskParam = new TaskParameterWithOwnerName(task, null);
            final int id = assignCurrentId(taskParam);

            Path taskDir = getDirectory(TASK_PARAM_DIR);
            Path taskFile = taskDir.resolve(id + ".xml");
            writeObjectInPath(taskParam, taskFile, pool);

            byId.put(taskParam.getId(), taskParam);

            if (byType.containsKey(taskParam.getType())) {
                byType.get(taskParam.getType()).add(taskParam);
            } else {
                List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                tasks.add(taskParam);
                byType.put(taskParam.getType(), tasks);
            }

            if (byAuthCodeName.containsKey(taskParam.getProcessAuthority())) {
                Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = byAuthCodeName.get(taskParam.getProcessAuthority());
                if (byCode.containsKey(taskParam.getProcessCode())) {
                    Map<String, List<TaskParameterWithOwnerName>> byName = byCode.get(taskParam.getProcessCode());
                    if (byName.containsKey(taskParam.getName())) {
                        byName.get(taskParam.getName()).add(taskParam);
                    } else {
                        List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                        tasks.add(taskParam);
                        byName.put(taskParam.getName(), tasks);
                    }

                } else {
                    List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                    tasks.add(taskParam);
                    Map<String, List<TaskParameterWithOwnerName>> byName = new HashMap<>();
                    byName.put(taskParam.getName(), tasks);
                    byCode.put(taskParam.getProcessCode(), byName);
                }
            } else {
                List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                tasks.add(taskParam);
                Map<String, List<TaskParameterWithOwnerName>> named = new HashMap<>();
                named.put(taskParam.getName(), tasks);
                Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = new HashMap<>();
                byCode.put(taskParam.getProcessCode(), named);
                byAuthCodeName.put(taskParam.getProcessAuthority(), byCode);
            }

            if (taskParam.getTrigger() != null) {
                programmedTask.add(taskParam);
            }
            return taskParam.getId();
        }
        return null;
    }

    @Override
    public void update(TaskParameter task) {
        if (byId.containsKey(task.getId())) {

            TaskParameterWithOwnerName taskParam = new TaskParameterWithOwnerName(task, null);

            Path taskDir = getDirectory(TASK_PARAM_DIR);
            Path taskFile = taskDir.resolve(taskParam.getId() + ".xml");
            writeObjectInPath(taskParam, taskFile, pool);

            byId.put(task.getId(), taskParam);

            if (byType.containsKey(taskParam.getType())) {
                byType.get(taskParam.getType()).add(taskParam);
            } else {
                List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                tasks.add(taskParam);
                byType.put(taskParam.getType(), tasks);
            }

            if (byAuthCodeName.containsKey(taskParam.getProcessAuthority())) {
                Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = byAuthCodeName.get(taskParam.getProcessAuthority());
                if (byCode.containsKey(taskParam.getProcessCode())) {
                    Map<String, List<TaskParameterWithOwnerName>> byName = byCode.get(taskParam.getProcessCode());
                    if (byName.containsKey(taskParam.getName())) {
                        byName.get(taskParam.getName()).add(taskParam);
                    } else {
                        List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                        tasks.add(taskParam);
                        byName.put(taskParam.getName(), tasks);
                    }

                } else {
                    List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                    tasks.add(taskParam);
                    Map<String, List<TaskParameterWithOwnerName>> byName = new HashMap<>();
                    byName.put(taskParam.getName(), tasks);
                    byCode.put(taskParam.getProcessCode(), byName);
                }
            } else {
                List<TaskParameterWithOwnerName> tasks = new ArrayList<>();
                tasks.add(taskParam);
                Map<String, List<TaskParameterWithOwnerName>> named = new HashMap<>();
                named.put(taskParam.getName(), tasks);
                Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = new HashMap<>();
                byCode.put(taskParam.getProcessCode(), named);
                byAuthCodeName.put(taskParam.getProcessAuthority(), byCode);
            }
        }
    }

    @Override
    public int delete(Integer taskId) {
        if (byId.containsKey(taskId)) {

            TaskParameterWithOwnerName taskParam = byId.get(taskId);

            Path taskDir = getDirectory(TASK_PARAM_DIR);
            Path taskFile = taskDir.resolve(taskParam.getId() + ".xml");
            try {
                Files.delete(taskFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(taskParam.getId());
            if (byType.containsKey(taskParam.getType())) {
                byType.get(taskParam.getType()).remove(taskParam);
            }

            if (byAuthCodeName.containsKey(taskParam.getProcessAuthority())) {
                Map<String, Map<String, List<TaskParameterWithOwnerName>>> byCode = byAuthCodeName.get(taskParam.getProcessAuthority());
                if (byCode.containsKey(taskParam.getProcessCode())) {
                    Map<String, List<TaskParameterWithOwnerName>> byName = byCode.get(taskParam.getProcessCode());
                    if (byName.containsKey(taskParam.getName())) {
                        byName.get(taskParam.getName()).remove(taskParam);
                    }
                }
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int cpt = 0;
        for (Integer id : new HashSet<>(byId.keySet())) {
            cpt = cpt + delete(id);
        }
        return cpt;
    }
}
