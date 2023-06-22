/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
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
package com.examind.setup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.sis.io.wkt.WKTDictionary;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CRSExtensionFactory extends WKTDictionary implements CRSAuthorityFactory {

    public CRSExtensionFactory() throws IOException, FactoryException {
        super(new DefaultCitation("ESRI"));

        try (InputStream stream = CRSExtensionFactory.class.getResourceAsStream("ESRI.txt");
            BufferedReader source = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            load(source);
        }
    }

}
