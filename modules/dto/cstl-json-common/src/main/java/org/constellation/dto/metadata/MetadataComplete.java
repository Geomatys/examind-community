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
package org.constellation.dto.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class MetadataComplete extends Metadata implements Serializable {

    private List<MetadataBbox> bboxes = new ArrayList<>();

    public MetadataComplete() {
        super(Integer.SIZE, null, null, null, null,
              null, null, null, null, null, null,
              null, Boolean.TRUE, Boolean.TRUE, null, null, "NONE", null, null, null, null, null, Boolean.FALSE, Boolean.FALSE);
    }

    public MetadataComplete(Metadata metadata, List<MetadataBbox> bboxes) {
        super(metadata.getId(),
              metadata.getMetadataId(),
              metadata.getDataId(),
              metadata.getDatasetId(),
              metadata.getServiceId(),
              metadata.getMdCompletion(),
              metadata.getOwner(),
              metadata.getDatestamp(),
              metadata.getDateCreation(),
              metadata.getTitle(),
              metadata.getProfile(),
              metadata.getParentIdentifier(),
              metadata.getIsValidated(),
              metadata.getIsPublished(),
              metadata.getLevel(),
              metadata.getResume(),
              metadata.getValidationRequired(),
              metadata.getValidatedState(),
              metadata.getComment(),
              metadata.getProviderId(),
              metadata.getMapContextId(),
              metadata.getType(),
              metadata.getIsShared(),
              metadata.getIsHidden());
        this.bboxes = bboxes;
    }

    /**
     * @return the bboxes
     */
    public List<MetadataBbox> getBboxes() {
        return bboxes;
    }

    /**
     * @param bboxes the bboxes to set
     */
    public void setBboxes(List<MetadataBbox> bboxes) {
        this.bboxes = bboxes;
    }

}
