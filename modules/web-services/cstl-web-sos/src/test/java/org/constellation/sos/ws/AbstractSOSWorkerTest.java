/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package org.constellation.sos.ws;

import java.util.logging.Logger;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.test.SpringContextTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractSOSWorkerTest extends SpringContextTest {
    
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.sos.ws");
    
    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected ISensorBusiness sensorBusiness;
    @Autowired
    protected IDatasourceBusiness datasourceBusiness;

    protected static final int NB_SENSOR = 19;
}
