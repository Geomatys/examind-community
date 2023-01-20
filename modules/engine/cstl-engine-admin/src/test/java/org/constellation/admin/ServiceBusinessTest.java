/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2014 Geomatys.
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

import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import org.constellation.dto.service.ServiceComplete;

public class ServiceBusinessTest extends AbstractBusinessTest {

    @Test
    public void createService() throws Exception {
        final Details frDetails = new Details("name", "identifier", Arrays.asList("keyword1", "keyword2"), "description", Arrays.asList("version1"), new Contact(), new AccessConstraint(), true, "FR");
        Integer id = serviceBusiness.create("wms", "test", new LayerContext(), frDetails, null);
        Assert.assertTrue(serviceBusiness.getServiceIdentifiers("wms").contains("test"));

        final Details jpnDetails = new Details("nameJPN", "identifierJPN", Arrays.asList("keyword1JPN", "keyword2JPN"), "descriptionJPN", Arrays.asList("version1"), new Contact(), new AccessConstraint(), true, "jpn");
        serviceBusiness.setInstanceDetails("wms", "test", jpnDetails, "jpn", false);

        ServiceComplete i = serviceBusiness.getServiceById(id, "FR");
        Assert.assertNotNull(i);
        Assert.assertEquals("test", i.getIdentifier());
        Assert.assertEquals("name", i.getTitle());

        // en is not defined so we get the default language
        i = serviceBusiness.getServiceById(id, "en");
        Assert.assertNotNull(i);
        Assert.assertEquals("test", i.getIdentifier());
        Assert.assertEquals("name", i.getTitle());

        i = serviceBusiness.getServiceById(id, "jpn");
        Assert.assertNotNull(i);
        Assert.assertEquals("test", i.getIdentifier());
        Assert.assertEquals("nameJPN", i.getTitle());

    }

}
