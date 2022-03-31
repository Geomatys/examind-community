/*
 *    Examind community - An open source and standard compliant SDI
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
package org.constellation.converter;

import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.ProviderConstants.INTERNAL_MAP_CONTEXT_PROVIDER;
import org.constellation.business.IMapContextBusiness;
import org.constellation.dto.process.MapContextProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.feature.util.converter.SimpleConverter;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextReferenceToMapLayersConverter extends SimpleConverter<MapContextProcessReference, MapLayers> {

    @Override
    public Class<MapContextProcessReference> getSourceClass() {
        return MapContextProcessReference.class;
    }

    @Override
    public Class<MapLayers> getTargetClass() {
        return MapLayers.class;
    }

    /**
     * Return MapLayers from a MapContextProcessReference.
     *
     * @param ref MapContextProcessReference.
     * @return MapLayers.
     * @throws UnconvertibleObjectException if getProvider() or findResource() fails.
     */
    @Override
    public MapLayers apply(MapContextProcessReference ref) throws UnconvertibleObjectException {
        if (ref == null) return null;
        try {
            return getByMpId(ref);
        } catch (ConstellationException e) {
            // Maybe input reference does not give map context id. We'll try with provider information instead
            try {
                return findData(INTERNAL_MAP_CONTEXT_PROVIDER, null, ref.getName());
            } catch (ConstellationException bis) {
                e.addSuppressed(bis);
                throw new UnconvertibleObjectException("Cannot find a map context for given information", e);
            }
        }
    }

    private static MapLayers getByMpId(final MapContextProcessReference ref) throws ConstellationException {
        final IMapContextBusiness mpBiz = SpringHelper.getBean(IMapContextBusiness.class);
        if (mpBiz == null) throw new UnconvertibleObjectException("Application context unavailable");
        final org.constellation.dto.Data data = mpBiz.getMapContextData(ref.getId());
        return findData(data.getProviderId(), data.getNamespace(), data.getName());
    }

    private static MapLayers findData(String providerId, String namespace, String name) throws ConstellationException {
        DataProvider dp = DataProviders.getProvider(providerId);
        return findData(dp, namespace, name);
    }

    private static MapLayers findData(int providerId, String namespace, String name) throws ConstellationException {
        DataProvider dp = DataProviders.getProvider(providerId);
        return findData(dp, namespace, name);
    }

    private static MapLayers findData(DataProvider dp, String namespace, String name) throws ConstellationException {
        Data d = dp.get(namespace, name);
        if (d == null) throw new TargetNotFoundException(String.format("No map context data found in provider %s for name %s:%s", dp.getId(), namespace, name));
        MapItem mi = d.getMapLayer(null);
        if (mi instanceof MapLayers) {
            return (MapLayers)mi;
        }
        throw new UnconvertibleObjectException("Map context data resource is not a SIS mapLayers");
    }
}
