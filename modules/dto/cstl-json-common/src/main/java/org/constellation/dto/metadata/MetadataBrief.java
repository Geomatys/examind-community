package org.constellation.dto.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents a summary of metadata used in Metadata dashboard page.
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class MetadataBrief extends Metadata implements Serializable {

    private User user;
    private String parentFileIdentifier;
    private List<String> keywords = new ArrayList<>();
    private List<MetadataBrief> linkedMetadata = new ArrayList<>();

    public MetadataBrief() {

    }

    public MetadataBrief(final Metadata metadata) {
        super(metadata);
    }

    public String getFileIdentifier() {
        return getMetadataId();
    }

    public void setFileIdentifier(String fileIdentifier) {
        setMetadataId(fileIdentifier);
    }

    @Override
    public String getType() {
        return getProfile();
    }

    @Override
    public void setType(String type) {
        setProfile(type);
    }

    public Long getUpdateDate() {
        return getDatestamp();
    }

    public void setUpdateDate(Long updateDate) {
        setDatestamp(updateDate);
    }

    public Long getCreationDate() {
        return getDateCreation();
    }

    public void setCreationDate(Long creationDate) {
        setDateCreation(creationDate);
    }

     public String getLevelCompletion() {
        return getLevel();
    }

    public void setLevelCompletion(String levelCompletion) {
        setLevel(levelCompletion);
    }

    public String getDocType() {
        return super.getType();
    }

    public void setDocType(String docType) {
        super.setType(docType);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<MetadataBrief> getLinkedMetadata() {
        return linkedMetadata;
    }

    public void setLinkedMetadata(List<MetadataBrief> linkedMetadata) {
        this.linkedMetadata = linkedMetadata;
    }

    public String getParentFileIdentifier() {
        return parentFileIdentifier;
    }

    public void setParentFileIdentifier(String parentFileIdentifier) {
        this.parentFileIdentifier = parentFileIdentifier;
    }
}
