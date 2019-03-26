package org.constellation.dto.thesaurus;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Fabien Bernard (Geomatys).
 */
public class ThesaurusBrief implements Serializable {

    private String uri;

    private String name;

    private Date creationDate;

    private String defaultLang;


    public String getUri() {
        return uri;
    }

    public ThesaurusBrief setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public ThesaurusBrief setName(String name) {
        this.name = name;
        return this;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public ThesaurusBrief setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public ThesaurusBrief setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
        return this;
    }
}
