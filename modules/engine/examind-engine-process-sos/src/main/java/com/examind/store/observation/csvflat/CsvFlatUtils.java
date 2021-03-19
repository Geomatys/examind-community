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

import com.opencsv.CSVReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.Util;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvFlatUtils {
    private static final Logger LOGGER = Logging.getLogger("com.examind.store.observation.csvflat");

    /**
     * hack method to find multiple csvFLat provider on the same file.
     * 
     * this is dirty, i know
     */
    public static List<Integer> csvFlatProviderForPath(String config, IProviderBusiness providerBusiness) {
        List<Integer> results = new ArrayList<>();
        int start = config.indexOf("<location>");
        int stop = config.indexOf("<location>");
        if (start != -1 && stop != -1) {
            String location = config.substring(start, stop);
            for (ProviderBrief pr : providerBusiness.getProviders()) {
                if (pr.getIdentifier().startsWith("observationCsvFlatFile") &&
                    pr.getConfig().contains(location)) {
                    results.add(pr.getId());
                }
            }
        }
        return results;
    }

    public static Set<String> extractCodes(Path dataFile, Collection<String> measureCodeColumns, char separator) throws ConstellationStoreException {
        try (final CSVReader reader = new CSVReader(Files.newBufferedReader(dataFile), separator)) {

            final Iterator<String[]> it = reader.iterator();

            // at least one line is expected to contain headers information
            if (it.hasNext()) {

                // read headers
                final String[] headers = it.next();
                List<Integer> measureCodeIndex = new ArrayList<>();

                // find measureCodeIndex
                for (int i = 0; i < headers.length; i++) {
                    final String header = headers[i];

                    if (measureCodeColumns.contains(header)) {
                        measureCodeIndex.add(i);
                    }
                }

                if (measureCodeIndex.size() != measureCodeColumns.size()) {
                    throw new ConstellationStoreException("csv headers does not contains All the Measure Code parameter.");
                }

                final Set<String> storeCode = new HashSet<>();
                // extract all codes
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
                    if (!Util.containsForbiddenCharacter(computed) && computed.indexOf('.') == -1) {
                        storeCode.add(computed);
                    } else {
                        LOGGER.warning("Invalid measure column value excluded: " + computed);
                    }
                }
                return storeCode;
            }
            throw new ConstellationStoreException("csv headers not found");
        } catch (IOException ex) {
            throw new ConstellationStoreException("problem reading csv file", ex);
        }
    }
}
