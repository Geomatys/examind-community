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
 * limitations under the License..
 */
package org.constellation.util;

import java.util.Locale;
import org.apache.sis.internal.metadata.Merger;
import org.apache.sis.internal.metadata.Merger.Resolution;
import org.apache.sis.metadata.ModifiableMetadata;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataMerger extends Merger {

    public MetadataMerger(Locale locale) {
        super(locale);
    }

    @Override
    protected Resolution resolve(Object source, ModifiableMetadata target) {
        return Resolution.MERGE;
    }

    @Override
    protected void merge(ModifiableMetadata target, String propertyName, Object sourceValue, Object targetValue) {
        // do nothing
    }

}
