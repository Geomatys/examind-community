/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.util;

import java.util.Map;
import java.util.Properties;
import org.constellation.util.json.JsonUtils;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JSonUtilsTest {

    @Test
    public void testToJSon() {
        Properties list = new Properties();
        list.put("a", "bcde");
        list.put("ab.c", "de");
        list.put("ab.d", "de");

        Map<String, Object> p2h = JsonUtils.toJSon(list);
        System.out.println(p2h);

        Properties properties = JsonUtils.toProperties(p2h);

        System.out.println(properties);
    }
}
