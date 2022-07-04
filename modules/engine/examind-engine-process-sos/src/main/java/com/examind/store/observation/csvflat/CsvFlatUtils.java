/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.examind.store.observation.csvflat;

import com.examind.store.observation.DataFileReader;
import com.examind.store.observation.FileParsingUtils;
import java.io.IOException;
import java.nio.file.Path;

import java.util.*;
import java.util.logging.Logger;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.Util;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvFlatUtils {
    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation.csvflat");

    public static Set<String> extractCodes(String format, Path dataFile, Collection<String> measureCodeColumns, Character separator, Character quoteChar, boolean noHeader, boolean directColumnIndex) throws ConstellationStoreException {
        try (final DataFileReader reader = FileParsingUtils.getDataFileReader(format, dataFile, separator, quoteChar)) {

            // read headers
            final String[] headers = reader.getHeaders();
            List<Integer> measureCodeIndex = new ArrayList<>();

            // find measureCodeIndex
            for (int i = 0; i < headers.length; i++) {
                final String header = headers[i];

                if ((directColumnIndex && measureCodeColumns.contains(Integer.toString(i))) ||
                    (!directColumnIndex && measureCodeColumns.contains(header))) {
                    measureCodeIndex.add(i);
                }
            }

            if (measureCodeIndex.size() != measureCodeColumns.size()) {
                throw new ConstellationStoreException("csv headers does not contains All the Measure Code parameter.");
            }

            final Set<String> storeCode = new HashSet<>();

            // extract all codes
            final Iterator<String[]> it = reader.iterator(!noHeader);
            line:while (it.hasNext()) {
                final String[] line = it.next();
                String computed = "";
                boolean first = true;
                for(Integer i : measureCodeIndex) {
                    final String nextCode = line[i];
                    if (nextCode == null || nextCode.isEmpty()) continue line;
                    if (!first) {
                        computed += "-";
                    }
                    computed += nextCode;
                    first = false;
                }
                if (!Util.containsForbiddenCharacter(computed) && computed.indexOf('.') == -1 && computed.length() < 64) {
                    storeCode.add(computed);
                } else {
                    LOGGER.warning("Invalid measure column value excluded: " + computed);
                }
            }
            return storeCode;
            
        } catch (IOException ex) {
            throw new ConstellationStoreException("problem reading csv file", ex);
        }
    }
}
