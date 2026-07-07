/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
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
package com.examind.setup;

import com.examind.dto.fs.Datasource;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Predicate;
import static org.constellation.api.CommonConstants.FILE_STORE;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.dto.DataSource;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author glegal
 */
public class DatasourceUtilities {
    
    public static int createDatasourceForProviderFiles(IDatasourceBusiness dBusiness, String dsIdentifier, Path rootDir, Predicate<Path> fileFilter, String storeId) throws ConstellationException {
        return createDatasource(dBusiness, dsIdentifier, rootDir, fileFilter, storeId);
    }
    
    public static int createDatasourceForConfigFiles(IDatasourceBusiness dBusiness, String dsIdentifier, Path rootDir, Predicate<Path> fileFilter) throws ConstellationException {
        return createDatasource(dBusiness, dsIdentifier, rootDir, fileFilter, FILE_STORE);
    }
    
    private static int createDatasource(IDatasourceBusiness dBusiness, String dsIdentifier, Path rootDir, Predicate<Path> fileFilter, String storeId) throws ConstellationException {
        int dsId;
        DataSource candidate = dBusiness.getDatasource(dsIdentifier);
        if (candidate == null) {
            // special case for csql as the files are from another store
            // maybe add a sub-type
            if ("coverage-sql".equals(storeId)) {
                storeId = null;
            }
            
            URI rootDirUri = rootDir.toUri();
            DataSource ds = new DataSource();
            ds.setIdentifier(dsIdentifier);
            ds.setType("file");
            ds.setUrl(rootDirUri.toString());
            ds.setPermanent(Boolean.TRUE);
            ds.setReadFromRemote(true);
            ds.setStoreId(storeId);
            dsId = dBusiness.create(ds);

            dBusiness.computeDatasourceStores(dsId, false, storeId, true, false, true, fileFilter);
            dBusiness.recordSelectedPath(dsId, false);

        } else {
            dsId = candidate.getId();
            dBusiness.scanForModification(dsId, fileFilter);
            dBusiness.recordSelectedPath(dsId, true);
        }
        return dsId;
    }
    
    public static Integer createSQLDatasource(IDatasourceBusiness dBusiness, String identifier, Datasource source) throws ConstellationException {
        if (source == null) return null;
        String location = source.getLocation();
        String userName = source.getUserName();
        String pwd = source.getPassword();
        DataSource ds = new DataSource(null, identifier, "database", location, userName, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true, source.getAdvancedParameters());
        return dBusiness.getOrcreate(ds);
    }
}
