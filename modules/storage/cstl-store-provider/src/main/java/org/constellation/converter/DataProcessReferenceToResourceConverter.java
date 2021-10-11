package org.constellation.converter;

import org.apache.sis.storage.Resource;
import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.feature.util.converter.SimpleConverter;

public class DataProcessReferenceToResourceConverter extends SimpleConverter<DataProcessReference, Resource> {

    @Override
    public Class<DataProcessReference> getSourceClass() {
        return DataProcessReference.class;
    }

    @Override
    public Class<Resource> getTargetClass() {
        return Resource.class;
    }

    /**
     * Return Resource from a DataProcessReference.
     * @param ref DataProcessReference.
     * @return Resource.
     * @throws UnconvertibleObjectException if getProvider() or findResource() fails.
     */
    @Override
    public Resource apply(DataProcessReference ref) throws UnconvertibleObjectException {
        final Resource source;
        try {
            DataProvider dp = DataProviders.getProvider(ref.getProvider());
            Data d = dp.get(ref.getNamespace(), ref.getName());
            if (d != null) {
                return d.getOrigin();
            }
            throw new UnconvertibleObjectException("Unabel to find a data correspounding to the reference");
        } catch (ConstellationException e) {
            throw new UnconvertibleObjectException(e);
        }
    }
}
