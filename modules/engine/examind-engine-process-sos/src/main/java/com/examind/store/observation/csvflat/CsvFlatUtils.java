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
import org.geotoolkit.util.StringUtilities;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvFlatUtils {
    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation.csvflat");

    public static Set<String> extractCodes(String format, Path dataFile, Collection<String> ObservedPropertiesColumns, Character separator, Character quoteChar, boolean noHeader, boolean directColumnIndex) throws ConstellationStoreException {
        try (final DataFileReader reader = FileParsingUtils.getDataFileReader(format, dataFile, separator, quoteChar)) {

            List<Integer> obsPropIndex = new ArrayList<>();
            
            // read headers
            String[] headers = null;
            // for error infos
            Set<String> missingHeaders = new HashSet<>(ObservedPropertiesColumns);
            // find obsPropIndex
            if (!noHeader) {
                headers = reader.getHeaders();

                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i];
                    if (directColumnIndex) {
                        header = Integer.toString(i);
                    }
                    if (ObservedPropertiesColumns.contains(header)) {
                        missingHeaders.remove(header);
                        obsPropIndex.add(i);
                    }
                }
            } else {
                // implying direct column index
                for (String i : ObservedPropertiesColumns) {
                    obsPropIndex.add(Integer.valueOf(i));
                }
            }

            if (obsPropIndex.size() != ObservedPropertiesColumns.size()) {
                throw new ConstellationStoreException("File headers is missing observed properties columns: " + StringUtilities.toCommaSeparatedValues(missingHeaders));
            }

            // sometimes in soe xlsx files, the last columns are empty, and so do not appears in the line
            // so we want to consider a line as imcomplete only if the last index we look for is missing.
            int maxIndex  = -1;
            for  (Integer i : obsPropIndex) {
                if (maxIndex < i) {
                    maxIndex = i;
                }
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
                } else if (headers != null && line.length < maxIndex + 1) {
                    LOGGER.finer("skipping imcomplete line " + lineNb + " (" +line.length + "/" + headers.length + ")");
                    continue;
                }
                
                String computed = "";
                boolean first = true;
                for(Integer i : obsPropIndex) {
                    final String nextCode = asString(line[i]);
                    if (nextCode == null || nextCode.isEmpty()) {
                        LOGGER.fine("Invalid measure ignore due to missing value at line " + lineNb + " column " + i);
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
                    LOGGER.warning("Invalid measure column value excluded (too long > 64 characters): " + computed);
                }
            }
            return storeCode;
            
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw new ConstellationStoreException("problem reading csv file", ex);
        }
    }
}
