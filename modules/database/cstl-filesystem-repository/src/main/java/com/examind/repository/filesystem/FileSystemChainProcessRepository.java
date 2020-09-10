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
import org.constellation.dto.process.ChainProcess;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.ChainProcessRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemChainProcessRepository extends AbstractFileSystemRepository implements ChainProcessRepository {

    private final Map<Integer, ChainProcess> byId = new HashMap<>();
    private final Map<String, Map<String, ChainProcess>> byAuthCode = new HashMap<>();

    public FileSystemChainProcessRepository() {
        super(ChainProcess.class);
        load();
    }

    private void load() {
        try {
            Path chainPDir = getDirectory(CHAIN_PROCESS_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(chainPDir)) {
                for (Path chainPfile : directoryStream) {
                    ChainProcess chain = (ChainProcess) getObjectFromPath(chainPfile, pool);
                    byId.put(chain.getId(), chain);

                    if (byAuthCode.containsKey(chain.getAuth())) {
                        byAuthCode.get(chain.getAuth()).put(chain.getCode(), chain);
                    } else {
                        Map<String, ChainProcess> byCode = new HashMap<>();
                        byCode.put(chain.getCode(), chain);
                        byAuthCode.put(chain.getAuth(), byCode);
                    }
                    incCurrentId(chain);
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }


    @Override
    public List<ChainProcess> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public ChainProcess findOne(String auth, String code) {
        if (byAuthCode.containsKey(auth)) {
            return byAuthCode.get(auth).get(code);
        }
        return null;
    }

    @Override
    public Integer findId(String auth, String code) {
        if (byAuthCode.containsKey(auth)) {
            if (byAuthCode.get(auth).containsKey(code)) {
                return byAuthCode.get(auth).get(code).getId();
            }
        }
        return null;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(ChainProcess chain) {
        if (chain != null) {
            final int id = assignCurrentId(chain);

            Path chainPDir = getDirectory(CHAIN_PROCESS_DIR);
            Path chainPfile = chainPDir.resolve(id + ".xml");
            writeObjectInPath(chain, chainPfile, pool);

            byId.put(chain.getId(), chain);
            if (byAuthCode.containsKey(chain.getAuth())) {
                byAuthCode.get(chain.getAuth()).put(chain.getCode(), chain);
            } else {
                Map<String, ChainProcess> byCode = new HashMap<>();
                byCode.put(chain.getCode(), chain);
                byAuthCode.put(chain.getAuth(), byCode);
            }
            return chain.getId();
        }
        return null;
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            ChainProcess chain = byId.get(id);

            Path chainPDir = getDirectory(CHAIN_PROCESS_DIR);
            Path chainPfile = chainPDir.resolve(chain.getId() + ".xml");
            try {
                Files.delete(chainPfile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }
            if (byAuthCode.containsKey(chain.getAuth())) {
                byAuthCode.get(chain.getAuth()).remove(chain.getCode());
            }

            byId.remove(chain.getId());

            return 1;
        }
        return 0;
    }

    @Override
    public int delete(String auth, String code) {
        ChainProcess chain = findOne(auth, code);
        if (chain != null) {

            Path chainPDir = getDirectory(CHAIN_PROCESS_DIR);
            Path chainPfile = chainPDir.resolve(chain.getId() + ".xml");
            try {
                Files.delete(chainPfile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }
            if (byAuthCode.containsKey(chain.getAuth())) {
                byAuthCode.get(chain.getAuth()).remove(chain.getCode());
            }

            byId.remove(chain.getId());

            return 1;
        }
        return 0;
    }

}
