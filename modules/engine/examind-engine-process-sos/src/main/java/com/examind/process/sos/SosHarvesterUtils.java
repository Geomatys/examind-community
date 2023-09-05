/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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
package com.examind.process.sos;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvesterUtils {

    /**
     * Return the observation provider for the specified sensor service.
     *
     * @param serviceID Sensor service identifier.
     * @param serviceBusiness
     * 
     * @return
     * @throws ConfigurationException If the service is not linked to an Observation provider.
     */
    public static ObservationProvider getServiceOMProvider(final Integer serviceID, IServiceBusiness serviceBusiness) throws ConfigurationException {
        final List<Integer> providers = serviceBusiness.getLinkedProviders(serviceID);
        for (Integer providerID : providers) {
            final DataProvider p = DataProviders.getProvider(providerID);
            if (p instanceof ObservationProvider omp) {
                // TODO for now we only take one provider by type
                return omp;
            }
        }
        throw new ConfigurationException("there is no OM provider linked to this ID:" + serviceID);
    }


    public static class SensorService {
        public ObservationProvider provider;
        public final List<ServiceProcessReference> services;

        public SensorService(ObservationProvider provider, final List<ServiceProcessReference> services) {
            this.provider = provider;
            this.services = services;
        }
        
        public String getServiceNames() {
            StringJoiner sb = new StringJoiner(",");
            for (ServiceProcessReference spr : services) {
                sb.add(spr.getName());
            }
            return sb.toString();
        }
    }


}
