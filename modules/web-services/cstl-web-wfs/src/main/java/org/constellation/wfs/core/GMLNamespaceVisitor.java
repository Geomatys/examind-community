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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.constellation.wfs.core;

import java.util.function.Function;
import org.apache.sis.filter.privy.FunctionNames;
import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.ValueReference;

/**
 * temporary hack for GML 3.2
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GMLNamespaceVisitor extends DuplicatingFilterVisitor{
    public GMLNamespaceVisitor() {
        final Function previous = getExpressionHandler(FunctionNames.ValueReference);
        setExpressionHandler(FunctionNames.ValueReference, (e) -> {
            final ValueReference expression = (ValueReference) e;
            if (expression.getXPath().indexOf("http://www.opengis.net/gml/3.2")   != -1 ||
                expression.getXPath().indexOf("http://www.opengis.net/gml/3.2.1") != -1) {
                String newPropertyName = expression.getXPath().replace("http://www.opengis.net/gml/3.2", "http://www.opengis.net/gml");
                newPropertyName = newPropertyName.replace("http://www.opengis.net/gml/3.2.1", "http://www.opengis.net/gml");
                return ff.property(newPropertyName);
            }
            return previous.apply(expression);
        });
    }
}
