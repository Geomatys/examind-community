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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.constellation.dto.Data;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class FullConcept extends ConceptBrief {

    private Map<String, String> definition = new HashMap<>();

    private List<ConceptBrief> narrowers = new ArrayList<>();

    private List<ConceptBrief> broaders = new ArrayList<>();

    private List<ConceptBrief> related = new ArrayList<>();

    private boolean topConcept = false;

    
    public FullConcept() {
    }

    public FullConcept(String uri) {
        super(uri);
    }

    public Map<String, String> getDefinition() {
        return definition;
    }

    public FullConcept setDefinition(Map<String, String> definition) {
        this.definition = definition;
        return this;
    }

    public List<ConceptBrief> getNarrowers() {
        return narrowers;
    }

    public FullConcept setNarrowers(List<ConceptBrief> narrowers) {
        this.narrowers = narrowers;
        return this;
    }

    public List<ConceptBrief> getBroaders() {
        return broaders;
    }

    public FullConcept setBroaders(List<ConceptBrief> broaders) {
        this.broaders = broaders;
        return this;
    }

    public List<ConceptBrief> getRelated() {
        return related;
    }

    public FullConcept setRelated(List<ConceptBrief> related) {
        this.related = related;
        return this;
    }

    public boolean isTopConcept() {
        return topConcept;
    }

    public FullConcept setTopConcept(boolean topConcept) {
        this.topConcept = topConcept;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri(), getAltLabels(), getPrefLabel(), broaders, definition, narrowers, related, topConcept);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof FullConcept that && super.equals(obj)) {
            return     Objects.equals(this.broaders,   that.broaders)
                    && Objects.equals(this.definition, that.definition)
                    && Objects.equals(this.narrowers,  that.narrowers)
                    && Objects.equals(this.related,    that.related)
                    && Objects.equals(this.topConcept, that.topConcept);
        }
        return false;
    }
}
