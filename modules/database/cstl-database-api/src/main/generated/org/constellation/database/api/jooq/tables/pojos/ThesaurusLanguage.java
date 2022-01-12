/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.thesaurus_language
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusLanguage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer thesaurusId;
    private String  language;

    public ThesaurusLanguage() {}

    public ThesaurusLanguage(ThesaurusLanguage value) {
        this.thesaurusId = value.thesaurusId;
        this.language = value.language;
    }

    public ThesaurusLanguage(
        Integer thesaurusId,
        String  language
    ) {
        this.thesaurusId = thesaurusId;
        this.language = language;
    }

    /**
     * Getter for <code>admin.thesaurus_language.thesaurus_id</code>.
     */
    @NotNull
    public Integer getThesaurusId() {
        return this.thesaurusId;
    }

    /**
     * Setter for <code>admin.thesaurus_language.thesaurus_id</code>.
     */
    public ThesaurusLanguage setThesaurusId(Integer thesaurusId) {
        this.thesaurusId = thesaurusId;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_language.language</code>.
     */
    @NotNull
    @Size(max = 3)
    public String getLanguage() {
        return this.language;
    }

    /**
     * Setter for <code>admin.thesaurus_language.language</code>.
     */
    public ThesaurusLanguage setLanguage(String language) {
        this.language = language;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ThesaurusLanguage (");

        sb.append(thesaurusId);
        sb.append(", ").append(language);

        sb.append(")");
        return sb.toString();
    }
}
