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

import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.provider.DataProviderFactory;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class ComputedResourceProviderDescriptor {

    protected static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<Integer> DATA_IDS =
            new ExtendedParameterDescriptor<>("data_ids", "data identifiers", 2, Integer.MAX_VALUE, Integer.class, null, null, null);

    public static final ParameterDescriptor<String> DATA_NAME =
             BUILDER.addName("DataName").setRemarks("Data Name").setRequired(false).create(String.class,null);


    public abstract ParameterDescriptorGroup getOpenParameters();

    public abstract ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param);

    public abstract String getName();

}
