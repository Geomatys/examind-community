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

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import org.geotoolkit.data.dbf.DbaseFileHeader;
import org.geotoolkit.data.dbf.DbaseFileReader;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DBFDataFileReader implements DataFileReader {

    private final DbaseFileReader reader;

    public DBFDataFileReader(Path dataFile) throws IOException {
        SeekableByteChannel sbc = Files.newByteChannel(dataFile, StandardOpenOption.READ);
        reader  = new DbaseFileReader(sbc, true, null);
    }

    @Override
    public Iterator<String[]> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getHeaders() throws IOException {
        final DbaseFileHeader headers = reader.getHeader();

        final String[] results = new String[headers.getNumFields()];
        for (int i = 0; i < headers.getNumFields(); i++) {
            results[i] = headers.getFieldName(i);
        }
        return results;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
