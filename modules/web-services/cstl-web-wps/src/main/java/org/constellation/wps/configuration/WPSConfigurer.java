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

package org.constellation.wps.configuration;

import java.util.Iterator;
import java.util.logging.Level;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.ogc.configuration.OGCConfigurer;
import com.examind.wps.util.WPSUtils;
import org.constellation.ws.IWPSConfigurer;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;

/**
 * {@link OGCConfigurer} implementation for WPS service.
 *
 * TODO: implement specific configuration methods
 *
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public class WPSConfigurer extends OGCConfigurer implements IWPSConfigurer {

    /**
     * Create a new {@link WPSConfigurer} instance.
     */
    protected WPSConfigurer() {
    }

    @Override
    public Instance getInstance(final Integer id, final String lang) throws ConfigurationException {
        final Instance instance = super.getInstance(id, lang);
        try {
            instance.setLayersNumber(getProcessCount(id));
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting process count on WPS instance:" + id, ex);
        }
        return instance;
    }

    @Override
    public int getProcessCount(Integer id) throws ConfigurationException {
        final ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration(id);
        int count = 0;
        if (Boolean.TRUE.equals(context.getProcesses().getLoadAll()) ) {
            for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext();) {
                final ProcessingRegistry processingRegistry = it.next();
                for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                    if (WPSUtils.isSupportedProcess(descriptor)) {
                        count++;
                    }
                }
            }
        } else {
            for (ProcessFactory pFacto : context.getProcessFactories()) {
                final ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory(pFacto.getAutorityCode());
                if (pFacto.getLoadAll()) {
                    for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                        if (WPSUtils.isSupportedProcess(descriptor)) {
                            count++;
                        }
                    }
                } else {
                    count = count + pFacto.getInclude().getProcess().size();
                }
            }
        }
        return count;
    }

}
