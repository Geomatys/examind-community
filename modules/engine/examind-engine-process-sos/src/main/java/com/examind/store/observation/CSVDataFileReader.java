/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

import com.opencsv.CSVReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CSVDataFileReader implements DataFileReader {

    private final CSVReader reader;
    public CSVDataFileReader(Path dataFile, char delimiter, char quote) throws IOException {
        this.reader = new CSVReader(Files.newBufferedReader(dataFile), delimiter, quote);
    }

    @Override
    public Iterator<String[]> iterator() {
        return reader.iterator();
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    @Override
    public String[] getHeaders() throws IOException {
        final Iterator<String[]> it = reader.iterator();

        // at least one line is expected to contain headers information
        if (it.hasNext()) {

            // read headers
            return it.next();
        }
        throw new IOException("csv headers not found");
    }
}
