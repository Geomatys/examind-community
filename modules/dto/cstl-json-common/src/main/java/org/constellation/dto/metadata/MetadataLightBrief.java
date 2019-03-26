package org.constellation.dto.metadata;

import java.io.Serializable;

/**
 * It is a very lightweight pojo that represents a metadata record,
 * used for selection of several rows in dashboard page
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class MetadataLightBrief implements Serializable {

    private int id;
    private String title;
    private String profile;

    public MetadataLightBrief() {

    }

    public MetadataLightBrief(int id, String title, String profile) {
        this.id = id;
        this.title = title;
        this.profile = profile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
