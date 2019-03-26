/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.store.metadata;

import org.apache.sis.parameter.ParameterBuilder;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class CstlMetadataStoreDescriptors {
    
    private static final ParameterBuilder BUILDER = new ParameterBuilder();
    
    public static final ParameterDescriptor<String> EXTRA_QUERYABLE_KEY = BUILDER
            .addName("extra-queryable-key")
            .setRemarks("additional queryable key")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String[]> EXTRA_QUERYABLE_VALUE = 
            BUILDER
            .addName("extra-queryable-value")
            .setRemarks("additional queryable value")
            .setRequired(false)
            .create(String[].class, null);
    
    public static final ParameterDescriptor<Class> EXTRA_QUERYABLE_TYPE = BUILDER
            .addName("extra-queryable-type")
            .setRemarks("additional queryable type")
            .setRequired(false)
            .create(Class.class, null);
    
    public static final ParameterDescriptorGroup EXTRA_QUERYABLE =
            BUILDER.addName("extra-queryable").createGroup(0, Integer.MAX_VALUE, EXTRA_QUERYABLE_KEY, EXTRA_QUERYABLE_VALUE, EXTRA_QUERYABLE_TYPE);
}
