/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.json.metadata.bean;

import java.util.Arrays;
import java.util.UUID;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.xml.IdentifiedObject;
import org.apache.sis.xml.IdentifierSpace;
import org.constellation.json.metadata.Template;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbractContactTemplate extends Template {

    public AbractContactTemplate(String identifier, String resourcePath) {
        super(identifier, MetadataStandard.ISO_19115, resourcePath, Arrays.asList("contact"), false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object emptyMetadata() {
        return new DefaultResponsibleParty();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getMetadataIdentifier(Object metadata) {
        if (metadata instanceof IdentifiedObject) {
            UUID identifier = ((IdentifiedObject)metadata).getIdentifierMap().getSpecialized(IdentifierSpace.UUID);
            if (identifier != null) {
                return identifier.toString();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setMetadataIdentifier(String identifier, Object metadata) {
        if (metadata instanceof IdentifiedObject) {
            ((IdentifiedObject)metadata).getIdentifierMap().putSpecialized(IdentifierSpace.UUID, UUID.fromString(identifier));
        }
    }
}
