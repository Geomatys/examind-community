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
import static org.constellation.store.observation.db.OM2Utils.IDENTIFIER_FIELD_NAME;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.store.observation.db.result.CsvFlatResultBuilder;
import org.constellation.util.FilterSQLRequest;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.temp.ObservationType;
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
    protected final List<Field> fields;
    protected final boolean nonTimeseries;
    protected final boolean includeId;
    protected final boolean includeTimeInProfile;
    protected final boolean includeQuality;
    protected final boolean includeParameter;
    protected final ProcedureInfo procedure;
    protected final int mainFieldIndex;
    protected final String idSuffix;
    
    /**
     * loaded only for some mode.
     */
    protected Map<Field, Phenomenon> phenomenons;
    protected Map<String, Object> procedureProperties;

    public ResultProcessor(List<Field> fields, boolean includeId, boolean includeQuality, boolean includeParameter, boolean includeTimeInProfile, ProcedureInfo procedure, String idSuffix) {
        this.fields = fields;
        this.nonTimeseries = procedure.type != ObservationType.TIMESERIES;
        this.includeId = includeId;
        this.includeQuality = includeQuality;
        this.includeParameter = includeParameter;
        this.includeTimeInProfile = includeTimeInProfile;
        this.procedure = procedure;
        this.mainFieldIndex = fields.indexOf(procedure.mainField);
        this.idSuffix = idSuffix == null ? "" : idSuffix;
    }

    
    public ResultBuilder initResultBuilder(String responseFormat, boolean countRequest) {
        if (DATA_ARRAY.equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
        } else if (CSV.equals(responseFormat)) {
            values = new ResultBuilder(ResultMode.CSV, CSV_ENCODING, true);
            // Add the header
            values.appendHeaders(fields);
        } else if (CSV_FLAT.equals(responseFormat)) {
            values = new CsvFlatResultBuilder(procedure, fields, phenomenons, procedureProperties, CSV_FLAT_ENCODING);
            // Add the header
            values.appendHeaders(fields);
        } else if (countRequest) {
            values = new ResultBuilder(ResultMode.COUNT, null, false);
        } else {
            values = new ResultBuilder(ResultMode.CSV, DEFAULT_ENCODING, false);
        }
        return values;
    }

    public void computeRequest(FilterSQLRequest sqlRequest, int fieldOffset, Connection c) throws SQLException {
        String mainFieldSelect = "m.\"" + procedure.mainField.name + "\"";
        StringBuilder select  = new StringBuilder(mainFieldSelect);
        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        if (nonTimeseries) {
            select.append(", o.\"id\" as oid ");
            if (includeTimeInProfile) {
                select.append(", o.\"time_begin\" ");
            }
            orderBy.append(" o.\"time_begin\", ");
        }
        if (includeId) {
            select.append(", o.\"identifier\" ");
        }
        // always order by main field
        orderBy.append("\"").append(procedure.mainField.name).append("\"");
        
        sqlRequest.replaceFirst(mainFieldSelect, select.toString());
        sqlRequest.append(orderBy.toString());
        sqlRequest.cleanupWhere();
    }
    
    public void processResults(SQLResult rs, int fieldOffset) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        FieldParser parser = new FieldParser(mainFieldIndex, fields, values, false, includeId, includeQuality, includeParameter, null, fieldOffset);
        while (rs.nextOnField(procedure.mainField.name)) {
            if (includeId) {
                String name = rs.getString(IDENTIFIER_FIELD_NAME);
                parser.setName(name + idSuffix);
            }
            parser.parseLine(rs);
        }
    }
    
    public void setPhenomenons(Map<Field, Phenomenon> phenomenons) {
        this.phenomenons = phenomenons;
    }
    
    public void setProcedureProperties(Map<String, Object> procProp) {
        this.procedureProperties = procProp;
    }
}
