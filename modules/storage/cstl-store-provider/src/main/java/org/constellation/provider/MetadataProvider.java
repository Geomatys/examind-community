/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.provider;

import java.net.URI;
import java.util.Map;
import org.constellation.dto.service.config.csw.MetadataProviderCapabilities;
import org.constellation.exception.ConstellationStoreException;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface MetadataProvider extends DataProvider {

    MetadataProviderCapabilities getCapabilities()  throws ConstellationStoreException;

    Map<String, URI> getConceptMap();

    boolean storeMetadata(final Node obj) throws ConstellationStoreException;

    boolean replaceMetadata(String metadataID, Node any) throws ConstellationStoreException;

    boolean updateMetadata(String metadataID, Map<String, Object> properties) throws ConstellationStoreException;

    boolean deleteMetadata(final String metadataID) throws ConstellationStoreException;
}
