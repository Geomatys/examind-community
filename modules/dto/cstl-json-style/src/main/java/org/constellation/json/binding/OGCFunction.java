/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

package org.constellation.json.binding;

import java.util.ArrayList;
import java.util.List;
import static org.constellation.json.util.StyleFactories.FF;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OGCFunction implements StyleElement<Expression> {

    private String name;
    private String propertyName;
    private List<String> literals;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the propertyName
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return the literals
     */
    public List<String> getLiterals() {
        return literals;
    }

    /**
     * @param literals the literals to set
     */
    public void setLiterals(List<String> literals) {
        this.literals = literals;
    }

    @Override
    public Expression toType() {
        final List<Expression> exps = new ArrayList<>();
        exps.add(FF.property(propertyName));
        for (String lit : literals) {
            exps.add(FF.literal(lit));
        }
        return FF.function(name, exps.toArray(new Expression[exps.size()]));
    }

}
