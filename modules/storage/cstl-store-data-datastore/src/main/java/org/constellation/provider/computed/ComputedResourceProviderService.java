/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.provider.computed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.provider.AbstractDataProviderFactory;
import org.constellation.provider.DataProvider;
import static org.constellation.provider.ProviderParameters.createDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ComputedResourceProviderService extends AbstractDataProviderFactory {

    /**
     * Service name
     */
    public static final String NAME = "computed-resource";
    public static final ParameterDescriptorGroup SOURCE_CONFIG_DESCRIPTOR;

    //all providers factories, unmodifiable
    private static final Collection<ComputedResourceProviderDescriptor> COMPUTED_DESCRIPTORS;
    static {
        final List<ComputedResourceProviderDescriptor> cache = new ArrayList<>();
        final ServiceLoader<ComputedResourceProviderDescriptor> loader = ServiceLoader.load(ComputedResourceProviderDescriptor.class);
        for(final ComputedResourceProviderDescriptor factory : loader){
            cache.add(factory);
        }
        COMPUTED_DESCRIPTORS = Collections.unmodifiableCollection(cache);
    }

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        final List<ParameterDescriptorGroup> descs = new ArrayList<>();
        final Iterator<ComputedResourceProviderDescriptor> ite = COMPUTED_DESCRIPTORS.iterator();
        while (ite.hasNext()) {
            ComputedResourceProviderDescriptor provider = ite.next();

            //copy the descriptor with a minimum number of zero
            final ParameterDescriptorGroup desc = provider.getOpenParameters();

            builder.addName(desc.getName());
            for (GenericName alias : desc.getAlias()) {
                builder.addName(alias);
            }
            final ParameterDescriptorGroup mindesc = builder.createGroup(0, 1, desc.descriptors().toArray(new GeneralParameterDescriptor[0]));

            descs.add(mindesc);
        }

        SOURCE_CONFIG_DESCRIPTOR = builder.addName("choice").setRequired(true)
                .createGroup(descs.toArray(new GeneralParameterDescriptor[descs.size()]));

    }

    public static final ParameterDescriptorGroup SERVICE_CONFIG_DESCRIPTOR = createDescriptor(SOURCE_CONFIG_DESCRIPTOR);

    public ComputedResourceProviderService(){
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

        String identifier = null;
        ParameterValueGroup config = null;
        if (!ps.groups("choice").isEmpty() &&
            !ps.groups("choice").get(0).values().isEmpty()) {
            identifier = ps.groups("choice").get(0).values().get(0).getDescriptor().getName().getCode();
            config =  (ParameterValueGroup) ps.groups("choice").get(0).values().get(0);
        }

        ComputedResourceProviderDescriptor candidate = null;
        for (ComputedResourceProviderDescriptor desc : COMPUTED_DESCRIPTORS) {
            if (desc.getName().equals(identifier)) {
                candidate = desc;
                break;
            }
        }

        try {
            if (candidate != null) {
                final ComputedResourceProvider provider = candidate.buildProvider(providerId,this,config);
                getLogger().log(Level.INFO, "[PROVIDER]> computed-resource {0} provider created.", providerId);
                return provider;
            } else {
                getLogger().log(Level.SEVERE, "[PROVIDER]> Invalid computed-resource provider config (not found)");
            }
        } catch (Exception ex) {
            // we should not catch exception, but here it's better to start all source we can
            // rather than letting a potential exception block the provider proxy
            getLogger().log(Level.SEVERE, "[PROVIDER]> Invalid computed-resource provider config", ex);
        }
        return null;
    }

}
