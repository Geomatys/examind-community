/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package com.examind.wps.util;

import com.examind.wps.api.IOParameterException;
import static com.examind.wps.util.WPSConstants.URN_SEPARATOR;
import static com.examind.wps.util.WPSUtils.parseBBOXData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.wps.xml.v200.Data;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.LiteralValue;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomays)
 */
public class CsvFileConditionalValues implements ConditionalValues {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.wps.util");
    
    private static final String MATCH_ALL_VALUE = "X";
    
    private final Input[] inputs;
    private final List<Object[]> rows = new ArrayList<>();
    
    private final Map<String, Set<String>> fullParamValue = new HashMap<>();
    
    private static class Input {
        public final String id;
        public final Class clazz;
        
        public Input(String id, Class clazz) {
            this.id = id;
            this.clazz = clazz;
        }
    }
    
    public CsvFileConditionalValues(Path cpvFile) throws IOException {
        String str = IOUtilities.toString(cpvFile);
        String[] lines = str.split("\n");
        if (lines.length > 1) {
            String[] inputIds = lines[0].split(";");
            this.inputs = new Input[inputIds.length];
            for (int i = 0; i < inputIds.length; i++) {
                String inputId = inputIds[i];
                if (inputId.endsWith(":bbox")) {
                    inputId = inputId.substring(0, inputId.length() - 5);
                    this.inputs[i] = new Input(inputId, Envelope.class);
                } else {
                    this.inputs[i] = new Input(inputId, String.class);
                }
                fullParamValue.put(inputId, new HashSet<>());
            }
            for (int i = 1; i < lines.length; i++) {
                String[] line = lines[i].split(";", -1);
                if (line.length != inputIds.length) {
                    throw new IOException("Malformed CSV conditional process file. line length != headers length");
                }
                Object[] row = new Object[inputIds.length];
                for (int j = 0; j < inputIds.length; j++) {
                    String value = line[j];
                    Class inputClass = this.inputs[j].clazz;
                    Object inputValue = value;
                    if (value != null && !value.isEmpty() && !value.equals(MATCH_ALL_VALUE)) {
                        if (inputClass.equals(Envelope.class)) {
                            try {
                                inputValue = parseBBox(value);
                            } catch (FactoryException ex) {
                                LOGGER.log(Level.WARNING, "Error while parsing conditional file envelope (line=" + i + " row=" + j + ") : " + value, ex);
                            }
                        } else {
                            fullParamValue.get(inputIds[j]).add(value);
                        }
                        
                    }
                    row[j] = inputValue;
                }
               rows.add(row);
            }
        } else {
            inputs = null;
        }
    }
    
    @Override
    public Map<String, Set<String>> autocomplete(String version, List<DataInput> dataInputs) throws IOParameterException {
        
        if (dataInputs == null || dataInputs.isEmpty()) {
            return fullParamValue;
        }
        
        // fill up with match all value
        Object[] key = new Object[inputs.length];
        for (int i = 0; i < key.length; i++) {
            key[i] = MATCH_ALL_VALUE;
        }
        // fill up with request input
        for (DataInput in : dataInputs) {
            String inId = in.getId();
            // TODO not safe
            inId        = inId.substring(inId.lastIndexOf(URN_SEPARATOR) + 1, inId.length());
            int index = getInputIndex(inId);
            if (index != -1) {
                Object value = getInputValue(in);
                key[index] = value;
            }
        }
        
        // init results
        Map<String, Set<String>> result = new HashMap<>();
        for (int i = 0; i < key.length; i++) {
            result.put(inputs[i].id, new HashSet<>());
        }
        // look for matching rows
        for (Object[] row : rows) {
            boolean match = true;
            for (int i = 0; i < key.length; i++) {
                if (key[i] instanceof String keyStr) {
                    if (!(keyStr.equals(MATCH_ALL_VALUE) || keyStr.equals(row[i]))) {
                        match = false;
                        break;
                    }
                } else if (key[i] instanceof Envelope keyEnv) {
                    try {
                        if (row[i] instanceof String rowStr && !rowStr.equalsIgnoreCase(MATCH_ALL_VALUE)) {
                            match = false;
                            break;
                        }
                        if (row[i] instanceof Envelope rowEnv) {
                            GeneralEnvelope intersection = Envelopes.intersect(keyEnv, rowEnv);
                            if (intersection == null || intersection.isEmpty()) {
                                match = false;
                                break;
                            }
                        }
                    } catch (TransformException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }
            // the row match, add the values to the result
            if (match) {
                for (int i = 0; i < key.length; i++) {
                    Object value = row[i];
                    if (!(value instanceof String str && str.isEmpty())) {
                        if (value.equals(MATCH_ALL_VALUE)) {
                            result.get(inputs[i].id).addAll(fullParamValue.get(inputs[i].id));
                        } else {
                            result.get(inputs[i].id).add(value.toString());
                        }
                    }
                }
            }
        }
        return result;
    }
    
    
    private int getInputIndex(String inputId) {
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i].id.equals(inputId)) return i;
        }
        return -1;
    }
    
    
    private Object getInputValue(DataInput input) throws IOParameterException {
        if (input.getReference() != null) {
            throw new IOParameterException("Reference input not supported", input.getId());
        } else if (input.getData() != null) {
            
            final Data dataType = input.getData();
            if (dataType.getBoundingBoxData() != null) {
                Envelope env = parseBBOXData(input.getId(), dataType.getBoundingBoxData());
                return env;

            // issue here : we don't need to have a literal value. the value can be directly in content
            // TODO see dirty patch added below
            } else if (dataType.getLiteralData() != null) {
                final LiteralValue literal = dataType.getLiteralData();
                if (literal != null) {
                    final String value = literal.getValue();
                    return value;
                } else {
                    throw new IOParameterException("BBox input not supported", input.getId());
                }
            } else {
                // dirty patch for literal values without LiteralData
                if (dataType.getContent().size() == 1 &&
                    dataType.getContent().get(0) instanceof String value) {
                    return value;
                } else {
                    throw new IOParameterException("Complex input not supported", input.getId());
                }
            }
            
        } else {
            throw new IOParameterException("Input doesn't have data or reference.", input.getId());
        }
    }
    
    private Envelope parseBBox(final String bboxStr) throws FactoryException {
        final String[] part = bboxStr.split(",");
        final CoordinateReferenceSystem crs;
        if (part.length > 4) {
            crs = CRS.forCode(part[4]);
        } else {
            crs = CommonCRS.WGS84.geographic();
        }
        GeneralEnvelope envelope = new GeneralEnvelope(crs);
        
        envelope.setRange(0, Double.parseDouble(part[0]), Double.parseDouble(part[2]));
        envelope.setRange(1, Double.parseDouble(part[1]), Double.parseDouble(part[3]));
        return envelope;
    }
}