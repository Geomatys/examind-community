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
package org.constellation.store.observation.db;

import java.util.logging.Logger;
import org.constellation.store.observation.db.OM2BaseReader.ProcedureInfo;
import org.geotoolkit.temporal.object.ISODateParser;

/**
 * Base class for handling measure results.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public abstract class OM2MeasureHandler {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    protected final ProcedureInfo pi;
    protected final String schemaPrefix;

    protected final ISODateParser dateParser = new ISODateParser();

    // calculated first measure table name
    protected final String baseTableName;

    public OM2MeasureHandler(ProcedureInfo pi, String schemaPrefix) {
        this.pi = pi;
        this.schemaPrefix = schemaPrefix;
        this.baseTableName = "mesure" + pi.pid;
    }
}
