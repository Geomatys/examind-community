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

package org.constellation.wfs.core;

import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AliasFilterVisitor extends DuplicatingFilterVisitor {

    private final Map<String, GenericName> aliases;

    public AliasFilterVisitor(final Map<String, GenericName> aliases) {
        if (aliases != null) {
            this.aliases = aliases;
        } else {
            this.aliases = new HashMap<>();
        }
    }

    @Override
    public Object visit(final PropertyName expression, final Object extraData) {
        for (Entry<String, GenericName> entry : aliases.entrySet()) {
            if (expression.getPropertyName().startsWith(entry.getKey() + "/")) {
                String nmsp = NamesExt.getNamespace(entry.getValue());
                if (nmsp == null) nmsp = "";
                final String newPropertyName = '{' + nmsp + '}' + entry.getValue().tip().toString() + expression.getPropertyName().substring(entry.getKey().length());
                return getFactory(extraData).property(newPropertyName);
            }
        }
        return super.visit(expression, extraData);
    }
}
