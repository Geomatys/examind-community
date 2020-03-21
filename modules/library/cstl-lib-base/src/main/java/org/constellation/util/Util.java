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

package org.constellation.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.StyleReference;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;

/**
 * Utility methods of general use.
 * <p>
 * TODO: this class needs review.
 *   * methods should be re-ordered for coherence
 *       -- String
 *       -- Reflection
 *       -- ...
 * </p>
 *
 * @author Mehdi Sidhoum (Geomatys)
 * @author Legal Guilhem (Geomatys)
 * @author Adrian Custer (Geomatys)
 *
 * @since 0.2
 */
public final class Util {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.util");

    private static final XMLInputFactory XML_IN_FACTORY = XMLInputFactory.newFactory();

    public static final DateFormat LUCENE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    static {
        LUCENE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static final DateFormat FULL_LUCENE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    static {
        LUCENE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Util() {}

    /**
     * Obtain the Thread Context ClassLoader.
     */
    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Return an input stream of the specified resource.
     */
    public static InputStream getResourceAsStream(final String url) {
        final ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(url);
    }

    /**
     * Parse a String to instantiate a named Layer (namespace : name).
     * @param layerName
     * @return
     */
    public static GenericName parseLayerName(final String layerName) {
        final GenericName name;
        if (layerName != null && layerName.lastIndexOf(':') != -1) {
            final String namespace = layerName.substring(0, layerName.lastIndexOf(':'));
            final String localPart = layerName.substring(layerName.lastIndexOf(':') + 1);
            name = NamesExt.create(namespace, localPart);
        } else {
            name = NamesExt.create(layerName);
        }
        return name;
    }

    public static QName parseQName(String name) {
        if (name != null) {
            if (name.startsWith("{}")) {
                name = name.substring(2);
            }
            return QName.valueOf(name);
        }
        return null;
    }


    public static String getXmlDocumentRoot(final String filePath) throws IOException, XMLStreamException {

        XMLStreamReader xsr = null;
        try (InputStream stream = Files.newInputStream(IOUtilities.toPath(filePath))) {
            xsr = XML_IN_FACTORY.createXMLStreamReader(stream);
            xsr.nextTag();
            return xsr.getLocalName();
        } finally {
            if (xsr != null) {
                xsr.close();
            }
        }
    }

    /**
     * Rename a file behind a Path with given name (keeing the previous extension)
     *
     * @param dataName new name (without extension)
     * @param filePath file to rename
     * @return Path to renamed file
     * @throws IOException
     */
    public static Path renameFile(String dataName, Path filePath) throws IOException {
        filePath = filePath.normalize();
        final String fileExt = IOUtilities.extension(filePath);
        final String newFileName = dataName + "." + fileExt;
        final Path newPath = filePath.getParent().resolve(newFileName);
        Files.move(filePath, newPath, StandardCopyOption.REPLACE_EXISTING);
        return newPath;
    }

    public static boolean containsInfinity(final GeneralEnvelope env){
        return Double.isInfinite(env.getLower(0)) || Double.isInfinite(env.getUpper(0)) ||
                Double.isInfinite(env.getLower(1)) || Double.isInfinite(env.getUpper(1));
    }


    public static Long getDeltaTime(String period) {
        final long currentTs = System.currentTimeMillis();
        final long dayTms = 1000 * 60 * 60 * 24L;
        if ("week".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 7);
        } else if ("month".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 30);
        } else if ("3months".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 90);
        } else if ("6months".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 180);
        } else if ("year".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 365);
        }
        return null;
    }

    public static GenericName getLayerId(DataReference dr) {
        if (dr != null) {
            return NamesExt.valueOf(dr.layerId);
        }
        return null;
    }

    public static DataReference getStyleReference(final String styleID, List<DataReference> styles) {
        if (styles != null) {
            for (DataReference styleRef : styles) {
                if (Util.getLayerId(styleRef).tip().toString().equals(styleID)) {
                    return styleRef;
                }
            }
        }
        return null;
    }

    public static StyleReference findStyleReference(final String styleID, List<StyleReference> styles) {
        if (styles != null) {
            for (StyleReference styleRef : styles) {
                if (styleRef.getName().equals(styleID)) {
                    return styleRef;
                }
            }
        }
        return null;
    }

    public static List<StyleBrief> convertIntoStylesBrief(final List<DataReference> refs) {
        final List<StyleBrief> briefs = new ArrayList<>();
        if (refs != null) {
            for (final DataReference ref: refs) {
                final StyleBrief styleToAdd = new StyleBrief();
                styleToAdd.setProvider(ref.getProviderId());
                final String styleName = getLayerId(ref).tip().toString();
                styleToAdd.setName(styleName);
                styleToAdd.setTitle(styleName);
                briefs.add(styleToAdd);
            }
        }
        return briefs;
    }

    public static List<StyleBrief> convertRefIntoStylesBrief(final List<StyleReference> refs) {
        final List<StyleBrief> briefs = new ArrayList<>();
        if (refs != null) {
            for (final StyleReference ref: refs) {
                final StyleBrief styleToAdd = new StyleBrief();
                styleToAdd.setProvider(ref.getProviderIdentifier());
                styleToAdd.setProviderId(ref.getProviderId());
                final String styleName = ref.getName();
                styleToAdd.setName(styleName);
                styleToAdd.setTitle(styleName);
                styleToAdd.setId(ref.getId());
                briefs.add(styleToAdd);
            }
        }
        return briefs;
    }
}
