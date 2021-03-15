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
package org.constellation.store.observation.db;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.opengis.observation.CompositePhenomenon;
import org.opengis.observation.Phenomenon;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2Utils {

    public static Date dateFromTS(Timestamp t) {
        if (t != null) {
            return new Date(t.getTime());
        }
        return null;
    }

    public static String getVersionFromHints(Map<String, String> hints) {
        if (hints != null && hints.containsKey("version")) {
            return hints.get("version");
        }
        return "2.0.0";
    }

    public static boolean getBooleanHint(Map<String, String> hints, String key, boolean defaultValue) {
        if (hints != null && hints.containsKey(key)) {
            return Boolean.parseBoolean(hints.get(key));
        }
        return defaultValue;
    }

    public static Integer getIntegerHint(Map<String, String> hints, String key) {
        if (hints != null && hints.containsKey(key)) {
            return Integer.parseInt(hints.get(key));
        }
        return null;
    }

    public static Long getLongHint(Map<String, String> hints, String key) {
        if (hints != null && hints.containsKey(key)) {
            return Long.parseLong(hints.get(key));
        }
        return null;
    }

    public static List<OM2BaseReader.Field> reOrderFields(List<OM2BaseReader.Field> procedureFields, List<OM2BaseReader.Field> subset) {
        List<OM2BaseReader.Field> result = new ArrayList();
        for (OM2BaseReader.Field pField : procedureFields) {
            if (subset.contains(pField)) {
                result.add(pField);
            }
        }
        return result;
    }

    /**
     * Return true if a composite phenomenon is a subset of another composite.
     * meaning that every of its component is present in the second.
     *
     * @param composite
     * @param fullComposite
     * @return
     */
    public static boolean isACompositeSubSet(CompositePhenomenon composite, CompositePhenomenon fullComposite) {
        for (Phenomenon component : composite.getComponent()) {
            if (!fullComposite.getComponent().contains(component)) {
                return false;
            }
        }
        return true;
    }

    public static CompositePhenomenon getOverlappingComposite(List<CompositePhenomenon> composites) throws DataStoreException {
        a:for (CompositePhenomenon composite : composites) {
            String compoId = getId(composite);
            for (CompositePhenomenon sub : composites) {
                if (!getId(sub).equals(compoId) && !isACompositeSubSet(sub, composite)) {
                    continue a;
                }
            }
            return composite;
        }
        throw new DataStoreException("No composite has all other as subset");
    }

    public static String getId(Phenomenon phen) {
        if (phen instanceof org.geotoolkit.swe.xml.Phenomenon) {
            return ((org.geotoolkit.swe.xml.Phenomenon)phen).getId();
        }
        throw new IllegalArgumentException("Unable to get an id from the phenomenon");
    }

    public static String getName(Phenomenon phen) {
        if (phen instanceof org.geotoolkit.swe.xml.Phenomenon) {
            return ((org.geotoolkit.swe.xml.Phenomenon)phen).getName().getCode();
        }
        throw new IllegalArgumentException("Unable to get an id from the phenomenon");
    }

    public static boolean hasComponent(Phenomenon phen, CompositePhenomenon composite) {
        String phenId = getId(phen);
        for (Phenomenon component : composite.getComponent()) {
            if (phenId.equals(getId(component))) {
                return true;
            }
        }
        return false;
    }
}
