package org.constellation.map.core;

import org.constellation.ws.ConstellationOGCModule;

import javax.inject.Named;

;

@Named
public class WMSConstellationOGCModule implements ConstellationOGCModule {

    @Override
    public String getName() {
        return "WMS";
    }

    @Override
    public boolean isRestService() {
        return true;
    }
}
