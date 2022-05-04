/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.provider.component;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.constellation.provider.Data;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.DefaultFeatureData;
import org.constellation.provider.DefaultOtherData;
import org.constellation.provider.mapcontext.DefaultMapContextData;
import org.constellation.provider.DefaultPyramidData;
import org.geotoolkit.storage.multires.TiledResource;
import org.opengis.util.GenericName;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal
 */
@Component("exaDataCreator")
@Primary
public class DefaultExaDataCreator implements ExaDataCreator {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.provider.component");

    @Override
    public Data create(final String dataName, Date version, final DataStore store, final Resource rs) throws DataStoreException {
        if (rs == null) throw new DataStoreException("Unable to find a resource named:" + dataName);
        GenericName targetName = rs.getIdentifier().orElseThrow(() -> new DataStoreException("Only named datasets should be available from provider"));
        targetName = targetName.toFullyQualifiedName();
        if (rs instanceof TiledResource && rs instanceof GridCoverageResource) {
            return new DefaultPyramidData(targetName, (GridCoverageResource) rs, store);
        } else if (rs instanceof GridCoverageResource) {
            return new DefaultCoverageData(targetName, (GridCoverageResource) rs, store);
        } else if (rs instanceof FeatureSet){
            return new DefaultFeatureData(targetName, store, (FeatureSet) rs, null, null, null, null, version);
        } else {
            LOGGER.log(Level.WARNING, "Unexpected resource class for creating Provider Data:{0}", rs.getClass().getName());
            return new DefaultOtherData(targetName, rs, store);
        }
    }

    @Override
    public Data createMapContextData(MapLayers mp) throws DataStoreException {
        return new DefaultMapContextData(mp);
    }

}
