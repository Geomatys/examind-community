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
package org.constellation.metadata.index;

import java.util.Map;
import org.constellation.api.PathType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SpecificQueryablePart {

    public final Map<String, PathType> queryable;
    public final String type;
    public final boolean spatial;

    public SpecificQueryablePart(Map<String, PathType> queryable, String type, boolean spatial) {
        this.queryable = queryable;
        this.spatial   = spatial;
        this.type      = type;
    }
}
