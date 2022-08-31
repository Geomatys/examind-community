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
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getCsvTextEncoding;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getDefaultTextEncoding;

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

    public ResultProcessor(List<Field> fields, boolean profile, boolean includeId) {
        this.fields = fields;
        this.profile = profile;
        this.includeId = includeId;
    }

    public ResultBuilder initResultBuilder(String responseFormat, boolean countRequest) {
        if ("resultArray".equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
        } else if ("text/csv".equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.CSV, getCsvTextEncoding("2.0.0"), true);
            // Add the header
            values.appendHeaders(fields);
        } else if (countRequest) {
            values = new ResultBuilder(ResultMode.COUNT, null, false);
        } else {
            values = new ResultBuilder(ResultMode.CSV, getDefaultTextEncoding("2.0.0"), false);
        }
        return values;
    }

    public void processResults(ResultSet rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        FieldParser parser = new FieldParser(fields, false, includeId, null);
        while (rs.next()) {
            if (includeId) {
                String name = rs.getString("identifier");
                parser.setName(name);
            }
            parser.parseLine(values, rs, 0);
        }
    }
}
