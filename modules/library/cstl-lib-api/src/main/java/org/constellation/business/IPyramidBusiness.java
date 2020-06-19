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
package org.constellation.business;

import java.util.List;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.ProviderData;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IPyramidBusiness {

    ProviderData pyramidMapContext(Integer userId, String pyramidDataName, final String crs, final MapContextLayersDTO mc) throws ConstellationException;

    ProviderData pyramidDatas(Integer userId, String pyramidDataName, List<Integer> dataIds, final String crs) throws ConstellationException;
}
