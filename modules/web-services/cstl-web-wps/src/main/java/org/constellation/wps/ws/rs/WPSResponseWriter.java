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

package org.constellation.wps.ws.rs;

import org.apache.sis.util.logging.Logging;
import org.geotoolkit.wps.xml.WPSMarshallerPool;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.StringUtilities;
import org.geotoolkit.wps.xml.WPSResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Quentin Boileau
 */
public class WPSResponseWriter implements HttpMessageConverter<WPSResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.ws.rs");
    
    public static final Map<String, String> XML_TO_JSON_NAMESPACES = new HashMap<>();
    static {
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/wps/1.0.0",           "wps");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/wps/2.0",             "wps2");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/gml",                 "gml");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/gml/3.2",             "gml32");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/ows/1.1",             "ows");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/ows/2.0",             "ows2");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/ogc",                 "ogc");
        XML_TO_JSON_NAMESPACES.put("http://www.opengis.net/fes/2.0",             "fes");
        XML_TO_JSON_NAMESPACES.put("http://www.w3.org/1999/xlink",               "xlink");
        XML_TO_JSON_NAMESPACES.put("http://www.w3.org/XML/1998/namespace",       "nmsp");
        XML_TO_JSON_NAMESPACES.put("http://www.cnig.gouv.fr/2005/fra",           "fra");
        XML_TO_JSON_NAMESPACES.put("http://www.isotc211.org/2005/gco",           "gco");
        XML_TO_JSON_NAMESPACES.put("http://www.isotc211.org/2005/gmx",           "gmx");
        XML_TO_JSON_NAMESPACES.put("http://www.isotc211.org/2005/gmi",           "gmi");
        XML_TO_JSON_NAMESPACES.put("http://www.isotc211.org/2005/gmd",           "gmd");
        XML_TO_JSON_NAMESPACES.put("http://www.isotc211.org/2005/gts",           "gts");
        XML_TO_JSON_NAMESPACES.put("urn:us:gov:ic:ism:v2",                       "ism");
        XML_TO_JSON_NAMESPACES.put("http://www.w3.org/2001/XMLSchema-instance",  "xsi");
        XML_TO_JSON_NAMESPACES.put("http://www.w3.org/2005/08/addressing",       "adr");
        XML_TO_JSON_NAMESPACES.put("http://docs.oasis-open.org/wsn/t-1",         "wsn");

    }
    
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return WPSResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_JSON);
    }
    
    @Override
    public WPSResponse read(Class<? extends WPSResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("WPSResponse message converter do not support reading.");
    }

    @Override
    public void write(WPSResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType media = null;
        try {
            media = outputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            LOGGER.log(Level.FINER, "unparseable mime type.", ex);
        }
        if (media == null) {
            media = contentType;
        }
        try {
            final Marshaller m = WPSMarshallerPool.getInstance().acquireMarshaller();        
            if ((MediaType.APPLICATION_JSON.equals(media))) {
                final Configuration config = new Configuration(XML_TO_JSON_NAMESPACES);
                final MappedNamespaceConvention con = new MappedNamespaceConvention(config);
                final Writer writer = new OutputStreamWriter(outputMessage.getBody());
                final XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer);
                m.marshal(t, xmlStreamWriter);
            } else {
                
                // temporary patch to handle CDATA with JAXB
                StringWriter sw = new StringWriter();
                m.marshal(t, sw);
                String s = sw.toString();
                s = s.replace("&lt;![CDATA[", "<![CDATA[");
                s = s.replace("]]&gt;", "]]>");
                IOUtils.write(s, outputMessage.getBody(), "UTF-8");
            }
            WPSMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
                LOGGER.log(Level.SEVERE, "JAXB exception while writing the wps Response", ex);
        }
    }
}
