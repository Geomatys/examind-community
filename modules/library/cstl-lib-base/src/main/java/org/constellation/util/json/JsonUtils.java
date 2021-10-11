package org.constellation.util.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.Static;
import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.dto.process.DataProcessReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class JsonUtils extends Static {

    /**
     * Jackson JsonFactory used to create temporary JsonGenerators.
     */
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private JsonUtils(){}

    /**
     * Write value object depending of object type.
     *
     * @param value
     * @param writer
     * @throws java.io.IOException
     * @throws IllegalArgumentException
     */
    static void writeValue(Object value, JsonGenerator writer) throws IOException, IllegalArgumentException {

        if (value == null) {
            writer.writeNull();
            return;
        }

        Class binding = value.getClass();

        if (binding.isArray()) {
            if (byte.class.isAssignableFrom(binding.getComponentType())) {
                writer.writeBinary((byte[])value);
            } else {
                writer.writeStartArray();
                final int size = Array.getLength(value);
                for (int i = 0; i < size; i++) {
                    writeValue(Array.get(value, i), writer);
                }
                writer.writeEndArray();
            }

        } else if (Collection.class.isAssignableFrom(binding)) {
            writer.writeStartArray();
            Collection coll = (Collection) value;
            for (Object obj : coll) {
                writeValue(obj, writer);
            }
            writer.writeEndArray();

        } else if (Double.class.isAssignableFrom(binding)) {
            writer.writeNumber((Double) value);
        } else if (Float.class.isAssignableFrom(binding)) {
            writer.writeNumber((Float) value);
        } else if (Short.class.isAssignableFrom(binding)) {
            writer.writeNumber((Short) value);
        } else if (Byte.class.isAssignableFrom(binding)) {
            writer.writeNumber((Byte) value);
        } else if (BigInteger.class.isAssignableFrom(binding)) {
            writer.writeNumber((BigInteger) value);
        } else if (BigDecimal.class.isAssignableFrom(binding)) {
            writer.writeNumber((BigDecimal) value);
        } else if (Integer.class.isAssignableFrom(binding)) {
            writer.writeNumber((Integer) value);
        } else if (Long.class.isAssignableFrom(binding)) {
            writer.writeNumber((Long) value);

        } else if (Boolean.class.isAssignableFrom(binding)) {
            writer.writeBoolean((Boolean) value);
        } else if (String.class.isAssignableFrom(binding)) {
            writer.writeString(String.valueOf(value));
        } else {
            //fallback
            try (final JsonGenerator tempGenerator = JSON_FACTORY.createGenerator(new ByteArrayOutputStream(), JsonEncoding.UTF8)){
                //HACK : create a temporary writer to write object.
                //In case of writeObject(value) fail input writer will not be in illegal state.
                tempGenerator.setCodec(new ObjectMapper());
                tempGenerator.writeObject(value);

                //using jackson auto mapping
                writer.writeObject(value);
            } catch (Throwable ex) {
                // last chance with converter and toString()
                writer.writeString(ObjectConverters.convert(value, String.class));
            }
        }
    }

    /**
     * Try to convert a JsonNode into an Object.
     * This method use Jackson ObjectMapper and ApacheSIS ObjectConverters (if JsonNode is a text).
     *
     * @param node JsonNode that contain value
     * @param binding expected java Class
     * @param parameterName parameter name for exception message purpose
     * @return Object instance of {@code binding}.
     * @throws IOException if node can't be converted in {@code binding}
     */
    static Object readValue(JsonNode node, Class binding, String parameterName) throws IOException {
        try {
            // particular case if binding extends a Resource, a Resource is returned.
            if (Resource.class.isAssignableFrom(binding)) {
                ObjectMapper mapper = new ObjectMapper();
                final DataProcessReference o = mapper.treeToValue(node, DataProcessReference.class);
                return ObjectConverters.convert(o, Resource.class);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.treeToValue(node, binding);
            }
        } catch (JsonProcessingException ex) {
            if (node.isTextual()) {
                try {
                    return ObjectConverters.convert(node.textValue(), binding);
                } catch (UnconvertibleObjectException bis) {
                    ex.addSuppressed(bis);
                    //ObjectConverters doesn't work
                }
            }

            throw new IOException("Can't convert JSON node ("+node.getNodeType().name()+") for parameter "+parameterName+" in Java type "+binding.getName(), ex);
        }
    }

    public static Properties toProperties(Map<String, Object> map) {
        return addValue(new Properties(), new Stack<>(), map);
    }

    private static Properties addValue(Properties properties, Stack<String> path, Object o) {
        if (o instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) o;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                path.push(entry.getKey());

                addValue(properties, path, entry.getValue());

                path.pop();
            }
        } else {
            StringBuilder builder = new StringBuilder();
            for (String string : path) {
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(string);
            }
            properties.put(builder.toString(), String.valueOf(o));
        }
        return properties;
    }

    public static Map<String, Object> toJSon(Properties list) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<Object, Object> entry : list.entrySet()) {
            String key = (String) entry.getKey();
            String[] tokens = key.split("\\.");
            addValue(map, tokens, 0, (String) entry.getValue());
        }
        return map;

    }

    public static void addValue(Map<String, Object> map, String[] tokens,
            int i, String value) {
        String token = tokens[i];
        if (i == tokens.length - 1) {
            map.put(token, value);
        } else {
            Map<String, Object> o = (Map<String, Object>) map.get(token);
            if (o == null) {
                o = new HashMap<>();
                map.put(token, o);
            }
            addValue(o, tokens, i + 1, value);
        }
    }

}
