package org.constellation.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import javax.measure.Unit;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.ObjectConverters;

/**
 * Deserialize a {@link GeneralParameterDescriptor} from a JSON.
 *
 * @author Johann Sorel (Geomatys)
 * @see org.constellation.util.json.ParameterDescriptorJSONSerializer
 */
public class ParameterDescriptorJSONDeserializer extends JsonDeserializer<GeneralParameterDescriptor> {

    public ParameterDescriptorJSONDeserializer() {
    }

    @Override
    public GeneralParameterDescriptor deserialize(JsonParser parser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        final JsonNode rootNode = parser.getCodec().readTree(parser);
        if (!rootNode.isObject()) {
            throw new IOException("Invalid JSON : Expecting JSON object as root node");
        }

        return readGeneralDesc(rootNode);
    }


    private GeneralParameterDescriptor readGeneralDesc(JsonNode node) throws IOException {
        if (node.has("descriptors")) {
            return readGroupDesc(node);
        } else {
            return readParamDesc(node);
        }
    }

    private ParameterDescriptorGroup readGroupDesc(JsonNode node) throws IOException {

        final ParameterBuilder builder = new ParameterBuilder();
        builder.addName(node.get("name").textValue());
        if (node.has("description")) {
            builder.setDescription(node.get("description").textValue());
        }
        int minOcc = 1;
        int maxOcc = 1;
        if (node.has("minOccurs")) minOcc = node.get("minOccurs").asInt();
        if (node.has("maxOccurs")) maxOcc = node.get("maxOccurs").asInt();
        final List<GeneralParameterDescriptor> atts = new ArrayList<>();

        for (JsonNode desc : (ArrayNode)node.get("descriptors")) {
            atts.add(readGeneralDesc(desc));
        }

        return builder.createGroup(minOcc, maxOcc, atts.toArray(GeneralParameterDescriptor[]::new));
    }

    private ParameterDescriptor readParamDesc(JsonNode node) throws IOException {

        final ParameterBuilder builder = new ParameterBuilder();
        builder.addName(node.get("name").textValue());
        if (node.has("description")) {
            builder.setDescription(node.get("description").textValue());
        }
        int minOcc = 1;
        if (node.has("minOccurs")) minOcc = node.get("minOccurs").intValue();
        builder.setRequired(minOcc == 1);

        final Class valueClass;
        try {
            valueClass = Class.forName(node.get("class").asText());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage(), ex);
        }


        Unit unit = null;
        Object defaultValue = null;
        if (node.has("unit")) {
            unit = ObjectConverters.convert(node.get("unit").asText(), Unit.class);
        }
        if (node.has("defaultValue")) {
            defaultValue = JsonUtils.readValue(node.get("defaultValue"), valueClass, "defaultValue");
        }

        Set validValues = null;
        Comparable minValue = null;
        Comparable maxValue = null;
        if (node.has("restriction")) {
            final JsonNode restriction = node.get("restriction");
            if (restriction.has("minValue")) {
                minValue = (Comparable) JsonUtils.readValue(restriction.get("minValue"), valueClass, "minValue");
            }
            if (restriction.has("maxValue")) {
                maxValue = (Comparable) JsonUtils.readValue(restriction.get("maxValue"), valueClass, "maxValue");
            }
            if (restriction.has("validValues")) {
                final ArrayNode array = (ArrayNode) restriction.get("validValues");
                validValues = new LinkedHashSet();
                for (JsonNode v : array) {
                    validValues.add(JsonUtils.readValue(v, valueClass, "validValue"));
                }
            }
        }

        if (validValues != null) {
            return builder.createEnumerated(valueClass, validValues.toArray(new IntFunction() {
                @Override
                public Object apply(int value) {
                    return Array.newInstance(valueClass, value);
                }
            }), defaultValue);
        } else if (minValue != null) {
            final Class<? extends Comparable> comparableClass = (Class)valueClass;
            try {
                //TODO can not find a way to call this directly because of generics, bypass it using reflection
                /*return builder.createBounded(
                comparableClass,
                comparableClass.cast(minValue),
                comparableClass.cast(maxValue),
                comparableClass.cast(defaultValue));*/
                return (ParameterDescriptor) builder.getClass()
                        .getMethod("createBounded", Class.class, Object.class, Object.class, Object.class)
                        .invoke(builder, comparableClass, minValue, maxValue, defaultValue);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        } else {
            if (unit != null && Double.class.equals(valueClass) && defaultValue != null) {
                return builder.create((Double)defaultValue, unit);
            } else {
                return builder.create(valueClass, defaultValue);
            }

        }
    }

}
