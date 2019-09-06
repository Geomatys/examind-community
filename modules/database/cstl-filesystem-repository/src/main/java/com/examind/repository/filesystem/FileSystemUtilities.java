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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.StringList;
import org.constellation.exception.ConstellationPersistenceException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemUtilities {

    public static final String ATTACHMENT_DIR = "attachments";
    public static final String DATA_DIR = "datas";
    public static final String DATASET_DIR = "datasets";
    public static final String DATASOURCE_DIR = "datasources";
    public static final String DATASOURCE_SELECTED_PATH_DIR = "datasources_selected_paths";
    public static final String DATASOURCE_COMPLETE_PATH_DIR = "datasources_complete_paths";
    public static final String PROVIDER_DIR = "providers";
    public static final String LAYER_DIR = "layers";
    public static final String METADATA_DIR = "metadatas";
    public static final String USER_DIR = "users";
    public static final String THESAURUS_DIR = "thesaurus";
    public static final String CHAIN_PROCESS_DIR = "chain_processes";
    public static final String INTERNAL_META_DIR = "internal_metadatas";
    public static final String INTERNAL_SENSOR_DIR = "internal_sensors";
    public static final String MAPCONTEXT_DIR = "mapcontexts";
    public static final String SENSOR_DIR = "sensors";
    public static final String STYLE_DIR = "styles";
    public static final String TASK_PARAM_DIR = "task_params";
    public static final String TASK_DIR = "tasks";
    public static final String STYLE_X_DATA_DIR = "style_x_data";
    public static final String STYLE_X_LAYER_DIR = "style_x_layer";
    public static final String DATA_X_DATA_DIR = "data_x_data";
    public static final String SENSOR_X_DATA_DIR = "sensor_x_data";


    public static Path getDirectory(String dirName) {
        Path configDir = ConfigDirectory.getConfigDirectory();
        Path dir = configDir.resolve(dirName);
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }
        }
        return dir;
    }

    public static Object getObjectFromPath(final Path file, final MarshallerPool pool) throws JAXBException {
        if (file != null) {
            final Unmarshaller u = pool.acquireUnmarshaller();
            final Object config = u.unmarshal(file.toFile());
            pool.recycle(u);
            return config;
        }
        return null;
    }

    public static void writeObjectInPath(final Object obj, final Path p, final MarshallerPool pool) {
        if (obj != null) {
            try {
                final Marshaller m = pool.acquireMarshaller();
                m.marshal(obj, p.toFile());
                pool.recycle(m);
            } catch (JAXBException e) {
                throw new ConstellationPersistenceException(e);
            }
        }
    }

    public static List<Integer> getIntegerList(StringList lst) {
        List<Integer> result = new ArrayList<>();
        for (String s : lst.getList()) {
            result.add(Integer.parseInt(s));
        }
        return result;
    }
}
