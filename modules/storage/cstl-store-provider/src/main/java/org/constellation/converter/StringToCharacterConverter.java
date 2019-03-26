/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

package org.constellation.converter;

import java.util.Collections;
import java.util.Set;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;

/**
 * String to Character converter.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StringToCharacterConverter implements ObjectConverter<String, Character> {

    public StringToCharacterConverter() {
    }

    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    @Override
    public Class<Character> getTargetClass() {
        return Character.class;
    }

    @Override
    public Set<FunctionProperty> properties() {
        return Collections.emptySet();
    }

    @Override
    public Character apply(String source) throws UnconvertibleObjectException {

        if (source != null && !source.isEmpty()) {
            return source.charAt(0);
        } else {
            throw new UnconvertibleObjectException("Source string can't be null or empty.");
        }
    }

    @Override
    public ObjectConverter<Character, String> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
