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
package org.constellation.dto.thesaurus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fabien Bernard (Geomatys).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptBrief implements Serializable {

    private String uri;

    private Map<String, String> prefLabel = new HashMap<>();

    private Map<String, String[]> altLabels = new HashMap<>();


    public ConceptBrief() {
    }

    public ConceptBrief(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public ConceptBrief setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public ConceptBrief setPrefLabel(Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
        return this;
    }

    public Map<String, String[]> getAltLabels() {
        return altLabels;
    }

    public ConceptBrief setAltLabels(Map<String, String[]> altLabels) {
        this.altLabels = altLabels;
        return this;
    }

    public void addAltLabel(String lang, String value) {
        String[] values = altLabels.get(lang);
        if (values != null) {
            final String[] array = new String[values.length+1];
            array[array.length-1] = value;
            values = array;
        } else {
            values = new String[]{value};
        }
        altLabels.put(lang, values);
    }
}
