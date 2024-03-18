/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.admin;

import org.geotoolkit.style.DefaultMutableStyle;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.style.Style;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StyleBusinessTest extends AbstractBusinessTest {

    @Test
    public void createStyle() throws Exception {
        DefaultMutableStyle style = new DefaultMutableStyle();
        style.setName("hauteur du géoïde");
        Integer id = styleBusiness.createStyle("sld-temp", style);

        org.opengis.style.Style s = (org.opengis.style.Style) styleBusiness.getStyle("sld-temp", "hauteur du géoïde");
        Assert.assertNotNull(s);

        s = (Style) styleBusiness.getStyle(id);
        Assert.assertNotNull(s);

        style.setName("hauteur du géoïde v2");
        styleBusiness.updateStyle(id, style);

        s = (Style) styleBusiness.getStyle(id);
        Assert.assertEquals("hauteur du géoïde v2", s.getName());

        s = (Style) styleBusiness.getStyle("sld-temp", "hauteur du géoïde v2");
        Assert.assertNotNull(s);
    }
}
