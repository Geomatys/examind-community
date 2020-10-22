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
package org.constellation.repository;

import java.util.List;

import org.constellation.dto.ProviderBrief;

public interface ProviderRepository extends AbstractRepository {

    List<ProviderBrief> findAll();

    ProviderBrief findOne(Integer id);

    ProviderBrief findForData(Integer dataId);

    List<ProviderBrief> findByImpl(String serviceName);

    List<String> getProviderIds();

    Integer findIdForIdentifier(String providerIdentifier);

    ProviderBrief findByIdentifier(String providerIdentifier);

    Integer create(ProviderBrief newProvider);

    int deleteByIdentifier(String providerID);

    int update(ProviderBrief provider);

    int removeLinkedServices(int providerID);

    List<Integer> getAllIds();
}
