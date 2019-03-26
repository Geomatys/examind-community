package org.constellation.ws.component;

import org.constellation.ws.ConstellationOGCModule;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.sis.util.logging.Logging;
import org.constellation.ws.IWSEngine;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.ws.Worker;

@Named
public class ConstellationOGCRegistry {

    private final static Logger LOGGER = Logging.getLogger("org.constellation.ws.component");

    @Autowired(required = false)
    private Map<String,ConstellationOGCModule> constellationOGCModules = new HashMap<>();

    @Inject
    private IWSEngine wsengine;
    @Inject
    private IServiceBusiness serviceBusiness;

    @PostConstruct
    public void init() {
        LOGGER.info(constellationOGCModules.size() + " Constellation OGC module"
                + (constellationOGCModules.size() > 1 ? "s" : "") + " detected.");

        LOGGER.info("=== Start service instances ===");
        for (Entry<String, ? extends ConstellationOGCModule> moduleEntry : constellationOGCModules.entrySet()) {
            final ConstellationOGCModule ogcMod = moduleEntry.getValue();
            final String modName = ogcMod.getName().toUpperCase();
            LOGGER.info(String.format("\t* %-5s (%s)", modName , moduleEntry.getKey()));
            if(ogcMod.isRestService()) wsengine.registerService(modName, "REST");
            if(ogcMod.isSoapService()) wsengine.registerService(modName, "SOAP");

            // Start service instances
            startInstances(modName);
        }
    }

    private void startInstances(String serviceName){
        if (!wsengine.isSetService(serviceName)) {
            try {
                final String serviceType = serviceName.toLowerCase();
                for(ServiceComplete service : serviceBusiness.getAllServicesByType(null, serviceType)){
                    if("STARTED".equalsIgnoreCase(service.getStatus())){
                        final String identifier = service.getIdentifier();

                        final Worker worker = wsengine.buildWorker(serviceType, identifier);
                        if (worker != null) {
                            wsengine.addServiceInstance(serviceType, identifier, worker);
                        } else {
                            throw new ConfigurationException("The instance " + identifier + " can not be instanciated.");
                        }
                    }
                }
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, "Error while starting services for :" + serviceName, ex);
            }
        } else {
            LOGGER.log(Level.INFO, "Workers already set for {0}", serviceName);
        }
    }

}
