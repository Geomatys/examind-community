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
import static com.examind.store.observation.FileParsingUtils.asString;
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

            List<Integer> measureCodeIndex = new ArrayList<>();
            
            // read headers
            String[] headers = null;

            // find measureCodeIndex
            if (!noHeader) {
                headers = reader.getHeaders();

                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if ((directColumnIndex && measureCodeColumns.contains(Integer.toString(i))) ||
                        (!directColumnIndex && measureCodeColumns.contains(header))) {
                        measureCodeIndex.add(i);
                    }
                }
            } else {
                // implying direct column index
                for (String i : measureCodeColumns) {
                    measureCodeIndex.add(Integer.valueOf(i));
                }
            }

            if (measureCodeIndex.size() != measureCodeColumns.size()) {
                throw new ConstellationStoreException("csv headers does not contains All the Measure Code parameter.");
            }

            final Set<String> storeCode = new HashSet<>();

            // extract all codes
            final Iterator<Object[]> it = reader.iterator(!noHeader);
            long lineNb = 0L;
            line:while (it.hasNext()) {
                lineNb++;
                final Object[] line = it.next();
                if (line.length == 0) {
                    LOGGER.finer("skipping empty line " + lineNb);
                    continue;
                } else if (headers != null && line.length < headers.length) {
                    LOGGER.finer("skipping imcomplete line " + lineNb + " (" +line.length + "/" + headers.length + ")");
                    continue;
                }
                
                String computed = "";
                boolean first = true;
                for(Integer i : measureCodeIndex) {
                    final String nextCode = asString(line[i]);
                    if (nextCode == null || nextCode.isEmpty()) {
                        LOGGER.warning("Invalid measure ignore due to missing value at line " + lineNb + " column " + i);
                        continue line;
                    } else if (Util.containsForbiddenCharacter(nextCode)) {
                        LOGGER.warning("Invalid measure ignored due to invalid character. Value: " + nextCode + " at line " + lineNb + " column " + i);
                        continue line;
                    }
                    if (!first) {
                        computed += "-";
                    }
                    computed += nextCode;
                    first = false;
                }
                if (computed.length() < 64) {
                    storeCode.add(computed);
                } else {
                    LOGGER.warning("Invalid measure column value excluded: " + computed);
                }
            }
            return storeCode;
            
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw new ConstellationStoreException("problem reading csv file", ex);
        }
    }
}
