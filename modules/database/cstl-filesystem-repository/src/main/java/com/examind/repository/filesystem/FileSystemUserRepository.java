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

                    if (user.getId() >= currentId) {
                        currentId = user.getId() +1;
                    }

                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }


    @Override
    public Optional<CstlUser> findOne(String login) {
        return Optional.ofNullable(bylogin.get(login));
    }

    @Override
    public Optional<CstlUser> findById(Integer id) {
        return Optional.of(byId.get(id));
    }

    @Override
    public Optional<CstlUser> findByEmail(String email) {
        return Optional.of(byEmail.get(email));
    }

    @Override
    public Optional<CstlUser> findByForgotPasswordUuid(String uuid) {
        return Optional.of(byForgotPwd.get(uuid));
    }

    @Override
    public List<CstlUser> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(Integer id) {
        return Optional.of(byId.get(id));
    }

    @Override
    public Optional<UserWithRole> findOneWithRole(String login) {
        return Optional.of(bylogin.get(login));
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
    public int countUser() {
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public CstlUser create(CstlUser user) {
        UserWithRole userR = new UserWithRole(user, new ArrayList<>());
        userR.setId(currentId);

        Path userDir = getDirectory(USER_DIR);
        Path userFile = userDir.resolve(currentId + ".xml");
        writeObjectInPath(userR, userFile, pool);

        byId.put(user.getId(), userR);
        bylogin.put(user.getLogin(), userR);
        byEmail.put(user.getEmail(), userR);
        byForgotPwd.put(user.getForgotPasswordUuid(), userR);
        if (user.getActive()) {
            activeById.put(user.getId(), userR);
        }

        currentId++;
        return userR;
    }

    @Override
    public CstlUser update(CstlUser user) {
        if (byId.containsKey(user.getId())) {

            UserWithRole userR = new UserWithRole(user, byId.get(user.getId()).getRoles());

            Path userDir = getDirectory(USER_DIR);
            Path userFile = userDir.resolve(userR.getId() + ".xml");
            writeObjectInPath(userR, userFile, pool);

            byId.put(user.getId(), userR);
            bylogin.put(user.getLogin(), userR);
            byEmail.put(user.getEmail(), userR);
            byForgotPwd.put(user.getForgotPasswordUuid(), userR);
            if (user.getActive()) {
                activeById.put(user.getId(), userR);
            }
        }
        return null;
    }

    @Override
    public void addUserToRole(Integer userId, String roleName) {
        if (byId.containsKey(userId)) {
            UserWithRole userR = byId.get(userId);
            if (!userR.getRoles().contains(roleName)) {
                userR.getRoles().add(roleName);
                update(userR);
            }
        }
    }

    @Override
    public int delete(int userId) {
        if (byId.containsKey(userId)) {

            UserWithRole userR = byId.get(userId);

            Path userDir = getDirectory(USER_DIR);
            Path userFile = userDir.resolve(userR.getId() + ".xml");
            try {
                Files.delete(userFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long searchCount(String search) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
