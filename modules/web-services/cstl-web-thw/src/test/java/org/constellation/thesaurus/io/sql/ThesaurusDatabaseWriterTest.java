/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.thesaurus.io.sql;

import java.util.Arrays;
import javax.sql.DataSource;
import org.constellation.dto.thesaurus.FullConcept;
import org.constellation.test.SpringContextTest;
import org.constellation.thesaurus.api.IThesaurusBusiness;
import org.constellation.util.SQLUtilities;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusDatabaseWriterTest extends SpringContextTest {

    @Autowired
    private IThesaurusBusiness thesaurusBusiness;

    @Test
    public void testWriteConcept() throws Exception {

        final String url = "jdbc:derby:memory:thtest";
        final DataSource ds = SQLUtilities.getDataSource(url + ";create=true");

        ThesaurusDatabaseWriter thw = new ThesaurusDatabaseWriter(ds, "th", "derby", "urn:thesau:test", "thesaurus test", null, Arrays.asList(ISOLanguageCode.ENG), ISOLanguageCode.ENG);
        thw.store();

        Concept c = new Concept("c1");
        FullConcept fc = new FullConcept("c1");

        thw.writeConcept(c);
        FullConcept fcResult = thw.getFullConcept("c1");

        Assert.assertEquals(fc, fcResult);
        // TODO use thesaurusBusiness when HSQL will be detected and supported
        // WriteableThesaurus th = thesaurusBusiness.createNewThesaurus(t);

    }
}
