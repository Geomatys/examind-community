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
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Style;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.style.DefaultMutableStyle;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class StyleBusinessTest {

    @Autowired
    private IStyleBusiness styleBusiness;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("StyleBusinessTest");
    }

    @AfterClass
    public static void tearDown() {
        try {
            final IStyleBusiness style = SpringHelper.getBean(IStyleBusiness.class);
            if (style != null) {
                style.deleteAll();
            }
            ConfigDirectory.shutdownTestEnvironement("StyleBusinessTest");
        } catch (ConstellationException ex) {
            Logging.getLogger("org.constellation.admin").log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void createStyle() throws Exception {
        DefaultMutableStyle style = new DefaultMutableStyle();
        style.setName("hauteur du géoïde");
        styleBusiness.createStyle("sld-temp", style);

        org.opengis.style.Style s = styleBusiness.getStyle("sld-temp", "hauteur du géoïde");
        Assert.assertNotNull(s);
    }
}
