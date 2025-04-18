
package org.constellation.store.observation.db.result;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotoolkit.observation.result.ResultBuilder;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.model.TextEncoderProperties;

/**
 *
 * @author glegal
 */
public class CsvFlatResultBuilder extends ResultBuilder {
    
    private final boolean profile;
    private final ProcedureInfo procedure;
    private StringBuilder values;
    
    private final TextEncoderProperties encoding;
    private Map<String, csvFlatLine> currentLines = new HashMap<>();
    
    private final List<Field> fields;
            
    public CsvFlatResultBuilder(ProcedureInfo procedure, List<Field> fields, final TextEncoderProperties encoding) {
        super(ResultMode.CSV, encoding, false);
        this.encoding = encoding;
        values = new StringBuilder();
        this.procedure = procedure;
        this.profile = "profile".equals(procedure.type);
        // remove fields before the main field (included)
        int i = 0;
        for (; i < fields.size(); i++) {
            if (fields.get(i).name.equals(procedure.mainField.name)) break;
        }
        this.fields = fields.subList(i + 1, fields.size());
    }
    
    /**
     * Reset all values.
     */
    @Override
    public void clear() {
       values = new StringBuilder();
    }

    /**
     * Start a new measure line.
     */
    @Override
    public void newBlock() {
        currentLines.clear();
        for (Field field : fields) {
            currentLines.put(field.name, new csvFlatLine(field, procedure, encoding));
        }
    }

    /**
     * Append a date to the current data line.
     *
     * @param value Date value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    @Override
    public void appendTime(Date value, boolean measureField, Field f) {
        String strValue = "";
        if (value!= null) {
            synchronized (format2) {
                strValue = format2.format(value);
            }
        }
        for (csvFlatLine line : currentLines.values()) {
            line.line = line.line.replace("${time}", strValue);
        }
    }
    
    /**
     * Append a date in millisecond to the current data line.
     *
     * @param value Date value in millisecond.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    @Override
    public void appendTime(Long value, boolean measureField, Field f) {
        Date d = null;
        if (value != null) {
            d = new Date(value);
        }
        appendTime(d, measureField, f);
    }

    /**
     * Append a number value to the current data line.
     *
     * @param value Number value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     */
    @Override
    public void appendNumber(Number value, boolean measureField, Field field) {
        if (value instanceof Double d) {
            appendDouble(d, measureField, field);
        } else if (value instanceof Float f) {
            appendFloat(f, measureField, field);
        } else if (value instanceof Integer i) {
            appendInteger(i, measureField, field);
        } else if (value instanceof Long l) {
            appendLong(l, measureField, field);
        } else if (value == null) {
            // do nothing dor now
        } else {
            throw new IllegalArgumentException("Unexpected number type:" + value.getClass().getSimpleName());
        }
    }

    private csvFlatLine getCurrentLine(String fieldName) {
        csvFlatLine currentLine = currentLines.get(fieldName);
        if (currentLine != null) {
            return currentLine;
        }

        // hack. we assume that we are in a quality field case.
        /* we use that last field data added
        if (lastFieldId != null && currentLines.containsKey(lastFieldId)) {
            currentLine = currentLines.get(lastFieldId);
            if (currentLine.resultAdded) {
                return currentLine;
            }
        }*/
        throw new IllegalArgumentException("No lines for field:" + fieldName);
    }

    /**
     * Append a Double value to the current data line.
     *
     * @param value Double value.
     * @param measureField if set to {@code false} this will not change the status of empty line.
     * @param f
     */
    @Override
    public void appendDouble(Double value, boolean measureField, Field f) {
        if (value != null && !value.isNaN()) {
            if (profile && f.name.equals(procedure.mainField.name)) {
                for (csvFlatLine line : currentLines.values()) {
                    line.line = line.line.replace("${z_value}", Double.toString(value));
                }
            } else {
                csvFlatLine currentLine = getCurrentLine(f.name);
                currentLine.appendValue(value);
            }
        }
    }
    
