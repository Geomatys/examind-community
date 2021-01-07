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
import org.constellation.dto.process.Task;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.TaskRepository;
import org.springframework.stereotype.Component;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemTaskRepository  extends AbstractFileSystemRepository implements TaskRepository {

    private final Map<String, Task> byId = new HashMap<>();
    private final Map<Integer, List<Task>> byTaskParameter = new HashMap<>();

    public FileSystemTaskRepository() {
        super(Task.class);
        load();
    }

    private void load() {
        try {
            Path taskDir = getDirectory(TASK_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(taskDir)) {
                for (Path taskFile : directoryStream) {
                    Task task = (Task) getObjectFromPath(taskFile, pool);
                    byId.put(task.getIdentifier(), task);
                    if (byTaskParameter.containsKey(task.getTaskParameterId())) {
                        byTaskParameter.get(task.getTaskParameterId()).add(task);
                    } else {
                        List<Task> tasks = new ArrayList<>();
                        tasks.add(task);
                        byTaskParameter.put(task.getTaskParameterId(), tasks);
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Task get(String uuid) {
        return byId.get(uuid);
    }

    @Override
    public List<? extends Task> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Task> findRunningTasks() {
        List<Task> results = new ArrayList<>();
        for (Task task : byId.values()) {
            if (task.getDateEnd() == null) {
                results.add(task);
            }
        }
        return results;
    }

     ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public String create(Task task) {
        if (task != null) {
            Path taskDir = getDirectory(TASK_DIR);
            Path taskFile = taskDir.resolve(task.getIdentifier() + ".xml");
            writeObjectInPath(task, taskFile, pool);
            byId.put(task.getIdentifier(), task);
            if (task.getTaskParameterId() != null) {
                if (byTaskParameter.containsKey(task.getTaskParameterId())) {
                    byTaskParameter.get(task.getTaskParameterId()).add(task);
                } else {
                    List<Task> tasks = new ArrayList<>();
                    tasks.add(task);
                    byTaskParameter.put(task.getTaskParameterId(), tasks);
                }
            }
            return task.getIdentifier();
        }
        return null;
    }

    @Override
    public void update(Task task) {
        if (byId.containsKey(task.getIdentifier())) {

            Task old = byId.get(task.getIdentifier());
            if (old.getTaskParameterId() != null) {
                if (byTaskParameter.containsKey(old.getTaskParameterId())) {
                    byTaskParameter.get(old.getTaskParameterId()).remove(old);
                }
            }

            Path taskDir = getDirectory(TASK_DIR);
            Path taskFile = taskDir.resolve(task.getIdentifier()+ ".xml");
            writeObjectInPath(task, taskFile, pool);
            byId.put(task.getIdentifier(), task);

            if (byTaskParameter.containsKey(task.getTaskParameterId())) {
                byTaskParameter.get(task.getTaskParameterId()).add(task);
            } else {
                List<Task> tasks = new ArrayList<>();
                tasks.add(task);
                byTaskParameter.put(task.getTaskParameterId(), tasks);
            }
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Task> findRunningTasks(Integer tpid, Integer offset, Integer limit) {
        List<Task> results = new ArrayList<>();
        if (byTaskParameter.containsKey(tpid)) {
            List<Task> tasks = new ArrayList<>();
            for (Task t : byTaskParameter.get(tpid)) {
                if (t.getDateEnd() == null) {
                    tasks.add(t);
                }
            }
            int i = offset;
            int cpt = 0;
            while (i < tasks.size() && cpt < limit) {
                results.add(tasks.get(i));
                cpt++;
                i++;
            }
        }
        return results;
    }

    @Override
    public List<Task> taskHistory(Integer tpid, Integer offset, Integer limit) {
        List<Task> results = new ArrayList<>();
        if (byTaskParameter.containsKey(tpid)) {
            List<Task> tasks = byTaskParameter.get(tpid);
            int i = offset;
            int cpt = 0;
            while (i < tasks.size() && cpt < limit) {
                results.add(tasks.get(i));
                cpt++;
                i++;
            }
        }
        return results;
    }

    @Override
    public int delete(String uuid) {
        if (byId.containsKey(uuid)) {

            Task t = byId.get(uuid);

            Path taskDir = getDirectory(TASK_DIR);
            Path taskFile = taskDir.resolve(t.getIdentifier()+ ".xml");
            try {
                Files.delete(taskFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(t.getIdentifier());
            if (t.getTaskParameterId() != null) {
                if (byTaskParameter.containsKey(t.getTaskParameterId())) {
                    byTaskParameter.get(t.getTaskParameterId()).remove(t);
                }
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int cpt = 0;
        for (String id : new HashSet<>(byId.keySet())) {
            cpt = cpt + delete(id);
        }
        return cpt;
    }

}
