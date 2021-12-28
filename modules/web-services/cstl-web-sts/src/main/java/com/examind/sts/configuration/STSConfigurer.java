/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package com.examind.sts.configuration;

import com.examind.sensor.configuration.SensorServiceConfigurer;
import static com.examind.sts.core.STSConstants.STS_VERSION;
import org.constellation.dto.service.ServiceComplete;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STSConfigurer extends SensorServiceConfigurer {

    @Override
    protected String getServiceUrl(ServiceComplete service) {
        String url = super.getServiceUrl(service);
        return url + '/' + STS_VERSION;
    }
}
