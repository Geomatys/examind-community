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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.examind.community.storage.csv.CsvAggregateSchema.CsvSchema;

/**
 * Unit tests for {@link CsvAggregateSchema}: the cross-file schema check that
 * {@link CsvAggregateStore#loadData} runs on every aggregated CSV file before
 * touching the PostGIS table.
 *
 * <p>Each test writes small CSV files to a temp directory and calls
 * {@code CsvAggregateSchema.readSchema}/{@code resolveUnresolvedTypes}/
 * {@code validateSchema} directly, without a {@link javax.sql.DataSource} - these
 * three methods are pure file/parsing logic and do not touch the database, so no
 * PostGIS instance (there is no Testcontainers/embedded-postgres harness in this
 * repository) is required to exercise them.</p>
 *
 * @author Quentin Bialota (Geomatys)
 */
public class CsvAggregateSchemaTest {

    /** Fresh temp directory per test, so files from one test cannot leak into another. */
    private Path dir;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("csv-aggregate-schema-test");
    }

    @After
    public void tearDown() throws IOException {
        if (dir != null) {
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    /** Writes {@code content} to {@code name} under the test's temp {@link #dir}. */
    private Path writeCsv(String name, String content) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Two files with the exact same header ({@code id;name;value}) must validate
     * against each other: builds the reference schema from the first file (resolving
     * its untyped columns by sampling), then asserts {@code validateSchema} does not
     * throw for the second file.
     */
    @Test
    public void nominalMatchingSchemaIsAccepted() throws DataStoreException, IOException {
        Path reference = writeCsv("ref.csv", "id;name;value\n1;a;1.5\n2;b;2.5\n");
        Path other = writeCsv("other.csv", "id;name;value\n3;c;3.5\n");

        CsvSchema schema = CsvAggregateSchema.readSchema(reference, ';');
        CsvAggregateSchema.resolveUnresolvedTypes(reference, ';', schema, 20);
        assertEquals(List.of("id", "name", "value"), schema.columns());

        // Must not throw: same columns, same order.
        CsvAggregateSchema.validateSchema(other, ';', schema);
    }

    /**
     * A file missing a column present in the reference ({@code name}) must be
     * rejected: asserts {@code validateSchema} throws a {@link DataStoreException}
     * whose message names the offending file, so the user can locate it among
     * hundreds of aggregated files.
     */
    @Test
    public void mismatchedColumnsAreRejected() throws DataStoreException, IOException {
        Path reference = writeCsv("ref.csv", "id;name;value\n1;a;1.5\n");
        Path diverging = writeCsv("diverging.csv", "id;value\n1;1.5\n");

        CsvSchema schema = CsvAggregateSchema.readSchema(reference, ';');
        CsvAggregateSchema.resolveUnresolvedTypes(reference, ';', schema, 20);

        try {
            CsvAggregateSchema.validateSchema(diverging, ';', schema);
            fail("Expected a DataStoreException for mismatched columns");
        } catch (DataStoreException ex) {
            assertTrue(ex.getMessage().contains(diverging.toString()));
        }
    }

    /**
     * A column resolved as {@code TEXT} on the reference file (no header hint,
     * sampled non-numeric value {@code "abc"}) but explicitly hinted {@code (Double)}
     * on another file is a type conflict: asserts {@code validateSchema} throws a
     * {@link DataStoreException} naming the offending file.
     */
    @Test
    public void conflictingTypeHintIsRejected() throws DataStoreException, IOException {
        // Reference column has no hint and is resolved to TEXT by sampling ("abc").
        Path reference = writeCsv("ref.csv", "id;value\n1;abc\n");
        // Diverging file explicitly hints the same column as numeric.
        Path diverging = writeCsv("diverging.csv", "id;value(Double)\n1;1.5\n");

        CsvSchema schema = CsvAggregateSchema.readSchema(reference, ';');
        CsvAggregateSchema.resolveUnresolvedTypes(reference, ';', schema, 20);
        assertEquals("TEXT", schema.sqlTypes()[1]);

        try {
            CsvAggregateSchema.validateSchema(diverging, ';', schema);
            fail("Expected a DataStoreException for conflicting type hint");
        } catch (DataStoreException ex) {
            assertTrue(ex.getMessage().contains(diverging.toString()));
        }
    }
}
