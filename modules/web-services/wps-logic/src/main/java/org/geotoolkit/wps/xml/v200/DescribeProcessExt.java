/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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
package org.geotoolkit.wps.xml.v200;

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.ows.xml.v200.CodeType;

/**
 *
 * @author glegal
 */
public class DescribeProcessExt extends DescribeProcess {
    
    private List<ProcessInput> input;
    
    public DescribeProcessExt() {}

    public DescribeProcessExt(String service, String version, String language, List<CodeType> identifiers, List<ProcessInput> input) {
        super(service, version, language, identifiers);
        this.input = input;
    }

    public List<ProcessInput> getInput() {
        if (input == null) {
            input = new ArrayList<>();
        }
        return input;
    }

    public void setInput(List<ProcessInput> input) {
        this.input = input;
    }
}
