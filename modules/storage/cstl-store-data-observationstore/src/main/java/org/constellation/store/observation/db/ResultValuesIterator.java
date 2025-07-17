/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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

import org.constellation.util.OMSQLDialect;
import org.constellation.store.observation.db.model.InsertDbField;
import jakarta.annotation.Nullable;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.util.OMSQLDialect.DERBY;
import org.constellation.util.Util;
import org.geotoolkit.observation.model.ComplexResult;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TEXT;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.geotoolkit.temporal.object.ISODateParser;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ResultValuesIterator {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    private final ComplexResult cr;
    private final TextEncoderProperties valueEncoding;
    private final ISODateParser dateParser = new ISODateParser();
    private final OMSQLDialect dialect;

    public ResultValuesIterator(ComplexResult cr, OMSQLDialect dialect) {
        this.cr = cr;
        this.dialect = dialect;
        if (cr.getValues() != null && !cr.getValues().isEmpty()) {
            valueEncoding = cr.getTextEncodingProperties();

        } else if (cr.getDataArray() != null) {
            valueEncoding = null;
        } else {
            throw new IllegalArgumentException("Either complex result values or data array must be filled.");
        }
    }

    public List<DataLine> getDataLines() {
        List<DataLine> results = new ArrayList<>();
        if (valueEncoding != null) {
            String[] blocks = cr.getValues().split(valueEncoding.getBlockSeparator());
            for (String block : blocks) {
                if (block.isEmpty()) {
                    continue;
                }
                results.add(new StringEncodedDataLine(block));
            }
        } else {
            for (Object block : cr.getDataArray()) {
                if (block instanceof List measures) {
                    results.add(new DataArrayDataLine(measures));
                } else {
                    throw new IllegalStateException("Input data array contains an unsupported block");
                }
            }
        }
        return results;
    }
    
    sealed interface DataLine {
        @Nullable Date getMainValue();
        List<Map.Entry<InsertDbField, String>> extractValues(List<InsertDbField> fields) throws DataStoreException;
    }


    private final class DataArrayDataLine implements DataLine {

        private final List<Object> measures;

        public DataArrayDataLine(List<Object> measures) {
            this.measures = measures;
        }

        @Override
        public List<Map.Entry<InsertDbField, String>> extractValues(List<InsertDbField> fields) throws DataStoreException {
            List<Map.Entry<InsertDbField, String>> results = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                final InsertDbField field = fields.get(i);
                final Object measure = measures.get(i);
                final String value = extractNextValue(measure, field);
                results.add(new AbstractMap.SimpleEntry<>(field, value));
            }
            return results;
        }

        /**
         * what about profile ???
         */
        @Override
        public Date getMainValue() {
            if (!measures.isEmpty()) {
                if (measures.get(0) instanceof Date mainValue) {
                    return mainValue;
                } else {
                    LOGGER.warning("Data array main value is not a Date");
                }
            }
            return null;
        }

        private String extractNextValue(Object measure, InsertDbField field) throws DataStoreException {
            String value = null;
            switch (field.dataType) {
                case TIME -> {
                    //format time
                    if (measure != null) {
                        if (measure instanceof Date d) {
                            final long millis = d.getTime();
                            value = "'" + new Timestamp(millis).toString() + "'";
                        } else if (measure instanceof Long millis) {
                            value = "'" + new Timestamp(millis).toString() + "'";
                        } else {
                            throw new DataStoreException("expecting timestamp for field " + field.name+ " value : " +measure);
                        }
                    }
                }
                case TEXT -> {
                    if (measure != null) {
                        if (measure instanceof String s) {
                            if (Util.containsForbiddenCharacter(s)) {
                                throw new DataStoreException("Invalid value inserted");
                            }
                            value = "'" + s + "'";
                        } else {
                            throw new DataStoreException("expecting timestamp for field " + field.name + " value : " +measure);
                        }
                    }
                }
                case BOOLEAN -> {
                   if (measure != null) {
                        if (measure instanceof Boolean b) {
                            if (dialect.equals(DERBY)) {
                                value = b ? "1" : "0";
                            } else {
                                value = Boolean.toString(b);
                            }
                        } else {
                            throw new DataStoreException("expecting boolean for field " + field.name + " value : " + measure);
                        }
                   }
                }
                case QUANTITY -> {
                    if (measure != null) {
                        if (measure instanceof Double d) {
                            d = (Double) field.convertValue(d);
                            value = Double.toString(d);
                        } else {
                            throw new DataStoreException("expecting double for field " + field.name + " value : " + measure);
                        }
                    }
                }
            }
            return value;
        }
    }
    
    private final class StringEncodedDataLine implements DataLine {

        private String block;

        public StringEncodedDataLine(String block) {
            this.block = block;
        }

        @Override
        public List<Map.Entry<InsertDbField, String>> extractValues(List<InsertDbField> fields) throws DataStoreException {
            List<Map.Entry<InsertDbField, String>> results = new ArrayList<>();
            if (valueEncoding != null) {
                for (int i = 0; i < fields.size(); i++) {
                    final InsertDbField field = fields.get(i);
                    final boolean lastTokenInBlock = (i == fields.size() - 1);
                    final String[] nextToken = extractNextValue(block, field, lastTokenInBlock, valueEncoding);
                    final String value = nextToken[0];
                    block = nextToken[1];

                    results.add(new AbstractMap.SimpleEntry<>(field, value));
                }
            }
            return results;
        }

        /**
         * what about profile ???
         */
        @Override
        public Date getMainValue() {
            int tokenPos = block.indexOf(valueEncoding.getTokenSeparator());
            final String mainValue;
            if (tokenPos > 0) {
                mainValue = block.substring(0, tokenPos);
            } else if (tokenPos == 0) {
                LOGGER.warning("empty main value in data block");
                return null;
            } else {
                mainValue = block;
            }
            Date d = null;
            try {
                d = dateParser.parseToDate(mainValue);
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.FINER, "unable to parse the value: {0}", mainValue);
            }
            return d;
        }

        /**
         * Extract the next value for a field in a String block.
         *
         * @param block A line correspounding to a single mesure.
         * @param field the current field to extract.
         * @param lastTokenInBlock if set t true, it means that the block
         * contain the entire last value.
         * @param encoding text encoding infos.
         *
         * @return A string array of a fixed value of 2. The first String is the
         * value (quoted or not depeding on field type). The second String id
         * the remaining block to parse.
         * @throws DataStoreException Ifthe block is malformed, if a timestamp
         * has a bad format, or if the text value contains forbidden character.
         */
        private String[] extractNextValue(String block, InsertDbField field, boolean lastTokenInBlock, final TextEncoderProperties encoding) throws DataStoreException {
            String value;
            if (lastTokenInBlock) {
                value = block;
            } else {
                int separator = block.indexOf(encoding.getTokenSeparator());
                if (separator != -1) {
                    value = block.substring(0, separator);
                    block = block.substring(separator + 1);
                } else {
                    throw new DataStoreException("Bad encoding for datablock, unable to find the token separator:" + encoding.getTokenSeparator() + "in the block.");
                }
            }

            switch (field.dataType) {
                case TIME -> {
                    //format time
                    if (value != null && !(value = value.trim()).isEmpty()) {
                        try {
                            final long millis = dateParser.parseToMillis(value);
                            value = "'" + new Timestamp(millis).toString() + "'";
                        } catch (IllegalArgumentException ex) {
                            throw new DataStoreException("Bad format of timestamp for:" + value + " for field " + field.name);
                        }
                    }
                }
                case TEXT -> {
                    if (Util.containsForbiddenCharacter(value)) {
                        throw new DataStoreException("Invalid value inserted");
                    }
                    value = "'" + value + "'";
                }
                case BOOLEAN -> {
                    boolean parsed = Boolean.parseBoolean(value);
                    if (dialect.equals(DERBY)) {
                        value = parsed ? "1" : "0";
                    } else {
                        value = Boolean.toString(parsed);
                    }
                }
                case QUANTITY -> {
                    if (value != null && !(value = value.trim()).isEmpty()) {
                        try {
                            Double d = Double.valueOf(value);
                            d = (Double) field.convertValue(d);
                            value = Double.toString(d);
                        } catch (NumberFormatException ex) {
                            throw new DataStoreException("Unable to parse double:" + value + " for field " + field.name);
                        }
                    }
                }
            }
            return new String[]{value, block};
        }
    }
}
