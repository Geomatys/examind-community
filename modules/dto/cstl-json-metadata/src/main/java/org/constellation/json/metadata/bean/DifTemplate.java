/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

import org.apache.sis.metadata.MetadataStandard;
import org.constellation.json.metadata.Template;
import org.geotoolkit.dif.xml.v102.DIF;
import org.geotoolkit.dif.xml.v102.EntryIDType;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("profile_dif")
public class DifTemplate extends Template {

    public DifTemplate() {
        super(MetadataStandard.ISO_19115, "org/constellation/json/metadata/profile_dif.json");
    }

    @Override
    public String getIdentifier() {
        return "profile_dif";
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean matchMetadata(Object metadata) {
        return metadata instanceof DIF;
    }

    @Override
    public boolean matchDataType(String dataType) {
        return false;
    }

    @Override
    public Object emptyMetadata() {
        return new DIF();
    }

    @Override
    public String getMetadataIdentifier(Object metadata) {
        if (metadata instanceof DIF) {
            DIF d = ((DIF)metadata);
            if (d.getEntryID() != null) {
                return d.getEntryID().getShortName();
            }
        }
        return super.getMetadataIdentifier(metadata);
    }

    /**
     * Set the identifier for the metadata Object.
     *
     * @param identifier The identifier to set.
     * @param metadata A metadata Object.
     *
     */
    @Override
    public void setMetadataIdentifier(final String identifier, final Object metadata) {
        if (metadata instanceof DIF) {
            DIF d = ((DIF)metadata);
            if (d.getEntryID() != null) {
                d.getEntryID().setShortName(identifier);
            } else {
                EntryIDType eid = new EntryIDType();
                eid.setShortName(identifier);
                d.setEntryID(eid);
            }
        } else {
            super.setMetadataTitle(identifier, metadata);
        }
    }

    @Override
    public String getMetadataTitle(Object metadata) {
        if (metadata instanceof DIF) {
            DIF d = ((DIF)metadata);
            return d.getEntryTitle();
        }
        return super.getMetadataTitle(metadata);
    }

    /**
     * Set the title for the metadata Object.
     *
     * @param title The title to set.
     * @param metadata A metadata Object.
     *
     */
    @Override
    public void setMetadataTitle(final String title, final Object metadata) {
        if (metadata instanceof DIF) {
             DIF d = ((DIF)metadata);
             d.setEntryTitle(title);
        } else {
            super.setMetadataTitle(title, metadata);
        }
    }

}
