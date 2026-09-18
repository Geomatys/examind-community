/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
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
package com.examind.community.storage.csv;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * DataStoreProvider that aggregates multiple CSV files sharing the same schema
 * into a single PostGIS table, then exposes it as an Examind data product.
 *
 * <p>Config YAML example:</p>
 * <pre>{@code
 * identifier: "stations-agg"
 * dataType:   "data-store"
 * providerType: "csv-aggregate"
 * location:   "/data/csv/stations"
 * dataset:    "stations"
 * source:
 *   location: "postgresql://localhost:5432/examind"
 *   userName: "postgres"
 *   password: "secret"
 * advancedParameters:
 *   tableName:   "stations_aggregated"
 *   spatialMode: "true"
 *   latColumn:   "latitude"
 *   lonColumn:   "longitude"
 *   epsg:        "4326"
 *   delimiter:   ";"
 * }</pre>
 *
 * @author Quentin Bialota (Geomatys)
 */
public class CsvAggregateProvider extends DataStoreProvider {

    public static final String NAME = "csv-aggregate";

    public static final ParameterDescriptor<Integer> DATASOURCE_ID;
    public static final ParameterDescriptor<String>  TABLE_NAME;
    public static final ParameterDescriptor<String>  CSV_DIRECTORY;
    public static final ParameterDescriptor<Boolean> SPATIAL_MODE;
    public static final ParameterDescriptor<String>  LAT_COLUMN;
    public static final ParameterDescriptor<String>  LON_COLUMN;
    public static final ParameterDescriptor<Integer> EPSG;
    public static final ParameterDescriptor<String>  DELIMITER;
    public static final ParameterDescriptorGroup     INPUT;

    static {
        final ParameterBuilder b = new ParameterBuilder();
        DATASOURCE_ID = b.addName("datasourceId").setRequired(true).create(Integer.class, null);
        TABLE_NAME    = b.addName("tableName").setRequired(true).create(String.class, null);
        CSV_DIRECTORY = b.addName("csvDirectory").setRequired(false).create(String.class, null);
        SPATIAL_MODE  = b.addName("spatialMode").setRequired(false).create(Boolean.class, Boolean.FALSE);
        LAT_COLUMN    = b.addName("latColumn").setRequired(false).create(String.class, "latitude");
        LON_COLUMN    = b.addName("lonColumn").setRequired(false).create(String.class, "longitude");
        EPSG          = b.addName("epsg").setRequired(false).create(Integer.class, 4326);
        DELIMITER     = b.addName("delimiter").setRequired(false).create(String.class, ",");
        INPUT = b.addName(NAME).createGroup(
                DATASOURCE_ID, TABLE_NAME, CSV_DIRECTORY,
                SPATIAL_MODE, LAT_COLUMN, LON_COLUMN, EPSG, DELIMITER);
    }

    private final SQLStoreProvider sisProvider = new SQLStoreProvider();

    @Override
    public String getShortName() { return NAME; }

    @Override
    public ParameterDescriptorGroup getOpenParameters() { return INPUT; }

    @Override
    public ProbeResult probeContent(StorageConnector sc) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        throw new UnsupportedOperationException("Use open(ParameterValueGroup) instead.");
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        return new CsvAggregateStore(sisProvider, Parameters.castOrWrap(parameters));
    }
}
