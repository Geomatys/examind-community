/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.process.service;

import org.constellation.business.IServiceBusiness;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractServiceProcess extends AbstractCstlProcess {

    @Autowired
    public IServiceBusiness serviceBusiness;

    public AbstractServiceProcess(ProcessDescriptor desc, ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    /**
     * Look for a service database identifier from the serviceType / identifier.
     * If not found it will throw a process exception.
     *
     * @param serviceType type of the service like WMS, CSW.....
     * @param identifier String identifier of the service.
     *
     * @return An db identifier.
     * @throws ProcessException If the service can't be found.
     */
    protected Integer getServiceId(String serviceType, String identifier) throws ProcessException {
        Integer sid = serviceBusiness.getServiceIdByIdentifierAndType(serviceType.toLowerCase(), identifier);
        if (sid == null) {
            throw new ProcessException("Unexisting service", this);
        }
        return sid;
    }
}
