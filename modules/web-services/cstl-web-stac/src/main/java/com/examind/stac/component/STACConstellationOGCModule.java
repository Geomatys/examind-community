package com.examind.stac.component;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.constellation.ws.ConstellationOGCModule;
import org.springframework.stereotype.Component;

/**
 * Declares the STAC service to the admin UI / service registry as a REST-based
 * OGC-family module.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@Component
public class STACConstellationOGCModule implements ConstellationOGCModule {

    @Override
    public String getName() {
        return "STAC";
    }

    @Override
    public boolean isRestService() {
        return true;
    }

    @Override
    public Set<String> getVersions() {
        return ImmutableSet.of("1.0.0");
    }
}
