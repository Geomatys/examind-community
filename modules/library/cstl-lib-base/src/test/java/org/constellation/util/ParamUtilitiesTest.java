package org.constellation.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import java.io.IOException;
import java.util.List;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.opengis.parameter.ParameterDescriptor;

/**
 * @author Quentin Boileau (Geomatys)
 * @author Guilhem Legal (Geomatys)
 *
 * Hack: we use LenientComparable#equals with comparisonMode.APPROXIMATE because of a bug in SIS DefaultParameterValue.equals
 * that does not use deepEquals for comparing value (causing in issue to compare Double[]).
 * restore basic assertEquals when the issue will be fixed in SIS.
 */
public class ParamUtilitiesTest {

    public static class SimplePojo {
        private int id;
        private String name;

        private SimplePojo(){}

        public SimplePojo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final SimplePojo that = (SimplePojo) o;

            if (id != that.id) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + name.hashCode();
            return result;
        }
    }

    public static enum SimpleEnum { ONE, TWO, THREE}

    private static ParameterDescriptorGroup DESCRITPTOR;
    private static ParameterValueGroup VALUE;

    @BeforeClass
    public static void init() {
        final ParameterBuilder builder = new ParameterBuilder();

        final ParameterDescriptor<Double> subParam = builder.addName("doubleParam").setRequired(true).create(Double.class, 10.5);

        final GeneralParameterDescriptor[] params = {
            builder.addName("strParam").setRequired(true).create(String.class, "Test"),
            builder.addName("boolParam").setRequired(true).create(Boolean.class, true),
            builder.addName("objParam").setRequired(true).create(SimplePojo.class, null),
            builder.addName("intParam").setRequired(true).createBounded(0, 255, 0),
            builder.addName("doubleArrayParam").setRequired(true).create(Double[].class, null),
            builder.addName("enumParam").setRequired(false).createEnumerated(SimpleEnum.class, SimpleEnum.values(), null),
            builder.addName("subgroup").createGroup(2, 5, subParam)
        };

        Double[] dap = {2.1};
        DESCRITPTOR = builder.addName("group")
                .setRequired(true)
                .createGroup(params);
        VALUE = DESCRITPTOR.createValue();
        VALUE.parameter("strParam").setValue("AString");
        VALUE.parameter("boolParam").setValue(false);
        VALUE.parameter("enumParam").setValue(SimpleEnum.TWO);
        VALUE.parameter("intParam").setValue(55);
        VALUE.parameter("doubleArrayParam").setValue(dap);
        VALUE.parameter("objParam").setValue(new SimplePojo(1,"constellation"));

        List<ParameterValueGroup> subgroups = VALUE.groups("subgroup");

        double[] values = {0.5, 55.74, 66.1};
        int groups = subgroups.size();
        for (int i = 0; i < values.length; i++) {
            final ParameterValueGroup subgroup;
            if (i < groups) {
                subgroup = subgroups.get(i);
            } else {
                subgroup = VALUE.addGroup("subgroup");
            }
            subgroup.parameter("doubleParam").setValue(values[i]);
        }
    }

    @Test
    public void testSerializeParameterGroupJSON() throws JsonProcessingException {
        String jsonValue = ParamUtilities.writeParameterJSON(VALUE);
        String expectedJSON = "{\"strParam\":[\"AString\"],\"boolParam\":[false],\"objParam\":[{\"id\":1,\"name\":\"constellation\"}]," +
                "\"intParam\":[55],\"doubleArrayParam\":[[2.1]],\"enumParam\":[\"TWO\"]," +
                "\"subgroup\":[{\"doubleParam\":[0.5]},{\"doubleParam\":[55.74]},{\"doubleParam\":[66.1]}]}";

        Assert.assertNotNull(jsonValue);
        Assert.assertEquals(expectedJSON, jsonValue);

        try {
            ParamUtilities.writeParameterJSON(null);
            Assert.fail("writeParameterJSON(null) should fail.");
        } catch (NullPointerException ex) {
            // failure ok
        }
    }

    @Test
    public void testSerializeParameterValueJSON() throws JsonProcessingException {
        ParameterValue strParam = VALUE.parameter("strParam");
        String jsonValue = ParamUtilities.writeParameterJSON(strParam);
        String expectedJSON = "{\"strParam\":[\"AString\"]}";

        Assert.assertNotNull(jsonValue);
        Assert.assertEquals(expectedJSON, jsonValue);
    }

    @Test
    public void testDeserializeParameterGroupJSON() throws IOException {
        String serializedJSON = ParamUtilities.writeParameterJSON(VALUE);
        ParameterValueGroup parameterValue = (ParameterValueGroup) ParamUtilities.readParameterJSON(serializedJSON, DESCRITPTOR);

        Assert.assertNotNull(parameterValue);
        Assert.assertTrue(((LenientComparable)VALUE.parameter("doubleArrayParam")).equals(parameterValue.parameter("doubleArrayParam"), ComparisonMode.APPROXIMATE));
        Assert.assertTrue(((LenientComparable)VALUE).equals(parameterValue, ComparisonMode.APPROXIMATE));

        // test without array for "mono-occurrence"
        String json = "{\"strParam\":\"AString\",\"boolParam\":false,\"objParam\":{\"id\":1,\"name\":\"constellation\"}," +
                "\"intParam\":55,\"doubleArrayParam\":[[2.1]],\"enumParam\":\"TWO\"," +
                "\"subgroup\":[{\"doubleParam\":0.5},{\"doubleParam\":55.74},{\"doubleParam\":66.1}]}";
        parameterValue = (ParameterValueGroup) ParamUtilities.readParameterJSON(json, DESCRITPTOR);

        Assert.assertNotNull(parameterValue);
        Assert.assertTrue(((LenientComparable)VALUE).equals(parameterValue, ComparisonMode.APPROXIMATE));
    }

    @Test
    public void testDeserializeError() throws JsonProcessingException {

        String serializedJSON = ParamUtilities.writeParameterJSON(VALUE);

        try {
            ParamUtilities.readParameterJSON(null, DESCRITPTOR);
            Assert.fail("readParameterJSON(null, desc) should fail.");
        } catch (NullPointerException ex) {
            // failure ok
        } catch (IOException e) {
            Assert.fail("readParameterJSON(null, desc) should not raise an IOException.");
        }

        try {
            ParamUtilities.readParameterJSON(serializedJSON, null);
            Assert.fail("readParameterJSON(json, null) should fail.");
        } catch (NullPointerException ex) {
            // failure ok
        } catch (IOException e) {
            Assert.fail("readParameterJSON(json, null) should not raise an IOException.");
        }

        try {
            ParamUtilities.readParameterJSON(null, null);
            Assert.fail("readParameterJSON(null, null) should fail.");
        } catch (NullPointerException ex) {
            // failure ok
        } catch (IOException e) {
            Assert.fail("readParameterJSON(null, null) should not raise an IOException.");
        }

        try {
            //test with not matching JSON <-> GeneralParameterDescriptor
            ParamUtilities.readParameterJSON("{\"test\":[\"test\"]}", DESCRITPTOR);
            Assert.fail("Test with not matching JSON <-> GeneralParameterDescriptor should fail.");
        } catch (IOException ex) {
            // failure ok
        }

        try {
            //test with not completely matching JSON <-> GeneralParameterDescriptor (missing mandatory parameters)
            ParamUtilities.readParameterJSON("{\"strParam\":[\"AString\"]}", DESCRITPTOR);
            Assert.fail("Test with not matching JSON <-> GeneralParameterDescriptor should fail.");
        } catch (IOException ex) {
            // failure ok
        }

    }

    @Test
    public void testDeserializeParameterValueJSON() throws IOException {
        ParameterValue strParam = VALUE.parameter("strParam");
        String jsonValue = ParamUtilities.writeParameterJSON(strParam);
        ParameterValue parameterValue = (ParameterValue) ParamUtilities.readParameterJSON(jsonValue, strParam.getDescriptor());

        Assert.assertNotNull(parameterValue);
        Assert.assertEquals(strParam, parameterValue);
    }

    @Test
    public void testSerializeDescriptorJSON() throws JsonProcessingException {
        String jsonValue = ParamUtilities.writeParameterDescriptorJSON(DESCRITPTOR);
        Assert.assertNotNull(jsonValue);

        String expectedJSON = "{\"name\":\"group\",\"minOccurs\":1,\"maxOccurs\":1,\"descriptors\":[" +
                "{\"name\":\"strParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"java.lang.String\",\"defaultValue\":\"Test\"}," +
                "{\"name\":\"boolParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"java.lang.Boolean\",\"defaultValue\":true}," +
                "{\"name\":\"objParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"org.constellation.util.ParamUtilitiesTest.SimplePojo\"}," +
                "{\"name\":\"intParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"java.lang.Integer\",\"defaultValue\":0,\"restriction\":{\"minValue\":0,\"maxValue\":255}}," +
                "{\"name\":\"doubleArrayParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"java.lang.Double[]\"}," +
                "{\"name\":\"enumParam\",\"minOccurs\":0,\"maxOccurs\":1,\"class\":\"org.constellation.util.ParamUtilitiesTest.SimpleEnum\",\"restriction\":{\"validValues\":[\"ONE\",\"TWO\",\"THREE\"]}}," +
                "{\"name\":\"subgroup\",\"minOccurs\":2,\"maxOccurs\":5,\"descriptors\":[{\"name\":\"doubleParam\",\"minOccurs\":1,\"maxOccurs\":1,\"class\":\"java.lang.Double\",\"defaultValue\":10.5}]}]}";

        Assert.assertNotNull(jsonValue);
        Assert.assertEquals(expectedJSON, jsonValue);
    }

}
