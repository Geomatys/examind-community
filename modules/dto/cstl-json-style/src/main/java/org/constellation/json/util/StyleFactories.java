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

package org.constellation.json.util;

import org.apache.sis.util.Static;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.sld.DefaultSLDFactory;
import org.geotoolkit.sld.MutableSLDFactory;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.filter.FilterUtilities;
import org.opengis.filter.FilterFactory;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class StyleFactories extends Static {

    /**
     * SLD factory.
     */
    public static final MutableSLDFactory SLDF = new DefaultSLDFactory();

    /**
     * Style elements factory.
     */
    public static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;

    /**
     * Filters factory.
     */
    public static final FilterFactory FF = FilterUtilities.FF;
}
