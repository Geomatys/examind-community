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
package org.constellation.admin;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.style.DefaultMutableStyle;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StyleBusinessTest extends org.constellation.test.SpringContextTest {

    @Autowired
    private IStyleBusiness styleBusiness;

    @AfterClass
    public static void tearDown() {
        try {
            final IStyleBusiness style = SpringHelper.getBean(IStyleBusiness.class);
            if (style != null) {
                style.deleteAll();
            }
        } catch (ConstellationException ex) {
            Logger.getLogger("org.constellation.admin").log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void createStyle() throws Exception {
        DefaultMutableStyle style = new DefaultMutableStyle();
        style.setName("hauteur du géoïde");
        Integer id = styleBusiness.createStyle("sld-temp", style);

        org.opengis.style.Style s = styleBusiness.getStyle("sld-temp", "hauteur du géoïde");
        Assert.assertNotNull(s);
        
        s = styleBusiness.getStyle(id);
        Assert.assertNotNull(s);
        
        style.setName("hauteur du géoïde v2");
        styleBusiness.updateStyle(id, style);
        
        s = styleBusiness.getStyle(id);
        Assert.assertEquals("hauteur du géoïde v2", s.getName());
        
        s = styleBusiness.getStyle("sld-temp", "hauteur du géoïde v2");
        Assert.assertNotNull(s);
    }
}
