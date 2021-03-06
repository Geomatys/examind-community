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
package org.constellation.converter;

import org.apache.sis.util.UnconvertibleObjectException;
import org.constellation.util.DataReference;
import org.opengis.style.Description;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Style;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

import java.util.List;
import java.util.logging.Level;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.util.Util;
import org.opengis.util.GenericName;


/**
 *
 * @author Cédric Briançon (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public final class DataReferenceConverter {

    /**
     * Prevents instantiation.
     */
    private DataReferenceConverter() {}

    public static Style convertDataReferenceToStyle(final DataReference source) throws UnconvertibleObjectException {
        if (source == null) {
            throw new UnconvertibleObjectException("Null data reference given.");
        }

        final String dataType = source.getDataType();

        Style style = null;
        final GenericName layerName = Util.getLayerId(source);

        /*
         * Search in Provider layers
         */
        if (dataType.equals(DataReference.PROVIDER_STYLE_TYPE)) {
            final String providerID = source.getProviderOrServiceId();


            //find provider
            final IStyleBusiness styleBusiness = SpringHelper.getBean(IStyleBusiness.class);
            try {
                style = styleBusiness.getStyle(providerID, layerName.toString());
            } catch (TargetNotFoundException ex) {
                Logging.getLogger("org.constellation").log(Level.WARNING, null, ex);
            }

            if (style == null) {
                throw new UnconvertibleObjectException("Layer name " + layerName + " not found.");
            }
        } else {
            throw new UnconvertibleObjectException("Layer provider and service are not supported.");
        }

        return new ReferenceStyleWrapper(style, source.getReference());
    }


    /**
     * Private internal class that wrap a Style into another with a specified identifier.
     */
    private static class ReferenceStyleWrapper implements Style {

        private final Style style;
        private final String name;

        public ReferenceStyleWrapper(final Style style, final String name) {
            this.style = style;
            this.name = name;
        }


        @Override
        public String getName() {
            return name;
        }

        @Override
        public Description getDescription() {
            return style.getDescription();
        }

        @Override
        public boolean isDefault() {
            return style.isDefault();
        }

        @Override
        public List<? extends FeatureTypeStyle> featureTypeStyles() {
             return style.featureTypeStyles();
        }

        @Override
        public Symbolizer getDefaultSpecification() {
             return style.getDefaultSpecification();
        }

        @Override
        public Object accept(final StyleVisitor visitor, final Object extraData) {
             return style.accept(visitor, extraData);
        }

    }
}
