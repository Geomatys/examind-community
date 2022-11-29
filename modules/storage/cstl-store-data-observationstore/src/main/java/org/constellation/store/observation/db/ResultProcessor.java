/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.store.observation.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.observation.model.TextEncoderProperties.CSV_ENCODING;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ResultProcessor {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
    protected ResultBuilder values = null;
    protected final List<Field> fields;
    protected final boolean profile;
    protected final boolean includeId;
    protected final boolean includeQuality;

    public ResultProcessor(List<Field> fields, boolean profile, boolean includeId, boolean includeQuality) {
        this.fields = fields;
        this.profile = profile;
        this.includeId = includeId;
        this.includeQuality = includeQuality;
    }

    public ResultBuilder initResultBuilder(String responseFormat, boolean countRequest) {
        if ("resultArray".equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
        } else if ("text/csv".equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.CSV, CSV_ENCODING, true);
            // Add the header
            values.appendHeaders(fields);
        } else if (countRequest) {
            values = new ResultBuilder(ResultMode.COUNT, null, false);
        } else {
            values = new ResultBuilder(ResultMode.CSV, DEFAULT_ENCODING, false);
        }
        return values;
    }

    public void processResults(ResultSet rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        FieldParser parser = new FieldParser(fields, values, false, includeId, includeQuality, null);
        while (rs.next()) {
            if (includeId) {
                String name = rs.getString("identifier");
                parser.setName(name);
            }
            parser.parseLine(rs, 0);
        }
    }
}
