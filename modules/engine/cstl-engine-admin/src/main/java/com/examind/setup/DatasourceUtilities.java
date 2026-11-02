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
import com.examind.dto.fs.Provider;
import static com.examind.setup.ProviderUtilities.getProviderFileFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.constellation.api.CommonConstants.FILE_STORE;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.dto.DataSource;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;

/**
 *
 * @author glegal
 */
public class DatasourceUtilities {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    public static int getOrCreateDatasourceForProviderFiles(IDatasourceBusiness dBusiness, FileSystemAnalysis.ProviderWithPath pwp, boolean computeHash) throws ConstellationException {
        String storeId = pwp.provider.getProviderType();
        return getOrCreateDatasourceForProviderFiles(dBusiness, pwp, storeId, computeHash);
    }
    
    public static int getOrCreateDatasourceForProviderFiles(IDatasourceBusiness dBusiness, FileSystemAnalysis.ProviderWithPath pwp, String storeId, boolean computeHash) throws ConstellationException {
        Predicate<Path> fileFilter = getProviderFileFilter(pwp.provider);
        URI dataUrl = getDataUri(pwp.ymlFile.getParent(), pwp.provider.getLocation());
        String dsIdentifier = pwp.provider.getIdentifier();

        final var source = pwp.provider.getSource();
        return getOrCreateDatasource(
                dBusiness, dsIdentifier, dataUrl,
                source == null ? null : source.getUserName(),
                source == null ? null : source.getPassword(),
                fileFilter, storeId, computeHash,
                source == null ? null : source.getAdvancedParameters());
    }
    
    public static int getOrCreateDatasourceForConfigFiles(IDatasourceBusiness dBusiness, String dsIdentifier, Path rootDir, Predicate<Path> fileFilter) throws ConstellationException {
        return getOrCreateDatasource(dBusiness, dsIdentifier, rootDir == null ? null : rootDir.toUri(), null, null, fileFilter, FILE_STORE, true, null);
    }
    
    private static int getOrCreateDatasource(IDatasourceBusiness dBusiness, String dsIdentifier, URI url, String userName, String password, Predicate<Path> fileFilter, String storeId, boolean computeHash, Map<String, String> datasourceProperties) throws ConstellationException {
        int dsId;
        DataSource candidate = dBusiness.getDatasource(dsIdentifier);
        if (candidate == null) {
            
            DataSource ds = new DataSource();
            ds.setIdentifier(dsIdentifier);
            if (userName != null) ds.setUsername(userName);
            if (password != null) ds.setPwd(password);
            ds.setType(url.getScheme() == null ? "file" : url.getScheme());
            ds.setUrl(url.toString());
            ds.setPermanent(Boolean.TRUE);
            ds.setReadFromRemote(true);
            ds.setStoreId(storeId);
            if (datasourceProperties != null) ds.setProperties(datasourceProperties);
            dsId = dBusiness.create(ds);

            dBusiness.computeDatasourceStores(dsId, false, storeId, true, false, computeHash, fileFilter);
            dBusiness.recordSelectedPath(dsId, false);

        } else {
            dsId = candidate.getId();
            dBusiness.scanForModification(dsId, fileFilter);
            dBusiness.recordSelectedPath(dsId, true);
        }
        return dsId;
    }
    
    public static Integer getOrCreateSQLDatasource(IDatasourceBusiness dBusiness, Provider conf) throws ConstellationException {
        Datasource ds = conf.getTarget() != null ? conf.getTarget() : conf.getSource();
        return getOrCreateSQLDatasource(dBusiness, ds.getIdentifier(), ds);
    }
    
    public static Integer getOrCreateSQLDatasource(IDatasourceBusiness dBusiness, String identifier, Datasource source) throws ConstellationException {
        if (source == null) return null;
        String location = source.getLocation();
        String userName = source.getUserName();
        String pwd = source.getPassword();
        DataSource ds = new DataSource(null, identifier, "database", location, userName, pwd, null, false, System.currentTimeMillis(), System.currentTimeMillis(), "COMPLETED", null, true, source.getAdvancedParameters());
        return dBusiness.getOrcreate(ds);
    }

    public static URI getDataUri(Path parentDir, String dataStr) {
        Path result;
        try {
            URI parsed = new URI(dataStr);
            if (parsed.getScheme() != null) {
                return parsed;
            } else {
                result = parentDir.resolve(dataStr).normalize();
            }
        } catch (URISyntaxException e) {
            result = parentDir.resolve(dataStr);
        }
        return result.toUri();
    }
    
    /**
     * Safely delete a datasource without throwing exception if the datasource does not exist.
     * 
     * @param dBusiness Datasource business.
     * @param datasourceId Datasource Identifier.
     */
    public static void deleteDatasource(IDatasourceBusiness dBusiness, int datasourceId) {
        if (datasourceId != -1) {
            try {
                dBusiness.delete(datasourceId);
            } catch (TargetNotFoundException ex) {
                LOGGER.log(Level.WARNING, "No datasource found to remove: {0}", datasourceId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error while removing datasource: " + datasourceId , ex);
            }
        }
    }
}
