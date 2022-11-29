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
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.v321.EnvelopeType;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationProviderUtils {
    
    private static final Logger LOGGER = Logger.getLogger("org.constellation.provider.observationstore");
    
    /**
     * Return true if the samplingPoint entry is strictly inside the specified envelope.
     *
     * @param sp A sampling point (2D) station.
     * @param e An envelope (2D).
     * @return True if the sampling point is strictly inside the specified envelope.
     */
    public static boolean samplingPointMatchEnvelope(final Point sp, final Envelope e) throws ConstellationStoreException {
        if (sp.getCoordinate()!= null) {
            org.opengis.geometry.Envelope reproj;
            try {
                final CoordinateReferenceSystem spCRS = JTS.findCoordinateReferenceSystem(sp);
                if (Utilities.equalsIgnoreMetadata(spCRS, e.getCoordinateReferenceSystem())) {
                    reproj = e;
                } else {
                        reproj = new GeneralEnvelope(e);
                        reproj = Envelopes.transform(reproj, spCRS);
                }
            } catch (TransformException | FactoryException ex) {
                throw new ConstellationStoreException(ex);
            }

            final double stationX = sp.getCoordinate().getOrdinate(0);
            final double stationY = sp.getCoordinate().getOrdinate(1);
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

    public static boolean BoundMatchEnvelope(final org.locationtech.jts.geom.Envelope sc, final Envelope e) {
         if (sc != null) {

            final double stationMinX  = sc.getMinX();
            final double stationMaxX  = sc.getMaxX();
            final double stationMinY  = sc.getMinY();
            final double stationMaxY  = sc.getMaxY();
            final double minx         = e.getLowerCorner().getOrdinate(0);
            final double maxx         = e.getUpperCorner().getOrdinate(0);
            final double miny         = e.getLowerCorner().getOrdinate(1);
            final double maxy         = e.getUpperCorner().getOrdinate(1);

            // we look if the station if contained in the BBOX
            if (stationMaxX < maxx && stationMinX > minx &&
                stationMaxY < maxy && stationMinY > miny) {
                return true;
            } else {
                LOGGER.log(Level.FINER, " the feature of interest is not in the BBOX");
            }
        } else {
            LOGGER.log(Level.WARNING, " the feature of interest does not have proper bounds");
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
