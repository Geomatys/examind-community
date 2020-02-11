/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.json.binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author guilhem
 */
public class JsonBindingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public JsonBindingTest() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerSubtypes(new NamedType(PointSymbolizer.class, "point"),
                                new NamedType(LineSymbolizer.class,"line"),
                                new NamedType(PolygonSymbolizer.class, "polygon"),
                                new NamedType(TextSymbolizer.class,"text"),
                                new NamedType(RasterSymbolizer.class,"raster"),
                                new NamedType(CellSymbolizer.class,"cell"),
                                new NamedType(PieSymbolizer.class,"pie"),
                                new NamedType(IsolineSymbolizer.class,"isoline"),
                                new NamedType(DynamicRangeSymbolizer.class,"dynamicrange"));
    }

    @Test
    public void unmarshallingTest() throws IOException {

        WrapperInterval result = objectMapper.readValue(getResourceAsStream("org/constellation/json/binding/wrapper1.json"), WrapperInterval.class);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getStyle());
        Assert.assertNotNull(result.getIntervalValues());

        result = objectMapper.readValue(getResourceAsStream("org/constellation/json/binding/wrapper2.json"), WrapperInterval.class);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getStyle());
        Assert.assertNotNull(result.getIntervalValues());

    }

    @Test
    public void marshallingTest() throws IOException {
        RasterSymbolizer rs = new RasterSymbolizer();
        rs.setColorMap(new ColorMap());
        rs.setOpacity(1.0);
        TextSymbolizer ts = new TextSymbolizer();
        ts.setName("text-symbolize");
        LineSymbolizer ls = new LineSymbolizer();
        ls.setName("ls-symbolize");
        IsolineSymbolizer isolineSymb = new IsolineSymbolizer();
        isolineSymb.setIsolineOnly(true);
        isolineSymb.setLineSymbolizer(ls);
        isolineSymb.setRasterSymbolizer(rs);
        isolineSymb.setTextSymbolizer(ts);
        isolineSymb.setName("style-isoline");
        objectMapper.writeValue(System.out, isolineSymb);
    }

    public static InputStream getResourceAsStream(final String url) {
        final ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(url);
    }

    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
    }
}
