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
package org.constellation.wfs.ws.rs;

import java.util.List;
import java.util.Map;
import org.apache.sis.storage.FeatureSet;
/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureSetWrapper {

    private final List<FeatureSet> featureSets;

    private final int nbMatched;

    private final Map<String, String> schemaLocations;

    private final String gmlVersion;

    private final String wfsVersion;

    private final boolean writeSingleFeature;

    public FeatureSetWrapper(final List<FeatureSet> featureSets, final Map<String, String> schemaLocations, final String gmlVersion,
            final String wfsVersion, final int nbMatched, boolean writeSingleFeature) {
        this.featureSets = featureSets;
        this.gmlVersion = gmlVersion;
        this.wfsVersion = wfsVersion;
        this.schemaLocations = schemaLocations;
        this.nbMatched = nbMatched;
        this.writeSingleFeature = writeSingleFeature;
    }

    /**
     * @return the featureSet
     */
    public List<FeatureSet> getFeatureSet() {
        return featureSets;
    }

    /**
     * @return the schemaLocations
     */
    public Map<String, String> getSchemaLocations() {
        return schemaLocations;
    }

    /**
     * @return the gmlVersion
     */
    public String getGmlVersion() {
        return gmlVersion;
    }

    /**
     * @return the wfsVersion
     */
    public String getWfsVersion() {
        return wfsVersion;
    }

    /**
     * @return the nbMatched
     */
    public int getNbMatched() {
        return nbMatched;
    }

    public boolean isWriteSingleFeature() {
        return writeSingleFeature;
    }

}
