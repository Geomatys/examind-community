/*
 *    Examind Community An open source and standard compliant SDI
 *    https://community.examind.com/
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.observation.model.ResultMode.COUNT;
import static org.geotoolkit.observation.model.ResultMode.CSV;
import static org.geotoolkit.observation.model.ResultMode.DATA_ARRAY;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.geotoolkit.observation.result.ResultBuilder;

/**
 * Temporay class until {@link org.geotoolkit.observation.result.ResultBuilder} will allow to remove empty lines
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TemporaryResultBuilder extends ResultBuilder {

    private final ResultMode mode;
    private final boolean csvHack;
    private boolean emptyLine;

    private StringBuilder values;
    private StringBuilder currentLine;
    private final TextEncoderProperties encoding;

    private List<Object> dataArray;
    private List<Object> currentArrayLine;

    private int count = 0;

    public TemporaryResultBuilder(ResultMode mode, final TextEncoderProperties encoding, boolean csvHack) {
        super(mode, encoding, csvHack);
        this.mode = mode;
        this.csvHack = csvHack;
        this.encoding = encoding;
        switch (mode) {
            case DATA_ARRAY -> dataArray = new ArrayList<>();
            case CSV        -> values = new StringBuilder();
        }
    }

    /**
     * Reset all values.
     */
    public void clear() {
        switch (mode) {
            case DATA_ARRAY -> dataArray = new ArrayList<>();
            case CSV        -> values = new StringBuilder();
        }
    }

    /**
     * Start a new measure line.
     */
    public void newBlock() {
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine = new ArrayList<>();
            case CSV        -> currentLine = new StringBuilder();
        }
        this.emptyLine = true;
    }

    /**
     * Append a date to the current data line.
     *
     * @param value Date value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendTime(Date value, boolean measureField) {
        if (value != null && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                String strValue = "";
                if (value!= null) {
                    DateFormat df;
                    df = csvHack ? format : format2;
                    synchronized (df) {
                        strValue = df.format(value);
                    }
                }
                currentLine.append(strValue).append(encoding.getTokenSeparator());
            }
        }
    }

    /**
     * Append a date in millisecond to the current data line.
     *
     * @param value Date value in millisecond.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendTime(Long value, boolean measureField) {
        Date d = null;
        if (value != null) {
            d = new Date(value);
        }
        appendTime(d, measureField);
    }

    /**
     * Append a number value to the current data line.
     *
     * @param value Number value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendNumber(Number value, boolean measureField) {
        if (value != null && measureField) {
            emptyLine = false;
        }
        if (value instanceof Double d) {
            appendDouble(d, measureField);
        } else if (value instanceof Float f) {
            appendFloat(f, measureField);
        } else if (value instanceof Integer i) {
            appendInteger(i, measureField);
        } else if (value instanceof Long l) {
            appendLong(l, measureField);
        } else if (value == null) {
            switch (getMode()) {
                case DATA_ARRAY -> currentArrayLine.add(null);
                case CSV        -> currentLine.append(encoding.getTokenSeparator());
            }
        } else {
            throw new IllegalArgumentException("Unexpected number type:" + value.getClass().getSimpleName());
        }
    }

    /**
     * Append a boolean value to the current data line.
     *
     * @param value Boolean value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendBoolean(Boolean value, boolean measureField) {
        if (value != null && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (value != null) {
                    currentLine.append(Boolean.toString(value));
                }
                currentLine.append(encoding.getTokenSeparator());
            }

        }
    }

    /**
     * Append a Double value to the current data line.
     *
     * @param value Double value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendDouble(Double value, boolean measureField) {
        if (value != null && !value.isNaN() && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (!value.isNaN()) {
                    currentLine.append(Double.toString(value));
                }
                currentLine.append(encoding.getTokenSeparator());
            }

        }
    }

    /**
     * Append a Float value to the current data line.
     *
     * @param value Float value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendFloat(Float value, boolean measureField) {
        if (value != null && !value.isNaN() && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (value != null && !value.isNaN()) {
                    currentLine.append(Double.toString(value));
                }
                currentLine.append(encoding.getTokenSeparator());
            }

        }
    }

    /**
     * Append a Integer value to the current data line.
     *
     * @param value Integer value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendInteger(Integer value, boolean measureField) {
        if (value != null && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (value != null) {
                    currentLine.append(Integer.toString(value));
                }
                currentLine.append(encoding.getTokenSeparator());
            }

        }
    }

    /**
     * Append a Integer value to the current data line.
     *
     * @param value String value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendString(String value, boolean measureField) {
        if (value != null && !value.isEmpty() && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (value != null && !value.isEmpty()) {
                    currentLine.append(value);
                }
                currentLine.append(encoding.getTokenSeparator());
            }
        }
    }

    /**
     * Append a Long value to the current data line.
     *
     * @param value String value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendLong(Long value, boolean measureField) {
        if (value != null && measureField) {
            emptyLine = false;
        }
        switch (getMode()) {
            case DATA_ARRAY -> currentArrayLine.add(value);
            case CSV -> {
                if (value != null) {
                    currentLine.append(value);
                }
                currentLine.append(encoding.getTokenSeparator());
            }
        }
    }

    /**
     * Append a value to the current data line.
     *
     * @param value value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    public void appendValue(Object value, boolean measureField) {
        if (value instanceof Number d) {
            appendNumber(d, measureField);
        } else if (value instanceof String s) {
            appendString(s, measureField);
        } else if (value instanceof Date d) {
            appendTime(d, measureField);
        } else if (value instanceof Boolean b) {
            appendBoolean(b, measureField);
        } else if (value == null) {
            appendString((String) null, measureField);
        } else {
            throw new IllegalArgumentException("Unssuported value type:" + value);
        }
    }

    public int endBlock() {
        if (!emptyLine) {
            switch (getMode()) {
                case DATA_ARRAY -> dataArray.add(currentArrayLine);
                case CSV -> {
                    values.append(currentLine);
                    // remove last token separator
                    values.deleteCharAt(values.length() - 1);
                    values.append(encoding.getBlockSeparator());
                }
                case COUNT -> count++;
            }
            return 1;
        }
        return 0;
    }

    public String getStringValues() {
        if (values != null) {
            return values.toString();
        }
        return null;
    }

    public List<Object> getDataArray() {
        return dataArray;
    }

    public int getCount() {
        return count;
    }

    public void appendHeaders(List<Field> fields) {
        switch (getMode()) {
            case CSV -> {
                boolean first = true;
                for (Field pheno : fields) {
                    // hack for the current graph in examind you only work when the main field is named "time"
                    if (csvHack && FieldType.TIME.equals(pheno.type) && first) {
                        values.append("time").append(encoding.getTokenSeparator());
                    } else {
                        values.append(pheno.label).append(encoding.getTokenSeparator());
                    }
                    first = false;
                }
                values.setCharAt(values.length() - 1, '\n');
            }
        }
    }

    public ResultMode getMode() {
        return mode;
    }

    public TextEncoderProperties getEncoding(){
        return encoding;
    }
}
