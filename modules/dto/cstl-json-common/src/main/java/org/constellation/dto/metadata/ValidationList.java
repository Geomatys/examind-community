package org.constellation.dto.metadata;

import java.io.Serializable;
import java.util.List;

/**
 * This is a simple pojo for json transfer to validate/discard a list of metadata.
 * in case of discarding validation a comment can be filled.
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class ValidationList implements Serializable {

    private List<Integer> metadataIdsList;

    private String comment;

    public ValidationList() {}

    public List<Integer> getMetadataList() {
        return metadataIdsList;
    }

    public void setMetadataList(List<Integer> metadataIdsList) {
        this.metadataIdsList = metadataIdsList;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
