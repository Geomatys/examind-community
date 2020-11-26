/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.provider.observationstore;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.v321.EnvelopeType;
import org.opengis.geometry.primitive.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationProviderUtils {
    
    private static final Logger LOGGER = Logging.getLogger("org.constellation.provider.observationstore");
    
    /**
     * Return true if the samplingPoint entry is strictly inside the specified envelope.
     *
     * @param sp A sampling point (2D) station.
     * @param e An envelope (2D).
     * @return True if the sampling point is strictly inside the specified envelope.
     */
    public static boolean samplingPointMatchEnvelope(final Point sp, final Envelope e) throws ConstellationStoreException {
        if (sp.getDirectPosition() != null) {
            org.opengis.geometry.Envelope reproj;
            final CoordinateReferenceSystem spCRS = sp.getCoordinateReferenceSystem();
            if (Utilities.equalsIgnoreMetadata(spCRS, e.getCoordinateReferenceSystem())) {
                reproj = e;
            } else {
                try {
                    reproj = new GeneralEnvelope(e);
                    reproj = Envelopes.transform(reproj, spCRS);
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            }

            final double stationX = sp.getDirectPosition().getOrdinate(0);
            final double stationY = sp.getDirectPosition().getOrdinate(1);
            final double minx     = reproj.getLowerCorner().getOrdinate(0);
            final double maxx     = reproj.getUpperCorner().getOrdinate(0);
            final double miny     = reproj.getLowerCorner().getOrdinate(1);
            final double maxy     = reproj.getUpperCorner().getOrdinate(1);

            // we look if the station if contained in the BBOX
            return stationX < maxx && stationX > minx && stationY < maxy && stationY > miny;
        }
        LOGGER.log(Level.WARNING, " the feature of interest does not have proper position");
        return false;
    }

    public static boolean BoundMatchEnvelope(final AbstractFeature sc, final Envelope e) {
         if (sc.getBoundedBy() != null &&
            sc.getBoundedBy().getEnvelope() != null &&
            sc.getBoundedBy().getEnvelope().getLowerCorner() != null &&
            sc.getBoundedBy().getEnvelope().getUpperCorner() != null &&
            sc.getBoundedBy().getEnvelope().getLowerCorner().getCoordinate().length > 1 &&
            sc.getBoundedBy().getEnvelope().getUpperCorner().getCoordinate().length > 1) {

            final double stationMinX  = sc.getBoundedBy().getEnvelope().getLowerCorner().getOrdinate(0);
            final double stationMaxX  = sc.getBoundedBy().getEnvelope().getUpperCorner().getOrdinate(0);
            final double stationMinY  = sc.getBoundedBy().getEnvelope().getLowerCorner().getOrdinate(1);
            final double stationMaxY  = sc.getBoundedBy().getEnvelope().getUpperCorner().getOrdinate(1);
            final double minx         = e.getLowerCorner().getOrdinate(0);
            final double maxx         = e.getUpperCorner().getOrdinate(0);
            final double miny         = e.getLowerCorner().getOrdinate(1);
            final double maxy         = e.getUpperCorner().getOrdinate(1);

            // we look if the station if contained in the BBOX
            if (stationMaxX < maxx && stationMinX > minx &&
                stationMaxY < maxy && stationMinY > miny) {
                return true;
            } else {
                LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", sc.getId());
            }
        } else {
            LOGGER.log(Level.WARNING, " the feature of interest (samplingCurve){0} does not have proper bounds", sc.getId());
        }
        return false;
    }
    
    public static String getIDFromObject(final Object obj) {
        if (obj != null) {
            final Method idGetter = ReflectionUtilities.getGetterFromName("id", obj.getClass());
            if (idGetter != null) {
                return (String) ReflectionUtilities.invokeMethod(obj, idGetter);
            }
        }
        return null;
    }
    
    public static Envelope getOrCastEnvelope(org.opengis.geometry.Envelope env) {
        if (env instanceof Envelope) {
            return (Envelope) env;
        } else {
            return new EnvelopeType(env);
        }
    }

}
