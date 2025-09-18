/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
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

import java.sql.Connection;
import org.constellation.util.SQLResult;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.DATA_ARRAY;
import static org.constellation.api.CommonConstants.CSV;
import static org.constellation.api.CommonConstants.CSV_FLAT;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.store.observation.db.result.CsvFlatResultBuilder;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.ObservationType;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.model.TextEncoderProperties;
import static org.geotoolkit.observation.model.TextEncoderProperties.CSV_ENCODING;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ResultProcessor {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
    protected final static TextEncoderProperties CSV_FLAT_ENCODING = new TextEncoderProperties(".", ";", "\n");
    
    protected ResultBuilder values = null;
    protected final List<? extends DbField> fields;
    protected final boolean nonTimeseries;
    protected final boolean includeQuality;
    protected final boolean includeParameter;
    protected final ProcedureInfo procedure;
    
    /**
     * loaded only for some mode.
     */
    protected Map<DbField, Phenomenon> phenomenons;
    protected Map<String, Object> procedureProperties;

    public ResultProcessor(List<? extends DbField> fields, boolean includeQuality, boolean includeParameter, ProcedureInfo procedure) {
        this.fields = fields;
        this.nonTimeseries = procedure.type != ObservationType.TIMESERIES;
        this.includeQuality = includeQuality;
        this.includeParameter = includeParameter;
        this.procedure = procedure;
    }

    
    public ResultBuilder initResultBuilder(String responseFormat, boolean countRequest) {
        if (DATA_ARRAY.equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
        } else if (CSV.equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.CSV, CSV_ENCODING, true);
            // Add the header
            values.appendHeaders(fields.stream().map(f -> (Field)f).toList());
        } else if (CSV_FLAT.equals(responseFormat)) {
            values = new CsvFlatResultBuilder(procedure, fields, phenomenons, procedureProperties, CSV_FLAT_ENCODING);
            // Add the header
            values.appendHeaders(fields.stream().map(f -> (Field)f).toList());
        } else if (countRequest) {
            values = new ResultBuilder(ResultMode.COUNT, null, false);
        } else {
            values = new ResultBuilder(ResultMode.CSV, DEFAULT_ENCODING, false);
        }
        return values;
    }

    public void computeRequest(FilterSQLRequest sqlRequest, Connection c) throws SQLException {
        // nothing in this implementation
    }
    
    public void processResults(SQLResult rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        FieldParser parser = new FieldParser(fields, values, false, includeQuality, includeParameter, null);
        while (rs.nextOnField(procedure.mainField.name)) {
            parser.parseLine(rs);
        }
    }
    
    public void setPhenomenons(Map<DbField, Phenomenon> phenomenons) {
        this.phenomenons = phenomenons;
    }
    
    public void setProcedureProperties(Map<String, Object> procProp) {
        this.procedureProperties = procProp;
    }
}
