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
package com.examind.process.sos.csv;

import java.util.List;
import java.util.Set;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.netcdf.OMUtils;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.opengis.geometry.DirectPosition;
/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvObservationStoreUtils {

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
}
