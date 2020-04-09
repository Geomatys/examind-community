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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;
import org.constellation.util.json.ParameterDescriptorJSONSerializer;
import org.constellation.util.json.ParameterValueJSONDeserializer;
import org.constellation.util.json.ParameterValueJSONSerializer;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.xml.parameter.ParameterValueReader;
import org.geotoolkit.xml.parameter.ParameterValueWriter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.geotoolkit.util.DomUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 */
public final class ParamUtilities extends Static {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.util");

    /**
     * Reads an {@link java.io.InputStream} to build a {@link org.opengis.parameter.GeneralParameterValue}
     * instance according the specified {@link org.opengis.parameter.ParameterDescriptorGroup}.
     *
     * @param stream
     *            the stream to read
     * @param descriptor
     *            the parameter descriptor
     * @return a {@link org.opengis.parameter.GeneralParameterValue} instance
     * @throws java.io.IOException
     *             on error while reading {@link org.opengis.parameter.GeneralParameterValue} XML
     */
    public static GeneralParameterValue readParameter(final InputStream stream,
                                                      final ParameterDescriptorGroup descriptor) throws IOException {
        return readParameterInternal(stream, descriptor);
    }

    public static GeneralParameterValue readParameter(final InputStream stream,
                                                      final GeneralParameterDescriptor descriptor) throws IOException {
        return readParameterInternal(stream, descriptor);
    }

    public static GeneralParameterValue readParameter(final String xml,
                                                      final GeneralParameterDescriptor descriptor) throws IOException {
        return readParameterInternal(xml, descriptor);
    }

    /**
     * FIXME this is a temporary fix for Geotk migration of Examind for backward compatibility of namespace.
     *
     *
     * Convenient method to acquire a DOM document from an input.
     * This is provided as a convenient method, use the default JRE classes so it may
     * not be the faster parsing method.
     */
    private static Document read(final Object input) throws ParserConfigurationException, SAXException, IOException {
        final Document document;
        try (InputStream stream = toInputStream(input)) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            //This is the fix to treat namespace backward compatibility
            factory.setNamespaceAware(true);
            final DocumentBuilder constructeur = factory.newDocumentBuilder();
            document = constructeur.parse(stream);
        }
        return document;
    }

    /**
     * Convert an object source to a stream.
     */
    private static InputStream toInputStream(final Object input) throws FileNotFoundException, IOException{

        //special case when input object is document itelf
        if (input instanceof String) {
            try {
                //try to open it as a path
                final URL url = new URL((String) input);
            } catch (MalformedURLException ex) {
                //consider it's the document itself
                return new ByteArrayInputStream(input.toString().getBytes());
            }
        }

        return IOUtilities.open(input);
    }


    private static GeneralParameterValue readParameterInternal(final Object input,
                                                      final GeneralParameterDescriptor descriptor) throws IOException {
        ensureNonNull("input", input);
        ensureNonNull("descriptor", descriptor);
        try {
            final ParameterValueReader reader = new ParameterValueReader(descriptor);
            reader.setInput(input);
            return reader.read();
        } catch (XMLStreamException ex) {

            try {
                //check for old namespace parameter for backward compatibility
                final Document doc = read(input);
                final Element root = doc.getDocumentElement();
                final Element nsElement = DomUtilities.firstElement(root, "namespace", true);
                if (nsElement != null) {
                    //remove it and retry parsing
                    nsElement.getParentNode().removeChild(nsElement);
                    final ParameterValueReader reader = new ParameterValueReader(descriptor);
                    reader.setInput(new DOMSource(root));
                    return reader.read();
                }
            } catch (ParserConfigurationException | SAXException | XMLStreamException ex1) {
                // do nothing
            }

            throw new IOException("An error occurred while parsing ParameterDescriptorGroup XML.", ex);
        }
    }

    /**
     * Transform a {@link GeneralParameterValue} instance into a {@link String}
     * instance.
     *
     * @param parameter
     *            the parameter to be written
     * @return a {@link String} instance
     * @throws IOException
     *             on error while writing {@link GeneralParameterValue} XML
     */
    public static String writeParameter(final GeneralParameterValue parameter) throws IOException {
        ensureNonNull("parameter", parameter);
        try {
            final StringWriter sw = new StringWriter();
            final ParameterValueWriter writer = new ParameterValueWriter();
            writer.setOutput(sw);
            writer.write(parameter);
            return sw.toString();
        } catch (XMLStreamException ex) {
            throw new IOException("An error occurred while writing ParameterDescriptorGroup XML.", ex);
        }
    }

    /**
     * Serialize a ParameterValueGroup into a JSON String.
     * @param parameter ParameterValueGroup
     * @return JSON String.
     * @throws JsonProcessingException
     * @throws org.apache.sis.util.NullArgumentException if {@code parameter} is {@code null}
     */
    public static String writeParameterJSON(GeneralParameterValue parameter) throws JsonProcessingException {
        ArgumentChecks.ensureNonNull("parameter", parameter);
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(GeneralParameterValue.class, new ParameterValueJSONSerializer()); //custom serializer
        mapper.registerModule(module);
        return mapper.writeValueAsString(parameter);
    }

    /**
     * Deserialize a ParameterValueGroup from a JSON String.
     * @param inputJson String json
     * @param descriptor GeneralParameterDescriptor that describe ParameterValueGroup
     * @return ParameterValueGroup matching GeneralParameterDescriptor descriptor
     * @throws IOException
     * @throws org.apache.sis.util.NullArgumentException if {@code inputJson} or {@code descriptor} are {@code null}
     */
    public static GeneralParameterValue readParameterJSON(String inputJson, GeneralParameterDescriptor descriptor) throws IOException {
        ArgumentChecks.ensureNonNull("inputJson", inputJson);
        ArgumentChecks.ensureNonNull("descriptor", descriptor);
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(GeneralParameterValue.class, new ParameterValueJSONDeserializer(descriptor)); //custom serializer
        mapper.registerModule(module);
        return mapper.readValue(inputJson, GeneralParameterValue.class);
    }

    /**
     * Serialize a GeneralParameterDescriptor into a JSON String.
     * @param descriptor GeneralParameterDescriptor.
     * @return JSON String.
     * @throws JsonProcessingException
     * @throws org.apache.sis.util.NullArgumentException if {@code descriptor} is {@code null}
     */
    public static String writeParameterDescriptorJSON(GeneralParameterDescriptor descriptor) throws JsonProcessingException {
        ArgumentChecks.ensureNonNull("descriptor", descriptor);
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(GeneralParameterDescriptor.class, new ParameterDescriptorJSONSerializer()); //custom serializer
        mapper.registerModule(module);
        return mapper.writeValueAsString(descriptor);
    }
}
