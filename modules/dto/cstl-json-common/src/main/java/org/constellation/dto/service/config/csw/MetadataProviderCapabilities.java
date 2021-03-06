/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.dto.service.config.csw;

import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataProviderCapabilities {

    public final List<QName> additionalQueryable;

    public final List<String> acceptedResourceType;

    public final List<QName> supportedTypeNames;

    public final boolean writeSupported;

    public final boolean deleteSupported;

    public final boolean updateSupported;

    public MetadataProviderCapabilities(List<QName> additionalQueryable, List<String> acceptedResourceType,
            List<QName> supportedTypeNames, boolean writeSupported, boolean deleteSupported, boolean updateSupported) {
        this.additionalQueryable = additionalQueryable;
        this.acceptedResourceType = acceptedResourceType;
        this.supportedTypeNames = supportedTypeNames;
        this.writeSupported = writeSupported;
        this.deleteSupported = deleteSupported;
        this.updateSupported = updateSupported;
    }
}
