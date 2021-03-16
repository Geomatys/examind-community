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
package com.examind.process.sos.csvcoriolis;

import com.opencsv.CSVReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.netcdf.Field;
import org.geotoolkit.sos.netcdf.OMUtils;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.Quantity;
import org.geotoolkit.swe.xml.UomProperty;
import org.opengis.geometry.DirectPosition;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.Util;

import static org.geotoolkit.sos.netcdf.OMUtils.TIME_FIELD;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvCoriolisObservationStoreUtils {

    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.csvcoriolis");

    private static final NumberFormat FR_FORMAT = NumberFormat.getInstance(Locale.FRANCE);

    public static AbstractGeometry buildGeom(final List<DirectPosition> positions) {
        final AbstractGeometry sp;
        if (positions.isEmpty()) {
            return null;
        } else if (positions.size() > 1) {
            sp = GMLXmlFactory.buildLineString("3.2.1", null, "EPSG:4326", positions);
        } else {
            sp = GMLXmlFactory.buildPoint("3.2.1", null, "EPSG:4326", positions.get(0));
        }
        return sp;
    }

    public static SamplingFeature buildFOIByGeom(String foiID, final List<DirectPosition> positions, final Set<org.opengis.observation.sampling.SamplingFeature> existingFeatures) {
        final SamplingFeature sp;
        if (positions.isEmpty()) {
            sp = SOSXmlFactory.buildSamplingFeature("2.0.0", foiID, null, null, null);
        } else if (positions.size() > 1) {
            sp = OMUtils.buildSamplingCurve(foiID, positions);
        } else {
            sp = OMUtils.buildSamplingPoint(foiID, positions.get(0).getOrdinate(0),  positions.get(0).getOrdinate(1));
        }
        for (org.opengis.observation.sampling.SamplingFeature existingFeature : existingFeatures) {

            if (existingFeature instanceof SamplingFeature) {
                SamplingFeature ef = (SamplingFeature) existingFeature;
                if ((ef.getGeometry() == null && sp.getGeometry() == null) ||
                    (ef.getGeometry() != null && ef.getGeometry().equals(sp.getGeometry()))
                ) {
                    return ef;
                }
            }
        }
        return sp;
    }


    public static SamplingFeature buildFOIById(String foiID, final List<DirectPosition> positions, final Set<org.opengis.observation.sampling.SamplingFeature> existingFeatures) {
        final SamplingFeature sp;
        if (positions.isEmpty()) {
            sp = SOSXmlFactory.buildSamplingFeature("2.0.0", foiID, null, null, null);
        } else if (positions.size() > 1) {
            sp = OMUtils.buildSamplingCurve(foiID, positions);
        } else {
            sp = OMUtils.buildSamplingPoint(foiID, positions.get(0).getOrdinate(0),  positions.get(0).getOrdinate(1));
        }
        for (org.opengis.observation.sampling.SamplingFeature existingFeature : existingFeatures) {
            if (existingFeature instanceof SamplingFeature &&
               ((SamplingFeature)existingFeature).getId().equals(sp.getId())
            ) {
                return (SamplingFeature) existingFeature;
            }
        }
        return sp;
    }

    public static AbstractDataRecord getDataRecordProfile(final String version, final List<Field> phenomenons) {
        final List<AnyScalar> fields = new ArrayList<>();
        for (Field phenomenon : phenomenons) {
            final UomProperty uom = SOSXmlFactory.buildUomProperty(version, phenomenon.unit, null);
            final Quantity cat = SOSXmlFactory.buildQuantity(version, phenomenon.label, uom, null);
            fields.add(SOSXmlFactory.buildAnyScalar(version, null, phenomenon.label, cat));
        }
        return SOSXmlFactory.buildSimpleDatarecord(version, null, null, null, true, fields);
    }

    public static AbstractDataRecord getDataRecordTrajectory(final String version, final List<Field> phenomenons) {
        final List<AnyScalar> fields = new ArrayList<>();
        fields.add(TIME_FIELD.get(version));
        for (Field phenomenon : phenomenons) {
            final UomProperty uom = SOSXmlFactory.buildUomProperty(version, phenomenon.unit, null);
            final Quantity cat = SOSXmlFactory.buildQuantity(version, phenomenon.label, uom, null);
            fields.add(SOSXmlFactory.buildAnyScalar(version, null, phenomenon.label, cat));
        }
        return SOSXmlFactory.buildSimpleDatarecord(version, null, null, null, true, fields);
    }
    
    /**
     * hack method to find multiple coriolis provider on the same file.
     * 
     * this is dirty, i know
     */
    public static List<Integer> coriolisProviderForPath(String config, IProviderBusiness providerBusiness) {
        List<Integer> results = new ArrayList<>();
        int start = config.indexOf("<location>");
        int stop = config.indexOf("<location>");
        if (start != -1 && stop != -1) {
            String location = config.substring(start, stop);
            for (ProviderBrief pr : providerBusiness.getProviders()) {
                if (pr.getIdentifier().startsWith("observationCsvCoriolisFile") && 
                    pr.getConfig().contains(location)) {
                    results.add(pr.getId());
                }
            }
        }
        return results;
    }

    /**
     * Parse a string double with dot or comma separator.
     * @param s string value of a double.
     * @return : double.
     * @throws ParseException the parse method failed.
     */
    public static double parseDouble(String s) throws ParseException {
        if (s.contains(",")) {
            synchronized(FR_FORMAT) {
                Number number = FR_FORMAT.parse(s);
                return number.doubleValue();
            }
        } else {
            return Double.parseDouble(s);
        }
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
