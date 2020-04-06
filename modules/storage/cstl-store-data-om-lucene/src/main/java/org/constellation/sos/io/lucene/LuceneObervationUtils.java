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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.ObservationStoreException;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import org.geotoolkit.temporal.object.ISODateParser;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneObervationUtils {

    /**
     * return a SQL formatted timestamp
     *
     * @param time a GML time position object.
     * @throws org.apache.sis.storage.DataStoreException
     */
    public static String getLuceneTimeValue(final Date time) throws DataStoreException {
        if (time != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
            String value = df.format(time);

            // we delete the data after the second TODO remove
            if (value.indexOf('.') != -1) {
                value = value.substring(0, value.indexOf('.'));
            }
            try {
                // verify the syntax of the timestamp
                //here t is not used but it allow to verify the syntax of the timestamp
                final ISODateParser parser = new ISODateParser();
                final Date d = parser.parseToDate(value);

            } catch (IllegalArgumentException e) {
                throw new ObservationStoreException("Unable to parse the value: " + value + '\n' +
                                              "Bad format of timestamp:\n" + e.getMessage(),
                                              INVALID_PARAMETER_VALUE, "eventTime");

            }
            value = value.replace(" ", "");
            value = value.replace("-", "");
            value = value.replace(":", "");
            value = value.replace("T", "");
            return value;
        } else {
            throw new ObservationStoreException("bad format of time, Timeposition mustn't be null",
                    MISSING_PARAMETER_VALUE, "eventTime");
        }
    }

    /**
     * Transform a Lucene Date syntax string into a yyyy-MM-dd hh:mm:ss Date format String.
     *
     * @param luceneTimeValue A String on Lucene date format
     * @return A String on yyy-MM-dd hh:mm:ss Date format
     */
    public static String unLuceneTimeValue(String luceneTimeValue) {
        final String year     = luceneTimeValue.substring(0, 4);
        luceneTimeValue = luceneTimeValue.substring(4);
        final String month    = luceneTimeValue.substring(0, 2);
        luceneTimeValue = luceneTimeValue.substring(2);
        final String day      = luceneTimeValue.substring(0, 2);
        luceneTimeValue = luceneTimeValue.substring(2);
        final String hour     = luceneTimeValue.substring(0, 2);
        luceneTimeValue = luceneTimeValue.substring(2);
        final String min      = luceneTimeValue.substring(0, 2);
        luceneTimeValue = luceneTimeValue.substring(2);
        final String sec      = luceneTimeValue.substring(0, 2);

        return year + '-' + month + '-' + day + ' ' + hour + ':' + min + ':' + sec;
    }
}
