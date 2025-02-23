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
package org.constellation.wcs.component;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.constellation.ws.ConstellationOGCModule;
import org.springframework.stereotype.Component;

@Component
public class WCSConstellationOGCModule implements ConstellationOGCModule {

    @Override
    public String getName() {
        return "WCS";
    }

    @Override
    public boolean isRestService() {
        return true;
    }

    @Override
    public Set<String> getVersions() {
        return ImmutableSet.of("1.0.0", "2.0.1");
    }
}
