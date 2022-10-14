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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Interface for data file reader allowing to iterate over CSV like file.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public interface DataFileReader extends Closeable {

    /**
     * Return an iterator on the file.
     * If an iterator has already been built by this reader, it will be cahed and returned here.
     *
     * @param skipHeaders if {@code true} the header will be skipped and the next line will be a data one.
     *
     */
    Iterator<String[]> iterator(boolean skipHeaders);

    /**
     * Return the data file headers.
     * 
     * @throws IOException If a the headers can not be found.
     */
    String[] getHeaders() throws IOException;

}
