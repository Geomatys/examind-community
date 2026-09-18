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

import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.examind.community.storage.csv.CsvAggregateStore.Config;

/**
 * Unit tests for {@link CsvAggregateStore#parseConfig(Parameters)}: the parsing and
 * validation of the {@code advancedParameters} (tableName, spatialMode, latColumn,
 * lonColumn, epsg, delimiter) that do not require a database connection to resolve.
 *
 * <p>Each test builds a {@link Parameters} value from {@link CsvAggregateProvider#INPUT}
 * (the same descriptor group Examind uses to configure the provider), sets only the
 * fields relevant to the case under test, and calls {@code parseConfig} directly - the
 * database-dependent parts of {@link CsvAggregateStore} (datasource lookup, table
 * probing) are never involved.</p>
 *
 * @author Quentin Bialota (Geomatys)
 */
public class CsvAggregateStoreConfigTest {

    private static Parameters newParameters() {
        return Parameters.castOrWrap(CsvAggregateProvider.INPUT.createValue());
    }

    /**
     * When only the mandatory {@code tableName} is set, every optional parameter must
     * fall back to its documented default: {@code spatialMode=false}, {@code
     * latColumn=latitude}, {@code lonColumn=longitude}, {@code epsg=4326}, {@code
     * delimiter=','}.
     */
    @Test
    public void defaultsAreAppliedWhenOptionalParametersAreAbsent() throws DataStoreException {
        Parameters p = newParameters();
        p.getOrCreate(CsvAggregateProvider.TABLE_NAME).setValue("test");

        Config config = CsvAggregateStore.parseConfig(p);

        assertEquals("test", config.tableName());
        assertFalse(config.spatialMode());
        assertEquals("latitude", config.latColumn());
        assertEquals("longitude", config.lonColumn());
        assertEquals(4326, config.epsg());
        assertEquals(',', config.delimiter());
    }

    /**
     * Every optional parameter explicitly set must override its default, and only the
     * first character of {@code delimiter} must be kept when a longer string is given.
     */
    @Test
    public void explicitParametersOverrideDefaults() throws DataStoreException {
        Parameters p = newParameters();
        p.getOrCreate(CsvAggregateProvider.TABLE_NAME).setValue("test");
        p.getOrCreate(CsvAggregateProvider.SPATIAL_MODE).setValue(Boolean.TRUE);
        p.getOrCreate(CsvAggregateProvider.LAT_COLUMN).setValue("lat");
        p.getOrCreate(CsvAggregateProvider.LON_COLUMN).setValue("lon");
        p.getOrCreate(CsvAggregateProvider.EPSG).setValue(2154);
        p.getOrCreate(CsvAggregateProvider.DELIMITER).setValue(";multi-char");

        Config config = CsvAggregateStore.parseConfig(p);

        assertTrue(config.spatialMode());
        assertEquals("lat", config.latColumn());
        assertEquals("lon", config.lonColumn());
        assertEquals(2154, config.epsg());
        assertEquals(';', config.delimiter());
    }

    /**
     * An empty {@code delimiter} string must fall back to the default {@code ','}
     * rather than throwing {@code StringIndexOutOfBoundsException} on {@code charAt(0)}.
     */
    @Test
    public void emptyDelimiterFallsBackToDefault() throws DataStoreException {
        Parameters p = newParameters();
        p.getOrCreate(CsvAggregateProvider.TABLE_NAME).setValue("stations");
        p.getOrCreate(CsvAggregateProvider.DELIMITER).setValue("");

        Config config = CsvAggregateStore.parseConfig(p);

        assertEquals(',', config.delimiter());
    }

    /**
     * A {@code tableName} that is not a plain letter-led identifier must be rejected -
     * this is the SQL injection guard, since {@code tableName} is later concatenated
     * into DDL/DML by {@link CsvAggregateStore}.
     */
    @Test
    public void tableNameWithSpaceIsRejected() {
        assertTableNameRejected("stations table");
    }

    @Test
    public void tableNameStartingWithDigitIsRejected() {
        assertTableNameRejected("1stations");
    }

    @Test
    public void tableNameWithSqlInjectionAttemptIsRejected() {
        assertTableNameRejected("stations\"; DROP TABLE users; --");
    }

    private static void assertTableNameRejected(String tableName) {
        Parameters p = newParameters();
        p.getOrCreate(CsvAggregateProvider.TABLE_NAME).setValue(tableName);
        try {
            CsvAggregateStore.parseConfig(p);
            fail("Expected a DataStoreException for invalid table name: " + tableName);
        } catch (DataStoreException ex) {
            assertTrue(ex.getMessage().contains(tableName));
        }
    }
}
