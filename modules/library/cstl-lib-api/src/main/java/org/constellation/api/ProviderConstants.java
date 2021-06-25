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
package org.constellation.api;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ProviderConstants {

     public static final String INTERNAL_SENSOR_PROVIDER = "default-internal-sensor";
     public static final String INTERNAL_METADATA_PROVIDER = "default-internal-metadata";
     public static final String GENERIC_SHAPE_PROVIDER = "generic_shp";
     public static final String INTERNAL_MAP_CONTEXT_PROVIDER = "default_map_context";
     public static final String GENERIC_TIF_PROVIDER = "generic_world_tif";

     public static final List<String> DEFAULT_PROVIDERS = Arrays.asList(INTERNAL_SENSOR_PROVIDER,
                                                                        INTERNAL_METADATA_PROVIDER,
                                                                        GENERIC_SHAPE_PROVIDER,
                                                                        INTERNAL_MAP_CONTEXT_PROVIDER,
                                                                        GENERIC_TIF_PROVIDER);
}
