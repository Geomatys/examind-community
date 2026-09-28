/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.edr.component;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.constellation.ws.ConstellationOGCModule;
import org.springframework.stereotype.Component;

/**
 * Advertises the OGC API - EDR service as an available examind service type, so it shows up
 * in the service-creation UI and the specification registry alongside WMS/WCS/DGGS/etc.
 *
 * @author Quentin Bialota (Geomatys)
 */
@Component
public class EDRConstellationOGCModule implements ConstellationOGCModule {

    @Override
    public String getName() {
        return "EDR";
    }

    @Override
    public boolean isRestService() {
        return true;
    }

    @Override
    public Set<String> getVersions() {
        return ImmutableSet.of("1.1.0");
    }
}
