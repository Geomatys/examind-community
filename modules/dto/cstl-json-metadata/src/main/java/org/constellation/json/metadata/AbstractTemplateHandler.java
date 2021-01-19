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
package org.constellation.json.metadata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.ValueExistencePolicy;
import org.apache.sis.util.logging.Logging;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DomainConsistency;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.VectorSpatialRepresentation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AbstractTemplateHandler {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.json.metadata");
    /**
     * The metadata standard.
     */
    protected final MetadataStandard standard;

    /**
     * The object to use for parsing dates of the form "2014-09-11".
     * Usage of this format shall be synchronized on {@code DATE_FORMAT}.
     */
    protected final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    protected final DateFormat dateHourFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * The default value to give to the {@code specialized} of {@link TemplateReader} constructor.
     */
    public static final Map<Class<?>, Class<?>> DEFAULT_SPECIALIZED;
    static {
        final Map<Class<?>, Class<?>> specialized = new HashMap<>();
        specialized.put(Responsibility.class,        ResponsibleParty.class);
        specialized.put(Identification.class,        DataIdentification.class);
        specialized.put(GeographicExtent.class,      GeographicBoundingBox.class);
        specialized.put(SpatialRepresentation.class, VectorSpatialRepresentation.class);
        specialized.put(Constraints.class,           LegalConstraints.class);
        specialized.put(Result.class,                ConformanceResult.class);
        specialized.put(Element.class,               DomainConsistency.class);
        DEFAULT_SPECIALIZED = specialized;
    }

    protected Map<Class<?>, Class<?>> specialized;

    public AbstractTemplateHandler(final MetadataStandard standard) {
        this(standard, DEFAULT_SPECIALIZED);
    }

    public AbstractTemplateHandler(final MetadataStandard standard, Map<Class<?>, Class<?>> specialized) {
        this.standard = standard;
        this.specialized = specialized;
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateHourFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    protected Map<String,Object> asMap(final Object metadata) {
        return standard.asValueMap(metadata, null, KeyNamePolicy.UML_IDENTIFIER, ValueExistencePolicy.NON_EMPTY);
    }

    protected Map<String,Object> asFullMap(final Object metadata) {
        return standard.asValueMap(metadata, null, KeyNamePolicy.UML_IDENTIFIER, ValueExistencePolicy.NON_EMPTY);
    }

    protected Class readType(ValueNode node) throws ParseException {
        Class type;
        try {
            type = Class.forName(node.type);
        } catch (ClassNotFoundException ex) {
            throw new ParseException("Unable to find a class for type : " + node.type);
        }
        return type;
    }
}
