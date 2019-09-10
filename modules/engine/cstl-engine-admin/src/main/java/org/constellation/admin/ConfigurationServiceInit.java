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
package org.constellation.admin;

import org.constellation.configuration.ConfigDirectory;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;

/**
 * This class wires spring managed beans with legacy code.
 *
 * @author Olivier NOUGUIER
 * @author Johann Sorel (Geomatys)
 *
 */
public class ConfigurationServiceInit {

    private final static Logger LOGGER = Logging.getLogger("org.constellation.admin");

    public void init() {
        LOGGER.info("=== Configure directory ===");
        ConfigDirectory.init();
    }

}
