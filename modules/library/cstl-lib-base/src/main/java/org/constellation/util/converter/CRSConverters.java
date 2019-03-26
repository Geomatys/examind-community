package org.constellation.util.converter;

import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.dto.process.CRSProcessReference;
import java.util.Map;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.feature.util.converter.SimpleConverter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * @author Alexis Manin (Geomatys).
 */
public class CRSConverters {

    public static class String2CRSProcessReference extends SimpleConverter<String, CRSProcessReference> {

        @Override
        public Class<String> getSourceClass() {
            return String.class;
        }

        @Override
        public Class<CRSProcessReference> getTargetClass() {
            return CRSProcessReference.class;
        }

        @Override
        public CRSProcessReference apply(String object) throws UnconvertibleObjectException {
            if (object == null || object.isEmpty()) {
                return null;
            }

            CRSProcessReference ref = new CRSProcessReference();
            ref.setCode(object);

            return ref;
        }
    }

    public static class String2CoordinateReferenceSystem extends SimpleConverter<String, CoordinateReferenceSystem> {

        @Override
        public Class<String> getSourceClass() {
            return String.class;
        }

        @Override
        public Class<CoordinateReferenceSystem> getTargetClass() {
            return CoordinateReferenceSystem.class;
        }

        @Override
        public CoordinateReferenceSystem apply(String object) throws UnconvertibleObjectException {
            if (object == null || object.isEmpty()) {
                return null;
            }

            try {
                return CRS.forCode(object);
            } catch (FactoryException ex) {
                throw new UnconvertibleObjectException("Problem while decoding coordinate reference system for identifier " + object, ex);
            }
        }
    }

    public static class Map2CoordinateReferenceSystem extends SimpleConverter<Map, CoordinateReferenceSystem> {

        @Override
        public Class<Map> getSourceClass() {
            return Map.class;
        }

        @Override
        public Class<CoordinateReferenceSystem> getTargetClass() {
            return CoordinateReferenceSystem.class;
        }

        @Override
        public CoordinateReferenceSystem apply(Map object) throws UnconvertibleObjectException {
            if (object == null || object.isEmpty()) {
                return null;
            }

            final Object code = object.get("code");
            if (code instanceof String) {

                try {
                    return CRS.forCode((String) code);
                } catch (FactoryException ex) {
                    throw new UnconvertibleObjectException("Problem while decoding coordinate reference system for identifier " + object, ex);
                }
            }

            throw new UnconvertibleObjectException("Given map does not contain any text value for code. Found: " + code);
        }
    }
}
