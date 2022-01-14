/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.apache.sis.referencing.CRS;
import org.constellation.dto.CRSList;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.opengis.util.InternationalString;

/**
 * Utility method that regroup CRS listing methods used in UI.
 *
 * @author Benjamin Garcia (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public class CRSUtilities {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.utils");

    /**
     * A map on the form "crs name" + " - EPSG:" + "crs code" => "crs code"
     */
    private static SortedMap<String, String> wktCrsList;

    /**
     * A map on the form "crs code" => "crs code" + " - " + "crs description"
     */
    private static Map<String, String> crsList;

    /**
     * Load in cache two kind of CRS listing.
     */
    private static void computeCRSMap() {
        if (wktCrsList == null) {
            wktCrsList = new TreeMap<>();
            crsList    = new LinkedHashMap<>();
            try {
                final CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
                final Collection<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

                for (final String code : codes) {
                    try {
                        final InternationalString descIs = factory.getDescriptionText(code);
                        final String description = descIs != null ? " - " + descIs.toString() : "";

                        final String codeAndName = code + description;
                        crsList.put(code, codeAndName);
                        final IdentifiedObject obj = factory.createObject(code);
                        final String wkt = obj.getName().toString();
                        wktCrsList.put(wkt + " - EPSG:" + code, code);
                    } catch (Exception ex) {
                        //some objects can not be expressed in WKT, we skip them
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "not available in WKT : " + code);
                        }
                    }
                }
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, "Error while accesing EPSG factory", e);
            }
        }
    }

    /**
     * Return a map of crs descriptions (on the form"crs name" + " - EPSG:" + "crs code" => "crs code").
     *
     * @param start Start offset.
     * @param nbByPage Number of element by page.
     * @param filter If specified, apply a contains filter the result list.
     * @return a map of crs descriptions.
     */
    public static CRSList pagingAndFilterCode(final int start, final int nbByPage, final String filter) {
        computeCRSMap();
        SortedMap<String, String> selectedEPSGCode = new TreeMap<>();
        final CRSList coverageList = new CRSList();
        if (!"none".equalsIgnoreCase(filter)) {
            //filter epsg codes
            Predicate<String> myStringPredicate = new Predicate<String>() {
                @Override
                public boolean apply(final String s) {
                    String s1 = s.toLowerCase();
                    String filter1 = filter.toLowerCase();
                    return s1.contains(filter1) || s1.equalsIgnoreCase(filter1);
                }
            };

            selectedEPSGCode = Maps.filterKeys(wktCrsList, myStringPredicate);
            coverageList.setLength(selectedEPSGCode.size());
        } else {
            coverageList.setLength(wktCrsList.size());
        }

        //selectedEPSGCode is empty because they don't have a filter applied
        if (selectedEPSGCode.isEmpty()) {
            int epsgCode = wktCrsList.size();
            if (nbByPage > epsgCode) {
                coverageList.setSelectedEPSGCode(wktCrsList);
                return coverageList;
            } else {
                selectedEPSGCode = getSubCRSMap(start, nbByPage, wktCrsList);
            }
        } else {
            selectedEPSGCode = getSubCRSMap(start, nbByPage, selectedEPSGCode);
        }
        coverageList.setSelectedEPSGCode(selectedEPSGCode);
        return coverageList;
    }

    private static SortedMap<String, String> getSubCRSMap(final int start, final int nbByPage, SortedMap<String, String> sortedMap) {
        final Set<String> keys = sortedMap.keySet();
        String[] key = new String[keys.size()];
        key = keys.toArray(key);
        final String startKey = key[start];
        if((start + nbByPage) > key.length){
            sortedMap = sortedMap.tailMap(startKey);
        }else{
            final String endPageKey = key[start + nbByPage];
            sortedMap = sortedMap.subMap(startKey, endPageKey);
        }
        return sortedMap;
    }

    /**
     * Return the count of available crs.
     */
    public static int getEPSGCodesLength(){
        computeCRSMap();
        return wktCrsList.size();
    }

    /**
     * Return a map of crs descriptions (on the form "crs code" => "crs code" + " - " + "crs description").
     * The result can be filtered by specifying a filter parameter applied as a contains on the crs code or crs description.
     *
     * @param filter If specified, apply a contains filter the result list.
     *
     * @return A map of crs descriptions.
     */
    public static Map<String, String> getCRSCodes(String filter) {
        computeCRSMap();
        if (filter != null) {
            filter = filter.toLowerCase();
            final Map<String, String> results = new LinkedHashMap<>();
            for (Entry<String, String> entry : crsList.entrySet()) {
                if (entry.getValue().toLowerCase().contains(filter)) {
                    results.put(entry.getKey(), entry.getValue());
                }
            }
            return results;
        } else {
            return crsList;
        }
    }

}
