/*
 *    Examind Community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import static org.constellation.provider.DataProviders.LOGGER;
import static org.constellation.provider.DataProviders.getProviderData;
import org.constellation.util.CRSUtilities;
import org.geotoolkit.storage.multires.TileMatrices;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ScaleUtilities {

    /**
     * Returns scales array for an envelope and a tile size.(for wmts scales)
     *
     * @param globalEnv Global envelope.
     * @param tileSize Tile size.
     * @param nbLevel Number of level to compute
     *
     * @return scales array. (for wmts scales)
     */
    public static double[] computeScales(Envelope globalEnv, int tileSize, int nbLevel) {
        final double geospanX = globalEnv.getSpan(0);
        final double[] scales = new double[nbLevel];
        scales[0] = geospanX / tileSize;
        for (int i = 1; i < scales.length; i++) {
            scales[i] = scales[i - 1] / 2.0;
        }
        return scales;
    }

    /**
     * Returns scales array for a data.(for wmts scales)
     *
     * @param providerId Identifier of the provider
     * @param dataNamespace Data namespace.
     * @param dataName Data name.
     * @param crs coordinate reference system identifier.
     * @param nbLevel Number of level to compute (used only for non-coverage data)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if the crs can be decoded or if data retrieval failed.
     */
    public static Double[] computeScales(final int providerId, final String dataNamespace, final String dataName, final String crs, int nbLevel) throws ConstellationException {
        CoordinateReferenceSystem c = CRSUtilities.verifyCrs(crs, false).orElse(null);
        return computeScales(providerId, dataNamespace, dataName, c, nbLevel);
    }

    /**
     * Returns scales array for a data.(for wmts scales)
     *
     * @param providerId Identifier of the provider
     * @param dataNamespace Data namespace.
     * @param dataName Data name.
     * @param crs coordinate reference system.
     * @param nbLevel Number of level to compute (used only for non-coverage data)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if data retrieval, envelope retrieval/transformation, gridGeometry extraction failed.
     */
    private static Double[] computeScales(final int providerId, final String dataNamespace, final String dataName, final CoordinateReferenceSystem crs, int nbLevel) throws ConstellationException {
        //get data
        final Data inData = getProviderData(providerId, dataNamespace, dataName);
        if (inData==null) {
            String nmsp = dataNamespace != null ? "{" + dataNamespace + "} " : "";
            throw new TargetNotFoundException("Data " + nmsp + dataName + " does not exist in provider "+providerId);
        }
        Envelope dataEnv;
        try {
            //use data crs
            dataEnv = inData.getEnvelope();
        } catch (ConstellationStoreException ex) {
            throw new ConstellationException("Failed to extract envelope for data "+dataName, ex);
        }
        final Object origin = inData.getOrigin();
        final Double[] scales;
        final Envelope env;
        try {
            if (crs == null) {
                env = dataEnv;
            } else if (dataEnv.getCoordinateReferenceSystem() == null) {
                throw new IllegalStateException("Cannot express data envelope in given CRS: input data envelope");
            } else {
                env = Envelopes.transform(dataEnv, crs);
            }
        } catch (Exception ex) {
            throw new ConstellationException("Failed to transform envelope to input CRS", ex);
        }

        if (origin instanceof GridCoverageResource) {
            //calculate pyramid scale levels
            final GridCoverageResource inRef = (GridCoverageResource) origin;
            final GridGeometry gg;
            try {
                gg = inRef.getGridGeometry();
            } catch (DataStoreException ex) {
                throw new ConstellationException("Failed to extract grid geometry for data "+dataName+". ",ex);
            }
            final double geospanX = env.getSpan(0);
            final double baseScale = geospanX / gg.getExtent().getSize(0);

            //detect common CRS : 3857 PseudoMercator
            final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
            if (id != null && "3857".equals(id.getCode())) {
                try {
                    final int lod = TileMatrices.computePseudoMercatorDepthForResolution(baseScale);
                    final double[] ds = TileMatrices.createMercatorTemplate(lod).getScales();
                    scales = new Double[ds.length];
                    for (int i = 0; i < scales.length; i++) scales[i] = ds[i];
                    //rever order, as expected by callers
                    Collections.reverse(Arrays.asList(scales));
                } catch (FactoryException ex) {
                    throw new ConstellationStoreException(ex);
                }
            // other CRS
            } else {
                final int tileSize = 256;
                double scale = geospanX / tileSize;
                final GeneralDirectPosition ul = new GeneralDirectPosition(env.getCoordinateReferenceSystem());
                ul.setOrdinate(0, env.getMinimum(0));
                ul.setOrdinate(1, env.getMaximum(1));
                final List<Double> scalesList = new ArrayList<>();
                while (true) {
                    if (scale <= baseScale) {
                        //fit to exact match to preserve base quality.
                        scale = baseScale;
                    }
                    scalesList.add(scale);
                    if (scale <= baseScale) {
                        break;
                    }
                    scale = scale / 2;
                }
                scales = new Double[scalesList.size()];
                for (int i = 0; i < scales.length; i++) {
                    scales[i] = scalesList.get(i);
                }
            }
        } else {
            //featurecollection or anything else, scales can not be defined accurately.
            //vectors have virtually an unlimited resolution
            final double geospanX = env.getSpan(0);
            final int tileSize = 256;
            scales = new Double[nbLevel];
            scales[0] = geospanX / tileSize;
            for(int i=1;i<scales.length;i++){
                scales[i] = scales[i-1] / 2.0;
            }
        }
        return scales;
    }

    /**
     * Returns scales array for a data list.(for wmts scales)
     *
     * @param briefs List of data.
     * @param crs coordinate reference system identifier.
     * @param nbLevel Number of level to compute (used only if a non-coverage data is present in the list)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if the crs can be decoded or no scales can be calculated from any data.
     */
    public static double[] getBestScales(List<? extends org.constellation.dto.Data> briefs, String crs, int nbLevel) throws ConstellationException {
        CoordinateReferenceSystem c = CRSUtilities.verifyCrs(crs, false).orElse(null);
        return getBestScales(briefs, c, nbLevel);
    }

    /**
     * Returns scales array for a data list.(for wmts scales)
     *
     * @param briefs  List of data.
     * @param crs coordinate reference.
     * @param nbLevel Number of level to compute (used only if a non-coverage data is present in the list)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if no scales can be calculated from any data.
     */
    public static double[] getBestScales(List<? extends org.constellation.dto.Data> briefs, CoordinateReferenceSystem crs, int nbLevel) throws ConstellationException {
        final List<Double> mergedScales = new LinkedList<>();
        for (final org.constellation.dto.Data db : briefs){
            final Double[] scales;
            try {
                scales = computeScales(db.getProviderId(), db.getNamespace(), db.getName(), crs, nbLevel);
            } catch(Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                continue;
            }
            if (mergedScales.isEmpty()) {
                mergedScales.addAll(Arrays.asList(scales));
            } else {
                Double max = Math.max(mergedScales.get(0),scales[0]);
                Double min = Math.min(mergedScales.get(mergedScales.size()-1),scales[scales.length-1]);
                final List<Double> scalesList = new ArrayList<>();
                Double scale = max;
                while (true) {
                    if (scale <= min) {
                        scale = min;
                    }
                    scalesList.add(scale);
                    if (scale <= min) {
                        break;
                    }
                    scale = scale / 2;
                }
                mergedScales.clear();
                mergedScales.addAll(scalesList);
            }
        }
        if (mergedScales.isEmpty()) {
            throw new ConstellationException("No scale found for supplied datas");
        }
        double[] results = new double[mergedScales.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = mergedScales.get(i);
        }
        return results;
    }
}
