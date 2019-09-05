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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.process.Task;
import org.constellation.repository.TaskRepository;
import org.springframework.stereotype.Component;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemTaskRepository  extends AbstractFileSystemRepository implements TaskRepository {

    private final Map<String, Task> byId = new HashMap<>();

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
            return task.getIdentifier();
        }
        return null;
    }

    @Override
    public void update(Task task) {
        if (byId.containsKey(task.getIdentifier())) {

            Path taskDir = getDirectory(TASK_DIR);
            Path taskFile = taskDir.resolve(task.getIdentifier()+ ".xml");
            writeObjectInPath(task, taskFile, pool);
            byId.put(task.getIdentifier(), task);
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Task> findRunningTasks(Integer id, Integer offset, Integer limit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Task> taskHistory(Integer id, Integer offset, Integer limit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Task> findDayTask(String process_authority) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(String uuid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
