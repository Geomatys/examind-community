package org.constellation.map.featureinfo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.sis.measure.Units;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

public class CoverageInfoTest {

    @Test
    public void write() throws Exception {
        final Instant now = Instant.parse("2019-12-03T10:15:30.002Z");
        final CoverageInfo info = new CoverageInfo("my Layer", now, -13.);

        info.getValues().add(new CoverageInfo.Sample("b0", 0.2, Units.METRES_PER_SECOND));
        info.getValues().add(new CoverageInfo.Sample("this is the second band", -120.2, Units.DEGREE));

        final ObjectMapper mapper = new ObjectMapper()
                .disable(SerializationFeature.INDENT_OUTPUT);

        final String strInfo = mapper.writeValueAsString(info);
        final String expectedJson =
                "{" +
                    "\"layer\":\"my Layer\"," +
                    "\"elevation\":-13.0," +
                    "\"values\":[" +
                        "{\"name\":\"b0\",\"value\":0.2,\"unit\":\"m∕s\"}," +
                        "{\"name\":\"this is the second band\",\"value\":-120.2,\"unit\":\"°\"}" +
                    "]," +
                    "\"time\":\"2019-12-03T10:15:30.002Z\"" +
                "}";
        Assert.assertEquals("JSON representation of coverage GetFeatureInfo", expectedJson, strInfo);
    }
}
