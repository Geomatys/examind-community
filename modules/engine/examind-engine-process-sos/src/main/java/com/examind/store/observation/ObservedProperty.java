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
package com.examind.store.observation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservedProperty {

    public final String id;

    public final String name;

    public final String description;

    public final String uom;

    public final Map<String, Object> properties;

    public ObservedProperty(String id, String name, String uom) {
        this(id, name, uom, null, new HashMap<>());
    }

    public ObservedProperty(String id, String name, String uom, String description, Map<String, Object> properties) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.uom = uom;
        this.properties = properties;
    }
}
