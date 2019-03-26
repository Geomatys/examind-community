/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package com.examind.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.SecurityManager;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.auth.DigestResponse;
import com.ettrema.http.fs.LockManager;
import com.ettrema.http.fs.NullSecurityManager;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.service.config.webdav.WebdavContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class FsResource implements Resource, MoveableResource, CopyableResource, LockableResource, DigestResource {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.webdav");

    protected Path file;
    protected final String host;
    protected String ssoPrefix;
    protected final boolean isDigestAllowed;
    private final LockManager lockManager;
    private final SecurityManager securityManager;
    protected final long maxAgeSecond;
    protected final WebdavContext context;


    protected abstract void doCopy(Path dest);

    public FsResource(final String host, final Path file, final WebdavContext context) {
        this.host            = host;
        this.file            = file;
        this.context         = context;
        this.maxAgeSecond    = context.getMaxAgeSeconds();
        this.isDigestAllowed = context.isDigestAllowed();
        this.ssoPrefix       = context.getSsoPrefix();
        this.lockManager     = new FsMemoryLockManager();
        this.securityManager = new NullSecurityManager();
    }

    public Path getFile() {
        return file;
    }

    @Override
    public String getUniqueId() {
        String id;
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            id = lastModifiedTime + "_" + Files.size(file) + "_" + file.toAbsolutePath().toString();
        } catch (IOException e) {
            id = file.toAbsolutePath().toString();
        }
        return id.hashCode() + "";
    }

    @Override
    public String getName() {
        return file.getFileName().toString();
    }

    @Override
    public Object authenticate(String user, String password) {
        return securityManager.authenticate(user, password);
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        return securityManager.authenticate(digestRequest);
    }

    @Override
    public boolean isDigestAllowed() {
        return isDigestAllowed;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        return securityManager.authorise(request, method, auth, this);
    }

    @Override
    public String getRealm() {
        return securityManager.getRealm(host);
    }

    @Override
    public Date getModifiedDate() {
        try {
            return new Date(Files.getLastModifiedTime(file).toMillis());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Date getCreateDate() {
        return null;
    }

    public int compareTo(Resource o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public void moveTo(CollectionResource newParent, String newName) {
        if (newParent instanceof FsDirectoryResource) {
            FsDirectoryResource newFsParent = (FsDirectoryResource) newParent;
            Path dest = newFsParent.getFile().resolve(newName);
            try {
                Files.move(file, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to move to: " + dest.toAbsolutePath().toString());
            }
            this.file = dest;
        } else {
            throw new RuntimeException("Destination is an unknown type. Must be a FsDirectoryResource, is a: " + newParent.getClass());
        }
    }

    @Override
    public void copyTo(CollectionResource newParent, String newName) {
        if (newParent instanceof FsDirectoryResource) {
            FsDirectoryResource newFsParent = (FsDirectoryResource) newParent;
            Path dest = newFsParent.getFile().resolve(newName);
            doCopy(dest);
        } else {
            throw new RuntimeException("Destination is an unknown type. Must be a FsDirectoryResource, is a: " + newParent.getClass());
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete");
        }
    }

    @Override
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException {
        return lockManager.lock(timeout, lockInfo, this);
    }

    @Override
    public LockResult refreshLock(String token) throws NotAuthorizedException {
        return lockManager.refresh(token, this);
    }

    @Override
    public void unlock(String tokenId) throws NotAuthorizedException {
        lockManager.unlock(tokenId, this);
    }

    @Override
    public LockToken getCurrentLock() {
        if (lockManager != null) {
            return lockManager.getCurrentToken(this);
        } else {
            LOGGER.log(Level.WARNING, "getCurrentLock called, but no lock manager: file: {0}", file.toAbsolutePath().toString());
            return null;
        }
    }
}
