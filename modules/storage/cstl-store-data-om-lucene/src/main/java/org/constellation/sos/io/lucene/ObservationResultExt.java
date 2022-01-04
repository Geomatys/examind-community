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

package org.constellation.sos.io.lucene;

import java.sql.Timestamp;
import java.util.Objects;
import org.geotoolkit.observation.ObservationResult;

/**
 * Temporary class waiting for geotk class to add hashcode/equals methods.
 * 
 * @author guilhem Legal (Geomatys)
 */
public class ObservationResultExt extends ObservationResult {

    public ObservationResultExt(String resultID, Timestamp beginTime, Timestamp endTime) {
        super(resultID, beginTime, endTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            ObservationResultExt that = (ObservationResultExt) obj;
            return Objects.equals(this.beginTime, that.beginTime) &&
                   Objects.equals(this.endTime,   that.endTime) &&
                   Objects.equals(this.resultID,  that.resultID);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 79;
        hash = 79 * hash + this.beginTime.hashCode();
        hash = 79 * hash + this.endTime.hashCode();
        hash = 79 * hash + this.resultID.hashCode();
        return hash;
    }
}
