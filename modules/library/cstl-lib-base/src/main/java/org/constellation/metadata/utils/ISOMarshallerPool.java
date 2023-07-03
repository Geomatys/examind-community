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

package org.constellation.metadata.utils;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.xml.MarshallerPool;
import org.geotoolkit.xml.AnchoredMarshallerPool;

import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ISOMarshallerPool {
    private static final MarshallerPool instance;
    static {
        try {
            final Map<String, Object> properties = new HashMap<>();
            instance = new AnchoredMarshallerPool(createJAXBContext(properties, getAllClassesList()));
        } catch (JAXBException ex) {
            throw new AssertionError(ex); // Should never happen, unless we have a build configuration problem.
        }
    }

    private ISOMarshallerPool() {}

    public static MarshallerPool getInstance() {
        return instance;
    }

    private static Class[] getAllClassesList() {
        final List<Class> classeList = new ArrayList<>();

        //ISO 19115 class
        classeList.add(DefaultMetadata.class);

        // Inspire classes
        try {
            Class insClass = Class.forName("org.geotoolkit.inspire.xml.ObjectFactory");
            classeList.add(insClass);
        } catch (ClassNotFoundException ex) {}


        // GML base factory
        classeList.add(org.apache.sis.internal.jaxb.geometry.ObjectFactory.class);

        // GML 3.1.1 / 3.2.1
        try {
            Class gml311Class = Class.forName("org.geotoolkit.gml.xml.v311.ObjectFactory");
            Class gml321Class = Class.forName("org.geotoolkit.gml.xml.v321.ObjectFactory");
            classeList.add(gml311Class);
            classeList.add(gml321Class);
        } catch (ClassNotFoundException ex) {}


        // vertical CRS
        try {
            Class vcrsClass = Class.forName("org.apache.sis.referencing.crs.DefaultVerticalCRS");
            classeList.add(vcrsClass);
        } catch (ClassNotFoundException ex) {}

        // we add the extensions classes
        classeList.add(org.apache.sis.metadata.iso.identification.DefaultServiceIdentification.class);

         return classeList.toArray(new Class[classeList.size()]);
    }

    public static JAXBContext createJAXBContext(final Map<String,?> properties, final Class<?>... classes) throws JAXBException {
        return JAXBContext.newInstance(classes, properties);
    }

}
