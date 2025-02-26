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
package org.constellation.ws.rs;

import static org.constellation.ws.ExceptionCode.INVALID_PARAMETER_VALUE;
import static org.constellation.ws.ExceptionCode.INVALID_REQUEST;
import static org.constellation.ws.ExceptionCode.MISSING_PARAMETER_VALUE;
import static org.constellation.ws.ExceptionCode.OPERATION_NOT_SUPPORTED;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.apache.sis.xml.MarshallerPool;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.WebServiceUtilities;
import org.constellation.xml.PrefixMappingInvocationHandler;
import org.geotoolkit.util.StringUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Abstract parent of all REST facade classes for Constellation web services.
 * <p>
 * This class begins the handling of all REST message exchange processing. In
 * the REST style of web service, message parameters either are passed directly
 * as arguments to the query, e.g.<br>
 *   {@code protocol://some.url/service?param=value&param2=othervalue }<br>
 * or are passed as raw messages in the body of an HTTP POST message, for
 * example as Key-Value Pairs (KVP) or as XML documents.
 * </p>
 * <p>
 * <i>Note:</i> This use of the term REST does not imply the services are
 * RESTful; we use the term to distinguish these classes from the other facade
 * classes in Constellation which use SOAP to exchange messages in HTTP POST
 * exchanges and JAXB to automatically unmarshall those messages into Java
 * objects.
 * </p>
 * <p>
 * All incoming requests are handled by one of the {@code doGET} or
 * {@code doPOST*} methods. These methods handle the incoming requests by
 * ensuring all KVP parameters are in the {@code uriContext} object and all
 * other information is in a serializable object of the right kind. The methods
 * then call the abstract {@code treatIncomingRequest(Object)} passing any
 * serializable object as the method parameter. Sub-classes then handle the
 * request calling the {@code uriContext} object or using the method parameter
 * as needed.
 * </p>
 * <p>
 * Two other abstract methods need to be implemented by extending classes. The
 * method {@code destroy()} will be called prior to the container shutting down
 * the service providing an opportunity to log that event. The method
 * {@code launchException(..)} forms part of the Constellation exception
 * handling design.
 * </p>
 * <p>
 * TODO: explain the design for exception handling.
 * </p>
 * <p>
 * Concrete extensions of this class should, in their constructor, call one of
 * the {@code setXMLContext(..)} methods to initialize the JAXB context and
 * populate the {@code marshaller} and {@code unmarshaller} fields.
 * </p>
 * <p>
 * Classes extending this one provide the REST facade to Constellation. Most of
 * the concrete extensions of this class in Constellation itself implement the
 * logic of {@code treatIncomingRequest(Object)} by calling a appropriate
 * method in a {@code Worker} object. Those same methods in the {@code Worker}
 * object are also called by the classes implementing the SOAP facade, enabling
 * the re-use of the logic.
 * </p>
 *
 * @author Guilhem Legal (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @author Adrian Custer (Geomatys)
 * @since 0.1
 */
public abstract class AbstractWebService implements WebService{
    /**
     * The default debugging logger for all web services.
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs");

    /**
     * Automatically set by Jersey.
     *
     * Used to communicate with the Servlet container, for example, to obtain
     * the MIME type of a file, to dispatch requests or to write to a log file.
     * The field is injected, thanks to the annotation, when a request arrives.
     */
    @Autowired(required = false)
    private volatile ServletContext servletContext;

    /**
     * Automatically set by Jersey.
     *
     * The HTTP Servlet request used to get information about the client which
     * sent the request. The field is injected, thanks to the annotation, when a
     * request arrives.
     */
    @Autowired(required = false)
    private volatile HttpServletRequest httpServletRequest;

    /**
     * Automatically set by Jersey.
     *
     * The HTTP Servlet response.
     * The field is injected, thanks to the annotation, when a
     * request arrives.
     */
    @Autowired(required = false)
    private volatile HttpServletResponse httpServletResponse;

    /**
     * The POST  kvp request parameters (one by thread)
     */
    private final ThreadLocal<Map<String, String[]>> postKvpParameters = new ThreadLocal<Map<String, String[]>>(){

        @Override
        protected Map<String, String[]> initialValue() {
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
    };

    /**
     * A pool of JAXB unmarshaller used to create Java objects from XML files.
     */
    private MarshallerPool marshallerPool;

    /**
     * Used to communicate with the servlet container, for example, to obtain
     * the MIME type of a file, to dispatch requests or to write to a log file.
     *
     * @return
     */
    protected final ServletContext getServletContext(){
        return servletContext;
    }

    /**
     * The HTTP servlet request used to get information about the client which
     * sent the request.
     *
     * @return
     */
    protected final HttpServletRequest getHttpServletRequest(){
        return httpServletRequest;
    }

    /**
     * Treat the incoming request and call the right function in the worker.
     * <p>
     * The parent class will have processed the request sufficiently to ensure
     * all the relevant information is either in the {@code uriContext} field or
     * in the {@code Object} passed in as a parameter. Here we proceed a step
     * further to ensure the request is encapsulated in a Java object which we
     * then pass to the worker when calling the appropriate method.
     * </p>
     *
     * @param  objectRequest  an object encapsulating the request or {@code null}
     *                          if the request parameters are all in the
     *                          {@code uriContext} field.
     * @return a Response, either an image or an XML document depending on the
     *           user's request.
     */
    public abstract ResponseObject treatIncomingRequest(Object objectRequest);

    /**
     * {@inheritDoc }
     */
    @Override
    public void destroy(){
    }

    /**
     * build an service Exception and marshall it into a StringWriter
     *
     * @param message
     * @param codeName
     * @param locator
     * @return
     */
    protected abstract ResponseObject launchException(String message, String codeName, String locator);

    /**
     * Provide the marshaller pool.
     * Live it's instantiation to implementations.
     */
    protected synchronized MarshallerPool getMarshallerPool() {
        return marshallerPool;
    }

    /**
     * Initialize the JAXB context.
     */
    protected synchronized void setXMLContext(final MarshallerPool pool) {
        LOGGER.finer("SETTING XML CONTEXT: marshaller Pool version");
        marshallerPool = pool;
    }

    protected void putServiceIdParam(String serviceId) {
        Map<String, String[]> kvpMap = postKvpParameters.get();
        if (kvpMap == null) {
            kvpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        kvpMap.put("serviceId", new String[]{serviceId});
        postKvpParameters.set(kvpMap);
    }

    protected void putParam(String name, String value) {
        Map<String, String[]> kvpMap = postKvpParameters.get();
        if (kvpMap == null) {
            kvpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        kvpMap.put(name, new String[]{value});
        postKvpParameters.set(kvpMap);
    }

    protected void clearKvpMap() {
        postKvpParameters.set(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Treat the incoming GET request.
     *
     * @return an image or xml response.
     */
    @RequestMapping(method = RequestMethod.GET, headers="Accept=*/*")
    public @ResponseBody ResponseEntity doGET(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        try {
            return treatIncomingRequest(null).getResponseEntity(httpServletResponse);
        } finally {
            clearKvpMap();
        }
    }

    /**
     * Treat the incoming POST request encoded in kvp.
     * for each parameters in the request it fill the httpContext.
     *
     * @param request
     * @return an image or xml response.
     */
    @RequestMapping(consumes = "application/x-www-form-urlencoded", method = RequestMethod.POST, headers="Accept=*/*")
    public ResponseEntity doPOSTKvp(@PathVariable("serviceId") String serviceId, final @RequestBody String request) throws UnsupportedEncodingException {
        /**
         * decode string that can be encoded to utf8 url. ie : image%2Fpng will
         * be image/png
         */
        final String params = URLDecoder.decode(request, "UTF-8");
        final StringTokenizer tokens = new StringTokenizer(params, "&");
        final Map<String, String[]> kvpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken().trim();
            final int equalsIndex = token.indexOf('=');
            final String paramName = token.substring(0, equalsIndex);
            final String paramValue = token.substring(equalsIndex + 1);
            // special case for XML request parameter
            if ("request".equalsIgnoreCase(paramName) && (paramValue.startsWith("<") || paramValue.startsWith("%3C"))) {
                final String xml = URLDecoder.decode(paramValue, "UTF-8");
                final InputStream in = new ByteArrayInputStream(xml.getBytes());
                return doPOSTXml(serviceId, in);
            }
            kvpMap.put(paramName, new String[]{paramValue});
        }
        kvpMap.put("serviceId", new String[]{serviceId});
        postKvpParameters.set(kvpMap);
        try {
            return treatIncomingRequest(null).getResponseEntity(httpServletResponse);
        } finally {
            clearKvpMap();
        }
    }

    /**
     * Treat the incoming POST request encoded in xml.
     *
     * @return an image or xml response.
     */
    @RequestMapping(consumes = {"text/xml", "application/xml"},  method = RequestMethod.POST, headers="Accept=*/*")
    public ResponseEntity doPOSTXml(@PathVariable("serviceId") String serviceId, final InputStream is) {
        putServiceIdParam(serviceId);
        if (marshallerPool != null) {
            final Object request;

            // we look for a configuration query
            final boolean requestValidationActivated;
            final List<Schema> schemas;
            if (serviceId != null){
                requestValidationActivated = isRequestValidationActivated(serviceId);
                schemas                    = getRequestValidationSchema(serviceId);
            } else {
                schemas                    = null;
                requestValidationActivated = false;
            }
            final MarshallerPool pool = getMarshallerPool();
            final Map<String, String> prefixMapping = new LinkedHashMap<>();
            try {
                final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
                if (requestValidationActivated) {
                    for (Schema schema : schemas) {
                        unmarshaller.setSchema(schema);
                    }
                    request = unmarshallRequestWithMapping(unmarshaller, is, prefixMapping);
                } else {
                    request = unmarshallRequest(unmarshaller, is);
                }
                pool.recycle(unmarshaller);
            } catch (JAXBException e) {
                String errorMsg = e.getMessage();
                if (errorMsg == null) {
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        errorMsg = e.getCause().getMessage();
                    } else if (e.getLinkedException() != null && e.getLinkedException().getMessage() != null) {
                        errorMsg = e.getLinkedException().getMessage();
                    }
                }
                final String codeName;
                if (errorMsg != null && errorMsg.startsWith("unexpected element")) {
                    codeName = OPERATION_NOT_SUPPORTED.name();
                } else {
                    codeName = INVALID_REQUEST.name();
                }
                final String locator = WebServiceUtilities.getValidationLocator(errorMsg, prefixMapping);

                return launchException("The XML request is not valid.\nCause:" + errorMsg, codeName, locator).getResponseEntity();
            } catch (CstlServiceException e) {

                return launchException(e.getMessage(), e.getExceptionCode().identifier().orElse(null).toString(), e.getLocator()).getResponseEntity();
            }
            try {
                return treatIncomingRequest(request).getResponseEntity(httpServletResponse);
            } finally {
                clearKvpMap();
            }
        } else {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<String>("This service is not running", responseHeaders, HttpStatus.OK);
        }
    }

    /**
     * Treat the incoming POST request encoded in text plain.
     *
     * @return an xml exception report.
     */
    @RequestMapping(consumes = "text/plain", method = RequestMethod.POST, headers="Accept=*/*")
    public ResponseEntity doPOSTPlain(@PathVariable("serviceId") String serviceId, final InputStream is) {
        putServiceIdParam(serviceId);
        LOGGER.warning("request POST plain sending Exception");
        return launchException("The plain text content type is not allowed. Send " +
        		       "a message body with key=value pairs in the " +
        		       "application/x-www-form-urlencoded MIME type, or " +
        		       "an XML file using an application/xml or text/xml " +
        		       "MIME type.", INVALID_REQUEST.name(), null).getResponseEntity();
    }

    protected abstract boolean isRequestValidationActivated(final String workerID);

    protected abstract List<Schema> getRequestValidationSchema(final String workerID);

    /**
     * A method simply unmarshalling the request with the specified unmarshaller from the specified inputStream.
     * can be overriden by child class in case of specific extractionfrom the stream.
     *
     * @param unmarshaller A JAXB Unmarshaller correspounding to the service context.
     * @param is The request input stream.
     * @return
     * @throws JAXBException
     */
    protected Object unmarshallRequest(final Unmarshaller unmarshaller, final InputStream is) throws JAXBException, CstlServiceException {
        return unmarshaller.unmarshal(is);
    }

    protected Object unmarshallRequestWithMapping(final Unmarshaller unmarshaller, final InputStream is, final Map<String, String> prefixMapping) throws JAXBException {
        try {
            final XMLInputFactory factory           = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLEventReader rootEventReader    = factory.createXMLEventReader(is);
            final XMLEventReader eventReader        = (XMLEventReader) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{XMLEventReader.class}, new PrefixMappingInvocationHandler(rootEventReader, prefixMapping));

            return unmarshaller.unmarshal(eventReader);
        } catch (XMLStreamException ex) {
            throw new JAXBException(ex);
        }
    }

    protected String getHeaderValue(final String headerName) {
        if (httpServletRequest != null) {
            return httpServletRequest.getHeader(headerName);
        }
        return null;
    }

    /**
     * Extracts the value, for a parameter specified, from a query.
     *
     * @param parameterName The name of the parameter.
     *
     * @return the parameter, or {@code null} if not specified.
     */
    private List<String> getParameter(final String parameterName) {
        final Map<String,String[]> parameters;
        if (httpServletRequest != null) {
            parameters = httpServletRequest.getParameterMap();
        } else {
            parameters = new HashMap<>();
        }
        String[] values = parameters.get(parameterName);

        //maybe the parameterName is case sensitive.
        if (values == null) {
            for(final Entry<String, String[]> key : parameters.entrySet()){
                if(key.getKey().equalsIgnoreCase(parameterName)){
                    values = key.getValue();
                    break;
                }
            }
        }

        // look in the POST kvp parameter
        if (values == null) {
            values = postKvpParameters.get().get(parameterName);
        }

        /* look in Path parameters
        // TODO issues here with Jersey => Spring
        if (values == null) {
            final MultivaluedMap<String,String> pathParameters = uriContext.getPathParameters();
            List<String> pp = pathParameters.get(parameterName);
            values = pp.toArray(new String[pp.size()]);
        }*/
        if (values == null) {
            return null;
        }
        return Arrays.asList(values);
    }

    /**
     * Extracts the value, for a parameter specified, from a query.
     * If it is a mandatory one, and if it is {@code null}, it throws an exception.
     * Otherwise returns {@code null} in the case of an optional parameter not found.
     * The parameter is then parsed as boolean.
     *
     * @param parameterName The name of the parameter.
     * @param mandatory true if this parameter is mandatory, false if its optional.
      *
     * @return the parameter, or {@code null} if not specified and not mandatory.
     * @throws  CstlServiceException
     */
    protected boolean getBooleanParameter(final String parameterName, final boolean mandatory) throws CstlServiceException {
        return Boolean.parseBoolean(getParameter(parameterName, mandatory));
    }

    /**
     * Extracts the value, for a parameter specified, from a query.
     * returns {@code null} if not found.
     * The parameter is then parsed as integer.
     *
     * @param parameterName The name of the parameter.
      *
     * @return the parameter, or {@code null} if not found.
     * @throws CstlServiceException If the parameter can not be parsed as an Integer.
     */
    protected Integer parseOptionalIntegerParam(String parameterName) throws CstlServiceException {
        Integer result = null;
        final String value = getParameter(parameterName, false);
        if (value != null) {
            try {
                result = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new CstlServiceException("Unable to parse the integer " + parameterName + " parameter" + value,
                                                  INVALID_PARAMETER_VALUE, parameterName);
            }

        }
        return result;
    }

    protected List<String> parseCommaSeparatedParameter(String paramName) throws CstlServiceException {
        final String propertyNameParam = getParameter(paramName, false);
        final List<String> results = new ArrayList<>();
        if (propertyNameParam != null) {
            final StringTokenizer tokens = new StringTokenizer(propertyNameParam, ",;");
            while (tokens.hasMoreTokens()) {
                final String token = tokens.nextToken().trim();
                results.add(token);
            }
        }
        return results;
    }

    /**
     * Extracts the value, for a parameter specified, from a query.
     * If it is a mandatory one, and if it is {@code null}, it throws an exception.
     * Otherwise returns {@code null} in the case of an optional parameter not found.
     *
     * @param parameterName The name of the parameter.
     * @param mandatory true if this parameter is mandatory, false if its optional.
      *
     * @return the parameter, or {@code null} if not specified and not mandatory.
     * @throws CstlServiceException if the parameter is mandotory and missing.
     */
    protected String getParameter(final String parameterName, final boolean mandatory) throws CstlServiceException {
        return getParameter(parameterName, Collections.EMPTY_LIST, mandatory);
    }

    /**
     * Extracts the value, for a parameter specified, from a query.
     * If it is a mandatory one, and if it is {@code null}, it throws an exception.
     * Otherwise returns {@code null} in the case of an optional parameter not found.
     *
     * @param parameterName The name of the parameter.
     * @param alternatives Alternative parameter names.
     * @param mandatory true if this parameter is mandatory, false if its optional.
      *
     * @return the parameter, or {@code null} if not specified and not mandatory.
     * @throws CstlServiceException if the parameter is mandotory and missing.
     */
    protected String getParameter(final String parameterName, List<String> alternatives, final boolean mandatory) throws CstlServiceException {

        List<String> values = getParameter(parameterName);
        if (values == null || values.isEmpty()) {
            if (alternatives != null && !alternatives.isEmpty()) {
                for (String alternative : alternatives) {
                    values = getParameter(alternative);
                    if (values != null && !values.isEmpty()) break;
                }
            }
        }

        if (values == null || values.isEmpty()) {
            if (mandatory) {
                throw new CstlServiceException("The parameter " + parameterName + " must be specified",
                        MISSING_PARAMETER_VALUE, parameterName.toLowerCase());
            }
            return null;
        } else {
            final String value = values.get(0);
            if (value == null && mandatory) {
                throw new CstlServiceException("The parameter " + parameterName + " should have a value",
                        MISSING_PARAMETER_VALUE, parameterName.toLowerCase());
            } else {
                return value;
            }
        }
    }

    protected String getSafeParameter(final String parameterName) {
        final List<String> values = getParameter(parameterName);
        if (values == null) {
            return null;
        } else {
            return values.get(0);
        }
    }

    /**
     * Return a map of parameters put in the query.
     * @return
     */
    public Map<String, String[]> getParameters() {
        final Map<String, String[]> results = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // GET parameters
        if (httpServletRequest != null) {
            for (final Entry<String, String[]> entry : httpServletRequest.getParameterMap().entrySet()) {
                if (entry.getValue() != null && entry.getValue().length != 0) {
                    results.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // POST kvp parameters
        for (final Entry<String, String[]> entry : postKvpParameters.get().entrySet()) {
            if (entry.getValue() != null && entry.getValue().length != 0) {
                results.put(entry.getKey(), entry.getValue());
            }
        }
        return results;
    }

    protected String getLogParameters() {
        String log = null;
        if (httpServletRequest != null && httpServletRequest.getRequestURL() != null) {
            StringBuffer buff = httpServletRequest.getRequestURL();
            if (httpServletRequest.getQueryString() != null) {
                buff = buff.append('?').append(httpServletRequest.getQueryString());
            }
            log = buff.toString();
        }
        return log;
    }
    /**
     * Extract all The parameters from the query and write it in the console.
     * It is a debug method.
     */
    protected void logParameters() {
        String log = getLogParameters();
        if (log != null) {
            LOGGER.info(log);
        }
    }

    protected void logPostParameters(final Object request) {
        if (request != null && LOGGER.isLoggable(Level.INFO)) {
            final MarshallerPool pool = getMarshallerPool();
            try {
                final Marshaller m = pool.acquireMarshaller();
                final StringWriter writer = new StringWriter();
                m.marshal(request, writer);
                pool.recycle(m);
                LOGGER.info(writer.toString());
            } catch (JAXBException ex) {
                LOGGER.log(Level.WARNING, "Error while marshalling the request", ex);
            }
        }
    }

    /**
     * Extract The complex parameter encoded in XML from the query.
     * If the parameter is mandatory and if it is null it throw an exception.
     * else it return null.
     *
     * @param parameterName The name of the parameter.
     * @param mandatory true if this parameter is mandatory, false if its optional.
     *
     * @return the parameter or null if not specified
     * @throws  CstlServiceException if the parameter is madatory but not present, or if an error occurs while reading xml.
     */
    protected Object getComplexParameter(final String parameterName, final boolean mandatory) throws CstlServiceException {
            final String value = getParameter(parameterName, mandatory);
            if (value != null) {
                return unmarshall(value, parameterName);
            }
            return null;
    }

    /**
     * Use the service marshaller pool to read an XML parameter value.
     *
     * @param str the xml value.
     * @param parameterName Name of the parameter currently read (used for exception message).
     *
     * @return An unmarshalled object.
     * @throws CstlServiceException if an error occurs while reading xml.
     */
    private Object unmarshall(String str, String parameterName) throws CstlServiceException {
        try {
            final StringReader sr = new StringReader(str);
            final Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();
            Object result = unmarshaller.unmarshal(sr);
            marshallerPool.recycle(unmarshaller);
            if (result instanceof JAXBElement) {
                result = ((JAXBElement)result).getValue();
            }
            return result;
        } catch (JAXBException ex) {
             throw new CstlServiceException("The xml object for parameter " + parameterName + " is not well formed:" + '\n' +
                            ex, INVALID_PARAMETER_VALUE);
        }
    }

    /**
     * Extract A list in a complex parameter encoded in XML from the query.
     * If the parameter is mandatory and if it is null it throw an exception.
     * else it return an empty list.
     *
     * @param parameterName The name of the parameter.
     * @param mandatory true if this parameter is mandatory, false if its optional.
     *
     * @return the parameter or an empty list if not specified
     * @throws  CstlServiceException if the parameter is madatory but not present, or if an error occurs while reading xml.
     */
    protected List<Object> getComplexParameterList(final String parameterName, final boolean mandatory) throws CstlServiceException {
        final String str = getParameter(parameterName, mandatory);
        if (str != null) {
            final List<Object> results = new ArrayList<>();
            final List<String> values = StringUtilities.toStringList(str);
            for (String value : values) {
                results.add(unmarshall(value, parameterName));
            }
            return results;
        }
        return Collections.EMPTY_LIST;

    }

    /**
     * Return the service URL obtain by the first request made.
     * something like : http://localhost:8080/constellation/WS
     *
     * @return the service uURL.
     */
    protected String getServiceURL() {
        String result = Application.getProperty(AppProperty.CSTL_SERVICE_URL);

        // try to build service URL from CSTL_URL property
        if (result == null) {
            String cstlURL = Application.getProperty(AppProperty.CSTL_URL);
            if (cstlURL != null) {
                cstlURL = cstlURL.endsWith("/") ? cstlURL : cstlURL + "/";
                result = cstlURL + "WS";
            }
        } else if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        // fallback to request context url
        if (result == null && httpServletRequest != null) {
            try {
                URL url = new URL(httpServletRequest.getRequestURL().toString());
                String host = url.getHost();
                String userInfo = url.getUserInfo();
                String scheme = url.getProtocol();
                int port = url.getPort();
                String path = httpServletRequest.getContextPath() + httpServletRequest.getServletPath();

                URI uri = new URI(scheme, userInfo, host, port, path, null, null);
                result = uri.toString();
            } catch (MalformedURLException | URISyntaxException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        if (result != null && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
