/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.admin;

import java.util.List;
import org.constellation.business.IInternalMetadataBusiness;
import org.constellation.dto.metadata.InternalMetadata;
import org.constellation.dto.metadata.Metadata;
import org.constellation.repository.InternalMetadataRepository;
import org.constellation.repository.MetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
@Component
public class InternalMetadataBusiness implements IInternalMetadataBusiness{

    /**
     * Injected data repository.
     */
    @Autowired
    protected InternalMetadataRepository intMetadataRepository;

    @Autowired
    protected MetadataRepository metadataRepository;

    @Override
    public String getMetadata(String metadataID) {
        final InternalMetadata meta = intMetadataRepository.findByMetadataId(metadataID);
        if (meta != null) {
            return meta.getMetadataIso();
        }
        return null;
    }

    @Override
    @Transactional
    public void updateMetadata(String metadataID, String newIdentifier, String metadataXML) {
        InternalMetadata metadata  = intMetadataRepository.findByMetadataId(metadataID);
        if (metadata == null) {
            metadata = new InternalMetadata();
            metadata.setMetadataIso(metadataXML);
            metadata.setMetadataId(metadataID);
            final Metadata meta = metadataRepository.findByMetadataId(newIdentifier);
            metadata.setId(meta.getId());
            intMetadataRepository.create(metadata);
        } else {
            metadata.setMetadataId(newIdentifier);
            metadata.setMetadataIso(metadataXML);
            intMetadataRepository.update(metadata);
        }
    }

    @Override
    public boolean existMetadata(String metadataID) {
        return intMetadataRepository.findByMetadataId(metadataID) != null;
    }

    @Override
    public List<String> getInternalMetadataIds() {
        return intMetadataRepository.getMetadataIds();
    }

    @Override
    public int getInternalMetadataCount() {
        return intMetadataRepository.countMetadata();
    }

    @Override
    public boolean deleteMetadata(String metadataID) {
        return intMetadataRepository.delete(metadataID) > 0;
    }
}
