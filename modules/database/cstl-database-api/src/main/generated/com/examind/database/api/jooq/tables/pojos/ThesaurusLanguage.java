/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.thesaurus_language
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusLanguage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer thesaurusId;
    private String language;

    public ThesaurusLanguage() {}

    public ThesaurusLanguage(ThesaurusLanguage value) {
        this.thesaurusId = value.thesaurusId;
        this.language = value.language;
    }

    public ThesaurusLanguage(
        Integer thesaurusId,
        String language
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ThesaurusLanguage other = (ThesaurusLanguage) obj;
        if (this.thesaurusId == null) {
            if (other.thesaurusId != null)
                return false;
        }
        else if (!this.thesaurusId.equals(other.thesaurusId))
            return false;
        if (this.language == null) {
            if (other.language != null)
                return false;
        }
        else if (!this.language.equals(other.language))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.thesaurusId == null) ? 0 : this.thesaurusId.hashCode());
        result = prime * result + ((this.language == null) ? 0 : this.language.hashCode());
        return result;
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
