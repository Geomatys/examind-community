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

import java.util.List;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.ArraysExt;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import static org.constellation.provider.DataProviders.LOGGER;
import static org.constellation.provider.DataProviders.getProviderData;
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
    public static double[] generateScales(Envelope globalEnv, int tileSize, int nbLevel) {
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
     * @param crs coordinate reference system.
     * @param tileSize Tile size.
     * @param nbLevel Number of level to compute (used only for non-coverage data)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if data retrieval, envelope retrieval/transformation, gridGeometry extraction failed.
     */
    private static double[] computeScales(final int providerId, final String dataNamespace, final String dataName, final CoordinateReferenceSystem crs, int tileSize, int nbLevel) throws ConstellationException {
        //get data
        final Data inData = getProviderData(providerId, dataNamespace, dataName);
        if (inData == null) {
            String nmsp = dataNamespace != null ? "{" + dataNamespace + "} " : "";
            throw new TargetNotFoundException("Data " + nmsp + dataName + " does not exist in provider "+providerId);
        }
        Envelope env;
        try {
            //use data crs
            env = inData.getEnvelope(crs);
        } catch (ConstellationStoreException ex) {
            throw new ConstellationException("Failed to extract envelope for data " + dataName, ex);
        }

        //calculate pyramid scale levels
        final double[] scales;
        final Object origin = inData.getOrigin();
        if (origin instanceof GridCoverageResource gcr) {

            final GridGeometry gg;
            try {
                gg = gcr.getGridGeometry();
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
                    final double[] ds = TileMatrices.getScales(TileMatrices.createMercatorTemplate(lod));

                    //rever order, as expected by callers
                    scales = new double[ds.length];
                    for (int i = 0, j = ds.length - 1; i < scales.length; i++, j--) scales[i] = ds[j];

                } catch (FactoryException ex) {
                    throw new ConstellationStoreException(ex);
                }
            // other CRS
            } else {
                double maxScale = geospanX / tileSize;
                scales = generateScales(maxScale, baseScale);
            }
        } else {
            //vectors or anything else, scales can not be defined accurately.
            scales = generateScales(env, tileSize, nbLevel);
        }
        return scales;
    }

    /**
     * Returns scales array for a data list.(for wmts scales)
     *
     * @param briefs  List of data.
     * @param crs coordinate reference.
     * @param tileSize Tile size.
     * @param nbLevel Number of level to compute (used only if a non-coverage data is present in the list)
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException if no scales can be calculated from any data.
     */
    public static double[] getBestScales(List<? extends org.constellation.dto.Data> briefs, CoordinateReferenceSystem crs, int tileSize, int nbLevel) throws ConstellationException {
        double[] mergedScales;
        if (briefs.isEmpty()) {
            throw new ConstellationException("Supplied datas can not be empty");
        } else if (briefs.size() == 1) {
            org.constellation.dto.Data db = briefs.get(0);
            return computeScales(db.getProviderId(), db.getNamespace(), db.getName(), crs, tileSize, nbLevel);
        } else {
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (final org.constellation.dto.Data db : briefs) {
                try {
                    final double[] scales = computeScales(db.getProviderId(), db.getNamespace(), db.getName(), crs, tileSize, nbLevel);
                    max = Math.max(max, scales[0]);
                    min = Math.min(min, scales[scales.length-1]);
                } catch(Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
            // generate scales from min / max
            mergedScales = generateScales(max, min);
        }
        if (mergedScales.length == 0) {
            throw new ConstellationException("No scale found for supplied datas");
        }
        return mergedScales;
    }

    private static double[] generateScales(double max, double min) {
        double[] scalesList = new double[0];
        double scale = max;
        while (true) {
            if (scale <= min) {
                scale = min;
            }
            scalesList = ArraysExt.resize(scalesList, scalesList.length + 1);
            scalesList[scalesList.length -1] = scale;
            if (scale <= min) {
                break;
            }
            scale = scale / 2;
        }
        return scalesList;
    }
}
