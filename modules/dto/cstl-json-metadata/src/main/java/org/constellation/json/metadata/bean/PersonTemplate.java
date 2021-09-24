/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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

import java.util.UUID;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.xml.IdentifierSpace;
import org.constellation.json.metadata.Template;
import org.opengis.metadata.citation.ResponsibleParty;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("profile_contact_person")
public class PersonTemplate extends Template {

    public PersonTemplate() {
        super(MetadataStandard.ISO_19115, "org/constellation/json/metadata/profile_contact_person.json");
    }

    @Override
    public String getIdentifier() {
        return "profile_contact_person";
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean matchMetadata(Object metadata) {
        if (metadata instanceof ResponsibleParty) {
            ResponsibleParty rp = (ResponsibleParty) metadata;
            return rp.getIndividualName() != null;
        }
        return false;
    }

    @Override
    public boolean matchDataType(String dataType) {
        return "contact".equalsIgnoreCase(dataType);
    }

    @Override
    public Object emptyMetadata() {
        return new DefaultResponsibleParty();
    }

    @Override
    public String getMetadataIdentifier(Object metadata) {
        if (metadata instanceof ResponsibleParty) {
            UUID identifier = ((DefaultResponsibleParty)metadata).getIdentifierMap().getSpecialized(IdentifierSpace.UUID);
            if (identifier != null) {
                return identifier.toString();
            }
        }
        return null;
    }

    @Override
    public void setMetadataIdentifier(String identifier, Object metadata) {
        if (metadata instanceof DefaultResponsibleParty) {
            ((DefaultResponsibleParty)metadata).getIdentifierMap().putSpecialized(IdentifierSpace.UUID, UUID.fromString(identifier));
        }
    }

    @Override
    public String getMetadataTitle(Object metadata) {
        if (metadata instanceof ResponsibleParty) {
            return ((ResponsibleParty)metadata).getIndividualName();
        }
        return null;
    }

    @Override
    public void setMetadataTitle(String title, Object metadata) {
        if (metadata instanceof DefaultResponsibleParty) {
            ((DefaultResponsibleParty)metadata).setIndividualName(title);
        }
    }
}
