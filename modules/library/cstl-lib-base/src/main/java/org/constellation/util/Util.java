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
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.StyleReference;
import org.constellation.exception.ConfigurationException;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.StringUtilities;
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
        FULL_LUCENE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
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
     * Return a List of named Layer  ({namespace}name ,namespace : name or just name) from a comma separated string.
     *
     * @param layerNamesStr A comma separated layer name string.
     *
     * @return A list of GenericName.
     */
    public static List<GenericName> parseLayerNameList(String layerNamesStr) {
        List<String> layerNames = StringUtilities.toStringList(layerNamesStr);
        return parseLayerNameList(layerNames);
    }

    /**
     * Return a List of named Layer ({namespace}name ,namespace : name or just name) from a string list.
     *
     * @param layerNames A list of layer name.
     * @return A list of GenericName.
     */
    public static List<GenericName> parseLayerNameList(List<String> layerNames) {
        final List<GenericName> result = new ArrayList<>();
        for (String layerName : layerNames) {
            result.add(Util.parseLayerName(layerName));
        }
        return result;
    }

    /**
     * Parse a String on the form ({namespace}name ,namespace : name or just name).
     *
     * @param layerName A String on the form ({namespace}name ,namespace : name or just name).
     * @return  A list of GenericName.
     */
    public static GenericName parseLayerName(final String layerName) {
        if (layerName == null) return null;
        final GenericName name;
        if (layerName.startsWith("{") && layerName.contains("}")) {
            int nmspEnd = layerName.indexOf('}');
            final String namespace = layerName.substring(1, nmspEnd);
            final String localPart = layerName.substring(nmspEnd + 1);
            name = NamesExt.create(namespace, localPart);
        } else if (layerName.contains(":")) {
            int nmspEnd = layerName.lastIndexOf(':');
            final String namespace = layerName.substring(0, nmspEnd);
            final String localPart = layerName.substring(nmspEnd+ 1);
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

    public static String getProcessAuthorityCode(ProcessDescriptor desc) {
        String processAuthority = null;
        if (!desc.getIdentifier().getAuthority().getIdentifiers().isEmpty()) {
            processAuthority = desc.getIdentifier().getAuthority().getIdentifiers().iterator().next().getCode();
        } else if (desc.getIdentifier().getAuthority().getTitle() != null){
            // fallback on Title
            processAuthority =  desc.getIdentifier().getAuthority().getTitle().toString();
        }
        return processAuthority;
    }

    /**
     * return true if the specified string contains forbidden SQL characters, leading to potential SQL injection.
     * 
     * @param s
     * @return
     */
    public static boolean containsForbiddenCharacter(String s) {
        return s.contains("'") || s.contains("/") || s.contains("--") || s.contains("\"") || s.contains("*");
    }

    /**
     * Marshall a configuration object into a String using the specified MarshallerPool
     *
     * @param obj An examind configuratin object.
     * @param pool he MarshallerPool used to marshall the object.
     * @return A XML string representation of the object
     */
    public static String writeConfigurationObject(final Object obj, MarshallerPool pool) throws ConfigurationException {
        String config = null;
        if (obj != null) {
            try {
                final StringWriter sw = new StringWriter();
                final Marshaller m = pool.acquireMarshaller();
                m.marshal(obj, sw);
                pool.recycle(m);
                config = sw.toString();
            } catch (JAXBException e) {
                throw new ConfigurationException(e);
            }
        }
        return config;
    }

    /**
     * Marshall a configuration object into a String using the {@link GenericDatabaseMarshallerPool}.
     * 
     * @param obj An examind configuratin object.
     * @return A XML string representation of the object
     */
    public static String writeConfigurationObject(final Object obj) throws ConfigurationException {
        return writeConfigurationObject(obj, GenericDatabaseMarshallerPool.getInstance());
    }

    /**
     * Read a configuration object into a String using the {@link GenericDatabaseMarshallerPool}.
     *
     * @param <T>
     * @param xml
     * @param type
     * @return
     * @throws ConfigurationException
     */
    public static <T> T readConfigurationObject(final String xml, Class<T> type, MarshallerPool pool) throws ConfigurationException {
        try {
            if (xml != null) {
                final Unmarshaller u = pool.acquireUnmarshaller();
                final Object config = u.unmarshal(new StringReader(xml));
                pool.recycle(u);
                return (T) config;
            }
            return null;
        } catch (JAXBException ex) {
            throw new ConfigurationException("The configuration object is malformed.", ex);
        }
    }

    public static <T> T readConfigurationObject(final String xml, Class<T> type) throws ConfigurationException {
        return readConfigurationObject(xml, type, GenericDatabaseMarshallerPool.getInstance());
    }

    /**
     * Read a configuration object into a String using the {@link GenericDatabaseMarshallerPool}.
     *
     * @param <T>
     * @param xml
     * @param type
     * @return
     * @throws ConfigurationException
     */
    public static <T> T readConfigurationObject(final InputStream xml, Class<T> type) throws ConfigurationException {
        try {
            if (xml != null) {
                final Unmarshaller u = GenericDatabaseMarshallerPool.getInstance().acquireUnmarshaller();
                final Object config = u.unmarshal(xml);
                GenericDatabaseMarshallerPool.getInstance().recycle(u);
                return (T) config;
            }
            return null;
        } catch (JAXBException ex) {
            throw new ConfigurationException("The configuration object is malformed.",ex);
        }
    }

}
