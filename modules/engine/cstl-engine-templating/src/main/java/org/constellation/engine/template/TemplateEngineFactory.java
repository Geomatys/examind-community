/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

package org.constellation.engine.template;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.constellation.util.Util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.Charset;

/** Factory for TemplateEngine
 * Created by christophem on 02/04/14.
 */
public class TemplateEngineFactory {
    public static final String GROOVY_TEMPLATE_ENGINE = "groovy";
    private static final String GROOVY_TEMPLATE_PACKAGE = "org/constellation/engine/template/";
    public static final String GROOVY_TEMPLATE_FILENAME = "TemplateEngine.groovy";

    public static TemplateEngine getInstance(String templateEngineType) throws TemplateEngineException {
        try {
            switch (templateEngineType) {
                case GROOVY_TEMPLATE_ENGINE :

                    try (final GroovyClassLoader gcl = new GroovyClassLoader();
                         final InputStream stream = Util.getResourceAsStream(GROOVY_TEMPLATE_PACKAGE + GROOVY_TEMPLATE_FILENAME);
                         final InputStreamReader rawReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                         final BufferedReader reader = new BufferedReader(rawReader)) {

                        final Class<?> clazz = gcl.parseClass(new GroovyCodeSource(reader, GROOVY_TEMPLATE_FILENAME, ""));
                        final Object aScript = clazz.getDeclaredConstructor().newInstance();
                        return (TemplateEngine) aScript;
                    }

                default:
                    throw new IllegalArgumentException( "templateEngineType "+ templateEngineType + " undefined." );
            }
        } catch (Exception e){
            throw new TemplateEngineException("unable to load template engine",e);
        }
    }
}
