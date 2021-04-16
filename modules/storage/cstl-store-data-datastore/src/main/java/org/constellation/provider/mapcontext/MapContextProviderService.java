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
package org.constellation.provider.mapcontext;

import java.util.logging.Level;
import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.provider.AbstractDataProviderFactory;
import org.constellation.provider.DataProvider;
import static org.constellation.provider.ProviderParameters.createDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys).
 */
public class MapContextProviderService extends AbstractDataProviderFactory {

    /**
     * Service name
     */
    public static final String NAME = "mapcontext-provider";
    public static final ParameterDescriptorGroup SOURCE_CONFIG_DESCRIPTOR;

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    static {
        SOURCE_CONFIG_DESCRIPTOR = BUILDER.addName("MapContextProvider").setRequired(true)
            .createGroup();
    }

    public static final ParameterDescriptorGroup SERVICE_CONFIG_DESCRIPTOR = createDescriptor(SOURCE_CONFIG_DESCRIPTOR);

    public MapContextProviderService(){
        super(NAME);
    }

    @Override
    public ParameterDescriptorGroup getProviderDescriptor() {
        return SERVICE_CONFIG_DESCRIPTOR;
    }

    @Override
    public ParameterDescriptorGroup getStoreDescriptor() {
        return SOURCE_CONFIG_DESCRIPTOR;
    }

    @Override
    public DataProvider createProvider(String providerId, ParameterValueGroup ps) {
        if (!canProcess(ps)) {
            return null;
        }

        try {
            ParameterValueGroup param = ps.groups("MapContextProvider").get(0);
            final MapContextProvider provider = new MapContextProvider(providerId, this, param);
            getLogger().log(Level.INFO, "[PROVIDER]> map-context {0} provider created.", providerId);
            return provider;
        } catch (Exception ex) {
            // we should not catch exception, but here it's better to start all source we can
            // rather than letting a potential exception block the provider proxy
            getLogger().log(Level.SEVERE, "[PROVIDER]> Invalid map-context provider config", ex);
        }
        return null;
    }
}
