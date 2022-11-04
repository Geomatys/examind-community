/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.provider.sensorstore;

import org.geotoolkit.sml.xml.AbstractProcess;
import org.geotoolkit.sml.xml.AbstractSensorML;

/**
 * Temporary class until methods are move to org.geotoolkit.sml.xml.SensorMLUtilities
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorMLUtilities {

    /**
     * Return the gml name of the first process member in the specified sensorML.
     *
     * @param sensor A sensorML object.
     * @return the code name or {@code null}
     */
    public static String getSmlName(final AbstractSensorML sensor) {
        if (sensor != null && !sensor.getMember().isEmpty()) {
            final AbstractProcess process = sensor.getMember().get(0).getRealProcess();
            if (process.getName() != null) {
                return process.getName().getCode();
            }
        }
        return null;
    }

    /**
     * Return the gml description of the first process member in the specified sensorML.
     *
     * @param sensor A sensorML object.
     * @return the description or {@code null}
     */
    public static String getSmlDescription(final AbstractSensorML sensor) {
        if (sensor != null && !sensor.getMember().isEmpty()) {
            final AbstractProcess process = sensor.getMember().get(0).getRealProcess();
            return process.getDescription();
        }
        return null;
    }
}
