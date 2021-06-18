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
import org.opengis.filter.ValueReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.apache.sis.internal.filter.FunctionNames;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AliasFilterVisitor extends DuplicatingFilterVisitor {

    public AliasFilterVisitor(final Map<String, GenericName> aliases) {
        final Map<String, GenericName> fa;
        if (aliases != null) {
            fa = aliases;
        } else {
            fa = new HashMap<>();
        }
        final Function previous = getExpressionHandler(FunctionNames.ValueReference);
        setExpressionHandler(FunctionNames.ValueReference, (e) -> {
            final ValueReference expression = (ValueReference) e;
            for (Entry<String, GenericName> entry : fa.entrySet()) {
                if (expression.getXPath().startsWith(entry.getKey() + "/")) {
                    String nmsp = NamesExt.getNamespace(entry.getValue());
                    if (nmsp == null) nmsp = "";
                    final String newPropertyName = '{' + nmsp + '}' + entry.getValue().tip().toString() + expression.getXPath().substring(entry.getKey().length());
                    return ff.property(newPropertyName);
                }
            }
            return previous.apply(expression);
        });
    }
}
