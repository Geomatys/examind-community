/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
import java.util.List;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.storage.multires.TiledResource;
import org.geotoolkit.storage.multires.TileMatrixSet;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultPyramidData extends DefaultCoverageData implements PyramidData {


    public DefaultPyramidData(final GenericName name, final GridCoverageResource ref, final DataStore store) {
        super(name, ref, store);
    }

    @Override
    public String getSubType() throws ConstellationStoreException {
        return "pyramid";
    }

    @Override
    public Boolean isRendered() {
//        try {
//            ViewType packMode = ((MultiResolutionResource) origin).getPackMode();
//            if (ViewType.RENDERED.equals(packMode)) {
                  return Boolean.TRUE;
//            }
//        } catch (DataStoreException e) {
//            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
//        }
    }



    @Override
    public List<String> listPyramidCRSCode() throws ConstellationStoreException {
        List<String> results = new ArrayList<>();
        try {
            final TiledResource mr = (TiledResource) origin;
            for (TileMatrixSet mrm : mr.getTileMatrixSets()) {
                final CoordinateReferenceSystem crs = mrm.getCoordinateReferenceSystem();
                final Identifier epsgid = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
                final Identifier otherid = IdentifiedObjects.getIdentifier(crs, null);
                if (epsgid != null) {
                    results.add("EPSG:"+epsgid.getCode());
                } else {
                    results.add(IdentifiedObjects.toString(otherid));
                }
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return results;
    }
}
