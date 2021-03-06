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
import java.util.Optional;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.CstlUser;
import org.constellation.dto.UserWithRole;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemUserRepository extends AbstractFileSystemRepository implements UserRepository {

    private final Map<Integer, UserWithRole> byId = new HashMap<>();

    private final Map<Integer, UserWithRole> activeById = new HashMap<>();

    private final Map<String, UserWithRole> bylogin = new HashMap<>();

    private final Map<String, UserWithRole> byEmail = new HashMap<>();

    private final Map<String, UserWithRole> byForgotPwd = new HashMap<>();

    public FileSystemUserRepository() {
        super(UserWithRole.class);
        load();
    }

    private void load() {
        try {
            Path userDir = getDirectory(USER_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(userDir)) {
                for (Path userFile : directoryStream) {
                    UserWithRole user = (UserWithRole) getObjectFromPath(userFile, pool);
                    byId.put(user.getId(), user);
                    bylogin.put(user.getLogin(), user);
                    byEmail.put(user.getEmail(), user);
                    byForgotPwd.put(user.getForgotPasswordUuid(), user);
                    if (user.getActive()) {
                        activeById.put(user.getId(), user);
                    }
                    incCurrentId(user);
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
    public Optional<CstlUser> findOne(String login) {
        return Optional.ofNullable(bylogin.get(login));
    }

    @Override
    public Optional<CstlUser> findById(Integer id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CstlUser> findByEmail(String email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    @Override
    public Optional<UserWithRole> findByForgotPasswordUuid(String uuid) {
        return Optional.ofNullable(byForgotPwd.get(uuid));
    }

    @Override
    public List<CstlUser> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(Integer id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(String login) {
        return Optional.ofNullable(bylogin.get(login));
    }

    @Override
    public Optional<UserWithRole> findOneWithRoleByMail(String mail) {
        return Optional.ofNullable(byEmail.get(mail));
    }

    @Override
    public List<String> getRoles(int userId) {
        UserWithRole u = byId.get(userId);
        if (u != null) {
            return u.getRoles();
        }
        return new ArrayList<>();
    }

    @Override
    public long countUser() {
        return byId.size();
    }

    @Override
    public boolean loginAvailable(String login) {
        return !bylogin.containsKey(login);
    }

    @Override
    public List<UserWithRole> findActivesWithRole() {
        return new ArrayList<>(activeById.values());
    }

    @Override
    public boolean isLastAdmin(int userId) {
        for (UserWithRole u : byId.values()) {
            if (!u.getId().equals(userId) &&
                 u.getRoles().contains("cstl-admin")) {
                return false;
            }
        }
        return true;
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(UserWithRole userR) {
        final int id = assignCurrentId(userR);

        Path userDir = getDirectory(USER_DIR);
        Path userFile = userDir.resolve(id + ".xml");
        writeObjectInPath(userR, userFile, pool);

        byId.put(userR.getId(), userR);
        bylogin.put(userR.getLogin(), userR);
        byEmail.put(userR.getEmail(), userR);
        byForgotPwd.put(userR.getForgotPasswordUuid(), userR);
        if (userR.getActive()) {
            activeById.put(userR.getId(), userR);
        }
        return userR.getId();
    }

    @Override
    public void update(UserWithRole userR) {
        if (byId.containsKey(userR.getId())) {

            Path userDir = getDirectory(USER_DIR);
            Path userFile = userDir.resolve(userR.getId() + ".xml");
            writeObjectInPath(userR, userFile, pool);

            byId.put(userR.getId(), userR);
            bylogin.put(userR.getLogin(), userR);
            byEmail.put(userR.getEmail(), userR);
            byForgotPwd.put(userR.getForgotPasswordUuid(), userR);
            if (userR.getActive()) {
                activeById.put(userR.getId(), userR);
            } else {
                activeById.remove(userR.getId());
            }
        }
    }

    @Override
    public int delete(Integer userId) {
        if (byId.containsKey(userId)) {

            UserWithRole userR = byId.get(userId);

            Path userDir = getDirectory(USER_DIR);
            Path userFile = userDir.resolve(userR.getId() + ".xml");
            if (Files.exists(userFile)) { // possible desync between fs and memory
                try {
                    Files.delete(userFile);
                } catch (IOException ex) {
                    throw new ConstellationPersistenceException(ex);
                }
            }
            byId.remove(userR.getId());
            bylogin.remove(userR.getLogin());
            byEmail.remove(userR.getEmail());
            byForgotPwd.remove(userR.getForgotPasswordUuid());
            if (userR.getActive()) {
                activeById.remove(userR.getId());
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

    @Override
    public int desactivate(int userId) {
        if (byId.containsKey(userId)) {
            UserWithRole userR = byId.get(userId);
            if (userR.getActive()) {
                userR.setActive(Boolean.FALSE);
                update(userR);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int activate(int userId) {
        if (byId.containsKey(userId)) {
            UserWithRole userR = byId.get(userId);
            if (!userR.getActive()) {
                userR.setActive(Boolean.TRUE);
                update(userR);
                return 1;
            }
        }
        return 0;
    }


    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<UserWithRole> search(String search, int size, int page, String sortFieldName, String order) {
        List<UserWithRole> results = new ArrayList<>();
        // todo pagination
        for (UserWithRole ur : byId.values()) {
            if (ur.getLogin().contains(search)) {
                results.add(ur);
            }
        }
        return results;
    }

    @Override
    public long searchCount(String search) {
        int results = 0;
        // todo pagination
        for (UserWithRole ur : byId.values()) {
            if (ur.getLogin().contains(search)) {
                results++;
            }
        }
        return results;
    }
}
