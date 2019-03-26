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

import org.constellation.dto.Data;
import org.constellation.dto.Style;
import org.constellation.dto.ProviderBrief;

public interface ProviderRepository {

    public List<ProviderBrief> findAll();

    public ProviderBrief findOne(Integer id);

    boolean existById(Integer id);

    public ProviderBrief findForData(Integer dataId);

    public List<ProviderBrief> findByImpl(String serviceName);

    public List<String> getProviderIds();

    public Integer findIdForIdentifier(String providerIdentifier);

    public ProviderBrief findByIdentifier(String providerIdentifier);

    public ProviderBrief getProviderParentIdOfLayer(String serviceType, String serviceId, String layerid);

    public Integer create(ProviderBrief newProvider);

    public int delete(int id);

    public int deleteByIdentifier(String providerID);

    public List<ProviderBrief> findChildren(String id);

    public List<Data> findDatasByProviderId(Integer id);

    public List<Integer> findDataIdsByProviderId(Integer id);

    public List<Data> findDatasByProviderId(Integer id, String dataType, boolean included, boolean hidden);

    public List<Integer> findDataIdsByProviderId(Integer id, String dataType, boolean included, boolean hidden);

    public List<Data> findDatasByProviderId(Integer id, String dataType);

    public int update(ProviderBrief provider);

    public List<Style> findStylesByProviderId(Integer providerId);

    public int removeLinkedServices(int providerID);

    public List<Integer> getAllIds();

    public List<Integer> getAllIdsWithNoParent();

}
