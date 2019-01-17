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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.DataDescription;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.ConstellationRuntimeException;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.NamesExt;
import org.opengis.geometry.Envelope;
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
     * This should be a class loader from the main constellation application.
     */
    private static final ClassLoader baseClassLoader;

    //we try to load this variable at the start by reading a properties file
    static {
        baseClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public static <T> T copy(Object src, T dst) {
        try {
            BeanUtils.copyProperties(dst, src);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ConstellationRuntimeException(e);
        }
        return dst;
    }


    /**
     * Return an marshallable Object from an url
     */
    public static Object getUrlContent(final String url, final Unmarshaller unmarshaller) throws MalformedURLException, IOException {
        final URL source         = new URL(url);
        final URLConnection conec = source.openConnection();
        Object response = null;

        try {

            // we get the response document
            final InputStream in   = conec.getInputStream();
            final StringWriter out = new StringWriter();
            final byte[] buffer    = new byte[1024];
            int size;

            while ((size = in.read(buffer, 0, 1024)) > 0) {
                out.write(new String(buffer, 0, size));
            }

            //we convert the brut String value into UTF-8 encoding
            String brutString = out.toString();

            //we need to replace % character by "percent because they are reserved char for url encoding
            brutString = brutString.replaceAll("%", "percent");
            final String decodedString = java.net.URLDecoder.decode(brutString, "UTF-8");

            try {
                response = unmarshaller.unmarshal(new StringReader(decodedString));
                if (response instanceof JAXBElement) {
                    response = ((JAXBElement<?>) response).getValue();
                }
            } catch (JAXBException ex) {
                LOGGER.log(Level.SEVERE, "The distant service does not respond correctly: unable to unmarshall response document.\ncause: {0}", ex.getMessage());
            }
        } catch (IOException ex) {
            LOGGER.severe("The Distant service have made an error");
            return null;
        }
        return response;
    }

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

    /**
     * Parse a String to instantiate a named Layer ({namespace}name).
     * @param layerName
     * @return
     *
     * @deprecated use parseQName()
     */
    @Deprecated
    public static QName parseLayerQName(final String layerName) {
        final QName name;
        if (layerName != null && layerName.lastIndexOf('}') != -1) {
            final String namespace = layerName.substring(1, layerName.lastIndexOf('}'));
            final String localPart = layerName.substring(layerName.lastIndexOf('}') + 1);
            name = new QName(namespace, localPart);
        } else {
            name = new QName(layerName);
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

    /**
     * Fills the geographical field of a {@link DataDescription} instance according the
     * specified {@link Envelope}.
     *
     * @param envelope    the envelope to visit
     * @param description the data description to update
     */
    public static void fillGeographicDescription(Envelope envelope, final DataDescription description) {
        double[] lower, upper;
        try {
            GeneralEnvelope trsf = GeneralEnvelope.castOrCopy(Envelopes.transform(envelope, CommonCRS.defaultGeographic()));
            trsf.simplify();
            lower = trsf.getLowerCorner().getCoordinate();
            upper = trsf.getUpperCorner().getCoordinate();
        } catch (Exception ignore) {
            lower = new double[]{-180, -90};
            upper = new double[]{180, 90};
        }
        description.setBoundingBox(new double[]{lower[0], lower[1], upper[0], upper[1]});
    }
}
