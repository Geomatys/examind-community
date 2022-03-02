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

package org.constellation.ogc.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.ws.IOGCConfigurer;
import org.constellation.ws.ServiceConfigurer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Describe methods which need to be specify by an implementation to manage
 * service (create, set configuration, etc...).
 *
 * @author Benjamin Garcia (Geomatys).
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public abstract class OGCConfigurer extends ServiceConfigurer implements IOGCConfigurer {

    @Autowired
    protected IServiceBusiness serviceBusiness;

    @Override
    public Instance getInstance(final Integer serviceId, String lang) throws ConfigurationException {
        ServiceComplete service = serviceBusiness.getServiceById(serviceId, lang);
        Instance i = new Instance(service);
        i.setBaseUrl(getServiceUrl(service));
        return i;
    }

    protected String getServiceUrl(ServiceComplete service) {
        String result = Application.getProperty(AppProperty.CSTL_SERVICE_URL);
        if (result == null) {
            String cstlURL = Application.getProperty(AppProperty.CSTL_URL);
            if (cstlURL != null) {
                cstlURL = cstlURL.endsWith("/") ? cstlURL : cstlURL + "/";
                result = cstlURL + "WS";
            }
        } else if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "/" + service.getType() + "/" + service.getIdentifier();
    }

    /**
     * Returns list of service {@link Instance}(s) related to the {@link OGCConfigurer}
     * implementation.
     *
     * @param spec Service specification.
     * @param lang Request language.
     * 
     * @return the {@link Instance} list.
     */
    @Override
    public List<Instance> getInstances(final String spec, String lang) {
        final List<Instance> instances = new ArrayList<>();
        final Map<Integer, ServiceStatus> statusMap = serviceBusiness.getStatus(spec);
        for (final Integer key : statusMap.keySet()) {
            try {
                instances.add(getInstance(key, lang));
            } catch (ConfigurationException ignore) {
                // Do nothing.
            }
        }
        return instances;
    }
}
