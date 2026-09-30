/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2026 Geomatys.
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
package com.examind.stac.core;

import javax.xml.namespace.QName;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Checks the layer names written in the WMS/WFS/WCS asset requests.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class DefaultSTACWorkerNameTest {

    @Test
    public void kvpNameTest() {
        assertEquals("city", DefaultSTACWorker.kvpName(new QName("city")));
        assertEquals("http%3A%2F%2Fexample.com%2Fns%3Acity", DefaultSTACWorker.kvpName(new QName("http://example.com/ns", "city")));
    }

    @Test
    public void wfsTypeNameTest() {
        assertEquals("city", DefaultSTACWorker.wfsTypeName(new QName("city")));
        assertEquals("ns:city&NAMESPACES=xmlns%28ns%3Dhttp%3A%2F%2Fexample.com%2Fns%29",
                DefaultSTACWorker.wfsTypeName(new QName("http://example.com/ns", "city")));
    }
}
