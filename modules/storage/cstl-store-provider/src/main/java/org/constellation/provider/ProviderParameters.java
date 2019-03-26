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

package org.constellation.provider;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import java.util.List;


/**
 * General parameters for provider configuration files.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ProviderParameters {

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    ////////////////////////////////////////////////////////////////////////////
    // Source parameters ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    public static final String SOURCE_DESCRIPTOR_NAME = "source";
    //TODO remove this parameter, duplicates argument in factory storeProvider method.
    /** /!\ NO ! Keep ALL the arguments, because we need them for old configuration import.
     */
    public static final ParameterDescriptor<String> SOURCE_ID_DESCRIPTOR =
            BUILDER.addName("id").setRemarks("source id").setRequired(true).create(String.class, null);
    public static final ParameterDescriptor<String> SOURCE_TYPE_DESCRIPTOR =
            BUILDER.addName("providerType").setRemarks("provider type").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<Boolean> SOURCE_CREATEDATASET_DESCRIPTOR =
            BUILDER.addName("create_dataset").setRemarks("optional internal parameters").setRequired(false).create(Boolean.class, null);

    private ProviderParameters(){}

    /**
     * Create a descriptor composed of the given source configuration.
     * Source
     *  - config
     *  - layers
     */
    public static ParameterDescriptorGroup createDescriptor(final GeneralParameterDescriptor sourceConfigDescriptor){
        return BUILDER.addName(SOURCE_DESCRIPTOR_NAME).setRequired(true).createGroup(1, Integer.MAX_VALUE,
                SOURCE_ID_DESCRIPTOR,SOURCE_TYPE_DESCRIPTOR,
                SOURCE_CREATEDATASET_DESCRIPTOR, sourceConfigDescriptor);
    }

    public static ParameterValueGroup getSourceConfiguration(
            final ParameterValueGroup group,final ParameterDescriptorGroup desc){
        final List<ParameterValueGroup> groups = group.groups(desc.getName().getCode());
        if(!groups.isEmpty()){
            return groups.get(0);
        }else{
            return null;
        }
    }

    public static String getNamespace(final DataProvider provider) {
        ParameterValueGroup group = provider.getSource();

        // Get choice if exists.
        try {
            group = group.groups("choice").get(0);
        } catch (ParameterNotFoundException ignore) {
        }

        // Get provider type configuration.
        final List<GeneralParameterValue> values = group.values();
        for (final GeneralParameterValue value : values) {
            if (value instanceof ParameterValueGroup) {
                group = (ParameterValueGroup) value;
            }
        }

        // Get namespace.
        try {
            final String namespace = group.parameter("namespace").stringValue();
            return "no namespace".equals(namespace) ? null : namespace;
        } catch (ParameterNotFoundException | IllegalStateException ignore) {
        }

        // Return default.
        return null;
    }

    public static List<ParameterValueGroup> getSources(final ParameterValueGroup config){
        return config.groups(SOURCE_DESCRIPTOR_NAME);
    }

    public static ParameterValueGroup getOrCreate(final ParameterDescriptorGroup desc,
            final ParameterValueGroup parent){

        ArgumentChecks.ensureBetween("descriptor occurences", 0, 1, desc.getMaximumOccurs());

        final String code = desc.getName().getCode();
        final List<ParameterValueGroup> candidates = parent.groups(desc.getName().getCode());

        if(candidates.isEmpty()){
            return parent.addGroup(code);
        }else{
            return candidates.get(0);
        }

    }
}
