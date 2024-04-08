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
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.constellation.exception.ConstellationException;
import org.opengis.util.InternationalString;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;

/**
 * Utility method that regroup CRS listing methods used in UI.
 *
 * @author Benjamin Garcia (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public class CRSUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.utils");

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
                final CRSAuthorityFactory factory = CRS.getAuthorityFactory(null);//list all crs, not just EPSG
                final Collection<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

                for (final String code : codes) {
                    try {
                        final InternationalString descIs = factory.getDescriptionText(CoordinateReferenceSystem.class, code).orElse(null);
                        final String description = descIs != null ? " - " + descIs.toString() : "";

                        final String codeAndName = code + description;
                        crsList.put(code, codeAndName);
                        final IdentifiedObject obj = factory.createCoordinateReferenceSystem(code);
                        final String wkt = obj.getName().toString();
                        if (code.startsWith("EPSG")) {
                            wktCrsList.put(wkt + " - EPSG:" + code, code);
                        }
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
            for (Entry<String, String> entry : wktCrsList.entrySet()) {
                String s1      = entry.getKey();
                String filter1 = filter.toLowerCase();
                if (s1.contains(filter1) || s1.equalsIgnoreCase(filter1)) {
                    selectedEPSGCode.put(entry.getKey(), entry.getValue());
                }
            }
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

    /**
     * Reproject an envelope in the specified CRS.
     * In some case the envelope resulting from the reprojection can contains some infinite coordinates.
     * If this happen, this method will return an intersection between the domain of validity of the target crs and the reprojected envelope,
     * in order to remove the infinite coordinates.
     *
     * @param src An evelope to reproject.
     * @param targetCRS The target coordinate reference system.
     *
     * @return A reprojected envelope.
     * @throws TransformException if the transformation failed or if the projected envelope contains infinite coordinates
     * and the domain of validity of the target crs {@code null}.
     */
    public static GeneralEnvelope reprojectWithNoInfinity(Envelope src, CoordinateReferenceSystem targetCRS) throws TransformException {
        final GeneralEnvelope result = GeneralEnvelope.castOrCopy(Envelopes.transform(src, targetCRS));
        boolean hasInfiniteCoordinate = false;
        for (int i = 0; i < result.getDimension(); i++) {
            if (Double.isInfinite(result.getMinimum(i)) || Double.isInfinite(result.getMaximum(i))) {
                hasInfiniteCoordinate = true;
                break;
            }
        }
        if (hasInfiniteCoordinate) {
            final Envelope crsEnv = CRS.getDomainOfValidity(targetCRS);
            if (crsEnv == null) {
                throw new TransformException("The projected envelope contains infinite coordinates and the domain of validity of the target crs is null");
            }
            result.intersect(crsEnv);
        }
        return result;
    }

    /**
     * Return a CRS for a specified code.
     *
     * @param crs a CRS identifier.
     * @param forceConvention If set to {@code true} The CRS is returned with a forced convention of AxesConvention.RIGHT_HANDED.
     *
     * @return A CoordinateReferenceSystem or {@code Optional.empty()} if the input parameter "crs" is null or empty.
     *
     * @throws ConstellationException If the CRS code is invalid.
     */
    public static Optional<CoordinateReferenceSystem>  verifyCrs(String crs, boolean forceConvention) throws ConstellationException {
        if (crs == null || (crs = crs.trim()).isEmpty()) return Optional.empty();
        try {
            final CoordinateReferenceSystem decodedCrs = CRS.forCode(crs);
            if (!forceConvention) return Optional.of(decodedCrs);
            return Optional.of(AbstractCRS.castOrCopy(CRS.forCode(crs)).forConvention(AxesConvention.RIGHT_HANDED));
        } catch (FactoryException ex) {
            throw new ConstellationException("Invalid CRS code : " + crs);
        }
    }
}