     /**
     * {@inheritDoc}
     */
    @Override
    public void appendBoolean(Boolean value, boolean measureField, Field f) {
        if (value != null) {
            csvFlatLine currentLine = getCurrentLine(f.name);
            currentLine.appendValue(value);
        }
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public void appendFloat(Float value, boolean measureField, Field f) {
        if (value != null && !Float.isNaN(value)) {
            csvFlatLine currentLine = getCurrentLine(f.name);
            currentLine.appendValue(value);
        }
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public void appendInteger(Integer value, boolean measureField, Field f) {
        if (value != null) {
            csvFlatLine currentLine = getCurrentLine(f.name);
            currentLine.appendValue(value);
        }
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public void appendString(String value, boolean measureField, Field f) {
         // we don't want to add the id
        if (!f.name.equals("id") && value != null) {
            csvFlatLine currentLine = getCurrentLine(f.name);
            currentLine.appendValue(value);
        }
    }

     /**
     * {@inheritDoc}
     */
    @Override
    public void appendLong(Long value, boolean measureField, Field f) {
        if (value != null) {
            csvFlatLine currentLine = getCurrentLine(f.name);
            currentLine.appendValue(value);
        }
    }

    @Override
    public int endBlock() {
        int nbLine = 0;
        for (csvFlatLine line : currentLines.values()) {
            if (!line.emptyLine) {
                values.append(line.line);
                nbLine++;
            }
        }
        return nbLine;
    }

    @Override
    public String getStringValues() {
        if (values != null) {
            return values.toString();
        }
        return null;
    }

    @Override
    public List<Object> getDataArray() {
        throw new IllegalStateException("Not available in edilabo export");
    }

    @Override
    public int getCount() {
        throw new IllegalStateException("Not available in edilabo export");
    }

    // 	sensor_metadata	 obsprop_metadata observation_z_value	observation_metadata
    //	

    @Override
    public void appendHeaders(List<Field> fields) {
        values.append("time").append(encoding.getTokenSeparator());
        values.append("sensor_id").append(encoding.getTokenSeparator());
        values.append("sensor_name").append(encoding.getTokenSeparator());
        values.append("sensor_description").append(encoding.getTokenSeparator());
        values.append("obsprop_id").append(encoding.getTokenSeparator());
        values.append("obsprop_name").append(encoding.getTokenSeparator());
        values.append("obsprop_desc").append(encoding.getTokenSeparator());
        values.append("obsprop_unit").append(encoding.getTokenSeparator());
        // quality ?
        if (profile) {
            values.append("z_value").append(encoding.getTokenSeparator());
        }
        values.append("value").append(encoding.getBlockSeparator());
    }

    @Override
    public ResultMode getMode() {
        return ResultMode.CSV;
    }

    @Override
    public TextEncoderProperties getEncoding(){
        return encoding;
    }
    
    private static class csvFlatLine {
         
        public boolean emptyLine = true;
        public String line;
        private final Field field;
        
        public csvFlatLine(Field field, ProcedureInfo procedure, TextEncoderProperties encoding) {
            this.field = field;
            StringBuilder sb = new StringBuilder();
            
            // prepare the line
            sb.append("${time}").append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(procedure.id)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(procedure.name)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(procedure.description)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(field.name)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(field.label)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(field.description)).append(encoding.getTokenSeparator());
            sb.append(valueOrEmpty(field.uom)).append(encoding.getTokenSeparator());
            if ("profile".equals(procedure.type)) {
                sb.append("${z_value}").append(encoding.getTokenSeparator());
            }
            sb.append("${result}").append(encoding.getBlockSeparator());

            line = sb.toString();
            this.emptyLine   = true;
        }
        
        public void appendValue(String value) {
            line = line.replace("${result}", value);
            emptyLine = false;
        }
        
        public void appendValue(Double value) {
            line = line.replace("${result}", Double.toString(value));
            emptyLine = false;
        }
        
        public void appendValue(Integer value) {
            line = line.replace("${result}", Integer.toString(value));
            emptyLine = false;
        }
        
        public void appendValue(Float value) {
            line = line.replace("${result}", Float.toString(value));
            emptyLine = false;
        }
        
        public void appendValue(Long value) {
            line = line.replace("${result}", Long.toString(value));
            emptyLine = false;
        }
        
        public void appendValue(Boolean value) {
            line = line.replace("${result}", Boolean.toString(value));
            emptyLine = false;
        }
        
        private static String valueOrEmpty(String s) {
            if (s == null) return "";
            return s;
        } 
    }
}