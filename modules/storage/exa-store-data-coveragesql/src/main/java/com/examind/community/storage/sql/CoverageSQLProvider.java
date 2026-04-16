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
package com.examind.community.storage.sql;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import static org.apache.sis.storage.DataStoreProvider.CREATE;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.coverage.sql.AddOption;
import org.geotoolkit.coverage.sql.DatabaseStore;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;


public class CoverageSQLProvider extends DataStoreProvider {
    
    public static final String NAME = "exa-coverage-sql";
    
    public static final ParameterDescriptor<Integer> DATASOURCE_ID;

    public static final ParameterDescriptor<Path> ROOT_DIRECTORY;
    public static final ParameterDescriptor<Boolean> ALLOW_CREATE;
    
    public static final ParameterDescriptorGroup INPUT;
    static final Logger LOGGER = Logger.getLogger("com.examind.storage.sql");

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        DATASOURCE_ID =  builder.addName("datasourceId")
                            .setDescription("Examind datasource identifier")
                            .create(Integer.class, null);

        builder.setRequired(false);

        ALLOW_CREATE = builder.addName(CREATE).setRemarks("Enable schemas creation if they do not exist.")
                    .create(Boolean.class, Boolean.TRUE);
        
        builder.setRequired(true);
        ROOT_DIRECTORY = builder.addName("rootDirectory").setRemarks("Root of data directory.")
                .create(Path.class, null);

        INPUT = builder.addName(NAME).createGroup(DATASOURCE_ID, ROOT_DIRECTORY, ALLOW_CREATE);
    }

    private final DatabaseStore.Provider geotkProvider;

    public CoverageSQLProvider() {
        geotkProvider = new DatabaseStore.Provider();
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return INPUT;
    }

    @Override
    public ProbeResult probeContent(StorageConnector storageConnector) throws DataStoreException {
        try {
            return geotkProvider.probeContent(storageConnector);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Content probing failed using SQL provider", e);
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    @Override
    public DataStore open(StorageConnector storageConnector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        final Parameters p = Parameters.castOrWrap(parameters);
        return new CoverageSQLStore(p);
    }
    
     private final Double defaultWorldGGRes = 0.002083333333333d;

    public class CoverageSQLStore extends DataStore implements Aggregate {
        
        @Autowired
        private IDatasourceBusiness datasourceBusiness;
        
        private final DataSource datasource;
        private final DatabaseStore geotkStore;

        public CoverageSQLStore(Parameters parameters) throws DataStoreException {
            SpringHelper.injectDependencies(this);
            Integer datasourceId = parameters.getMandatoryValue(DATASOURCE_ID);
            try {
                this.datasource = datasourceBusiness.getSQLDatasource(datasourceId).orElse(null);
            } catch (ConstellationException ex) {
                throw new DataStoreException(ex);
            }
            if (datasource == null) throw new DataStoreException("Unable to obtain an SQL datasource from examind source:" + datasourceId);
            final Parameters geotkParams = Parameters.castOrWrap(geotkProvider.getOpenParameters().createValue());
            geotkParams.parameter("database").setValue(datasource);
            geotkParams.parameter(ROOT_DIRECTORY.getName().getCode()).setValue(parameters.getValue(ROOT_DIRECTORY));
            geotkParams.parameter(ALLOW_CREATE.getName().getCode()).setValue(parameters.getValue(ALLOW_CREATE));
            
            geotkStore = geotkProvider.open(geotkParams);
        }
        
        public void createProduct(String productName, boolean worldGG, Double worldGGRes, boolean asChild, List<Path> dataPaths) throws DataStoreException {
            GridGeometry gg = null;
            if (worldGG) {
                if (worldGGRes == null) worldGGRes = defaultWorldGGRes;
                gg = GridGeometries.getWorldGG(worldGGRes);
            }
            AddOption opt = AddOption.CREATE_PRODUCT;
            if (asChild) {
                opt = AddOption.CREATE_AS_CHILD_PRODUCT;
            }
            geotkStore.addRaster(productName, gg, opt, dataPaths.toArray(Path[]::new));
        }
        
        public void removeAllProducts() throws DataStoreException {
            for (Resource res : geotkStore.components()) {
                // remove from store
                geotkStore.remove(res);
            }
        }

        @Override
        public Collection<? extends Resource> components() throws DataStoreException {
            return geotkStore.components();
        }

        @Override
        public Optional<ParameterValueGroup> getOpenParameters() {
            return geotkStore.getOpenParameters();
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            if (geotkStore != null) {
                return geotkStore.getMetadata();
            }
            return new DefaultMetadata();
        }

        @Override
        public void close() throws DataStoreException {
            if (geotkStore != null) geotkStore.close();
        }
    }
}