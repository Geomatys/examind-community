package org.constellation.dto.metadata;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataWithState extends Metadata {

    private boolean previousPublishState;

    private boolean previousHiddenState;

    public MetadataWithState() {
    }

    public MetadataWithState(Metadata metadata, boolean previousPublishState, boolean previousHiddenState) {
        super(metadata);
        this.previousPublishState = previousPublishState;
        this.previousHiddenState = previousHiddenState;
    }

    /**
     * @return the previousPublishState
     */
    public boolean isPreviousPublishState() {
        return previousPublishState;
    }

    /**
     * @param previousPublishState the previousPublishState to set
     */
    public void setPreviousPublishState(boolean previousPublishState) {
        this.previousPublishState = previousPublishState;
    }

    /**
     * @return the previousHiddenState
     */
    public boolean isPreviousHiddenState() {
        return previousHiddenState;
    }

    /**
     * @param previousHiddenState the previousHiddenState to set
     */
    public void setPreviousHiddenState(boolean previousHiddenState) {
        this.previousHiddenState = previousHiddenState;
    }

}
