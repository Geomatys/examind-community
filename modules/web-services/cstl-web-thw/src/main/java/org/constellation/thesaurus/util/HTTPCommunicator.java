/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.thesaurus.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.geotoolkit.csw.xml.GetRecordById;
import org.geotoolkit.csw.xml.GetRecordsRequest;
import org.geotoolkit.ows.xml.RequestBase;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.ws.CstlServiceException;

/**
 * Provides a static method to communicate over HTTP with a remote server.
 * 
 * @author Mehdi Sidhoum (Geomatys)
 * @author Adrian Custer (Geomatys)
 * @since 2.0.4
 */
/* Prior to 2.0.5 this was HTTPUtils but we split out the other methods. */
public final class HTTPCommunicator {

    private static final Logger LOGGER = Logger.getLogger(HTTPCommunicator.class.getName());

    private HTTPCommunicator() {
    }

    public static Object sendRequest(final URL cswURL, final Object requestObj, final MarshallerPool mpool) throws CstlServiceException {
        return sendRequest(cswURL, requestObj, mpool, true);
    }

    /**
     * Send a request to a distant CSW service.
     * 
     * The request will be either getCaps, getRecord (... ?)
     *
     * @param cswURL          the identifier for the distant Catalog Service.
     * @param requestObj   an object determining the request to be made, either
     *                       a String with the info required to post a GET 
     *                       request or a JAXB object which will be POSTed.
     * @param mpool        the marshallerPool from which to obtain marshallers
     *                       and unmarshallers.
     *
     * @param verbose 
     * @return the object corresponding to the XML response of the distant web 
     *           service.
     *
     * @throws CstlServiceException if the communication failed for any reason.
     */
    /*
     * TODO: have Martin compare this to Guilhem's version for design evaluation
     * TODO: rethrow real exceptions so we can know what really happened!
     * TODO: check all (un)marshallers are released to the pool
     * TODO: check all connections are closed
     * TODO: make this generic for any web service when we have a generic WebServiceId
     * TODO: add logger tracing.
     */
    public static Object sendRequest(final URL cswURL, final Object requestObj, final MarshallerPool mpool, final boolean verbose) throws CstlServiceException {

        //getting the url
        URL sourceUrl = cswURL;

        ////////////////////////////////////////////////////////////////////////
        //0. Bail on error conditions
        ////////////////////////////////////////////////////////////////////////
        if (requestObj == null) {
            throw new NullPointerException("The request object may not be null");
        }
        if (!(requestObj instanceof String || 
                requestObj instanceof JAXBElement<?> || 
                requestObj instanceof GetRecordsRequest || 
                requestObj instanceof GetRecordById ||
                requestObj instanceof RequestBase)) {
            throw new IllegalArgumentException("The request object must be String or a service request.");
        }




        ////////////////////////////////////////////////////////////////////////
        //1. Determine if we are going to POST or GET
        ////////////////////////////////////////////////////////////////////////
        boolean post = false;
        String requestType = null;
        if (!(requestObj instanceof String)) {
            post = true;
            requestType = "GetRecords";
        } else {
            try {
                //append the url with parameters requestObj
                sourceUrl = new URL(cswURL.toString() + requestObj);
                requestType = getParameterValue("request", sourceUrl.toString());
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }



        
        ////////////////////////////////////////////////////////////////////////
        //2. Marshall the request into an XML String
        ////////////////////////////////////////////////////////////////////////
        String requestXML = null;
        if (post && mpool != null) {
            final StringWriter sw = new StringWriter();
            Marshaller m = null;
            try {
                m = mpool.acquireMarshaller();
            } catch (JAXBException jex) {
                throw new CstlServiceException("Unable to obtain a marshaller.", jex);
            }//no finally since we did not succeed.
            try {
                m.marshal(requestObj, sw);
                if (verbose) {
                    LOGGER.log(Level.INFO,"===================================================\n{0}" + '\n'
                            + "===================================================", sw.toString());
                }
            } catch (JAXBException jex) {
                final String strRequest;
                if (requestObj instanceof JAXBElement<?>) {
                    strRequest = ((JAXBElement<?>) requestObj).getValue().toString();
                } else {
                    strRequest = requestObj.toString();
                }
                throw new CstlServiceException("Unable to marshall the request: " + strRequest, jex);
            } finally {
                mpool.recycle(m);//mpool will not be null at this point.
            }
            requestXML = sw.toString();
        }

        
        ////////////////////////////////////////////////////////////////////////
        //2b. Alter the XML String to handle special cases
        ////////////////////////////////////////////////////////////////////////
//        //Martin's design.
//        Handler xmlproc = csw.getHandlerForXMLrequests();
//        if (xmlproc != null ){
//            requestXML = xmlproc.handle(requestXML);
//        }
//        //This was the first attempt.
//        requestXML = csw.filterXMLrequest(requestObj, requestXML);




        ////////////////////////////////////////////////////////////////////////
        //3. Make the request
        ////////////////////////////////////////////////////////////////////////
        URLConnection conec;
        try {
            if (verbose) {
                LOGGER.log(Level.FINEST, "Sending a request for {0}", sourceUrl);
            }
            conec = sourceUrl.openConnection();
        } catch (IOException ioex) {
            throw new CstlServiceException("Unable to connect to remote web service.", ioex);
        }

        if (post) {
            // for a POST request
            conec.setDoOutput(true);
            conec.setRequestProperty("Content-Type", "text/xml");
            OutputStreamWriter wr = null;
            try {
                wr = new OutputStreamWriter(conec.getOutputStream());
                wr.write(requestXML);
                wr.flush();
            } catch (IOException ioex) {
                throw new CstlServiceException("Unable to perform POST on remote web service.", ioex);
            }//TODO: Close the output stream!
            finally {
                if (wr != null) {
                    try {
                        wr.close();
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }
        }




        ////////////////////////////////////////////////////////////////////
        //4. Get the response
        ////////////////////////////////////////////////////////////////////
        InputStream in = null;
        try {
            /*
             * 4.1- Verify HTTP response code is 200 OK
             */
            final String httpCode = conec.getHeaderField(0);
            if (httpCode == null || httpCode.indexOf("200 OK") == -1) {
                throw new CstlServiceException("Got HTTP response code " + httpCode + "  when communicating with " + sourceUrl + "   for " + requestType);
            }

            /*
             * 4.2- Check if it is XML, first line must start with '<?xml'
             */
            conec.setReadTimeout(20000);
            in = conec.getInputStream();
            if (!in.markSupported()) {
                in = new BufferedInputStream(in);
            }
            in.mark(60);
            final StringWriter firstBlock = new StringWriter();
            final byte[] firstbuffer = new byte[60];
            firstBlock.write(new String(firstbuffer, 0, in.read(firstbuffer, 0, 60)));
            String first = firstBlock.toString();
            if (!first.startsWith("<?xml version=\"1.0\"")) {
                throw new CstlServiceException("The response when communicating with " + sourceUrl + "   for " + requestType + "  is not a valid XML format !");
            }


            /*
             * 4.3- Find encoding and convert to string,
             * @TODO at first we need to use a string here because there are several usecases to apply some fix for special catalogs
             * but it could be better to use a filter writer
             */
            String encoding = "UTF-8";
            if (first != null && first.indexOf("encoding=\"") != -1) {
                final String temp = first.substring(first.indexOf("encoding=\"") + 10);
                encoding = temp.substring(0, temp.indexOf("\""));
            }
            
            LOGGER.log(Level.FINEST, "requestType : {0}  response encoding : {1}", new Object[]{requestType, encoding});

            /*
             * 4.4- Return string or unmarshalled object depending on if the MarshallerPool mpool is null
             */
            //we must use a string because we have a fix for no standard catalogs
            //@TODO use a FilterReader/Writer to apply the fix per lines when reading the response instead of passing by a string.

            in.reset();
            InputStreamReader conv = new InputStreamReader(in, encoding);

            StringWriter out = new StringWriter();
            char[] buffer = new char[1024];
            int size;
            while ((size = conv.read(buffer, 0, 1024)) > 0) {
                out.write(buffer, 0, size);
            }
            String responseXML = out.toString();

            if (mpool == null) {
                return responseXML;
            } else {

                /*
                 * Some implementations replace the standardized namespace
                 *   "http://www.opengis.net/cat/csw" by
                 *   "http://www.opengis.net/csw"
                 * If we detect this we replace this namespace before unmarshalling the object.
                 *
                 * TODO replace even when the prefix is not "csw" or blank
                 */
                if (responseXML.contains("xmlns:csw=\"http://www.opengis.net/csw\"")) {
                    responseXML = responseXML.replace("xmlns:csw=\"http://www.opengis.net/csw\"",
                            "xmlns:csw=\"http://www.opengis.net/cat/csw\"");
                }


                // we want to handle gml 3.2.1 like we handle gml 3.1.1
                if (responseXML.contains("\"http://www.opengis.net/gml/3.2\"")) {
                    responseXML = responseXML.replace("\"http://www.opengis.net/gml/3.2\"", "\"http://www.opengis.net/gml\"");
                }
                
                ////////////////////////////////////////////////////////////////////////
                //5. Unmarshall the response to a functional object
                ////////////////////////////////////////////////////////////////////////
                Object harvested = null;
                Unmarshaller unmarshaller = null;
                try {
                    unmarshaller = mpool.acquireUnmarshaller();
                } catch (JAXBException ex) {
                    throw new CstlServiceException("Could not acquire the Unmarshaller", ex);
                }//no finally: we don't release something we never got.

                try {
                    //logger.info("request received" + decodedString);
                    harvested = unmarshaller.unmarshal(new StringReader(responseXML));
                    
                    if (harvested instanceof JAXBElement<?>) {
                        harvested = ((JAXBElement<?>) harvested).getValue();
                    }
                } catch (JAXBException ex) {
                    LOGGER.log(Level.WARNING, "The distant service does not respond correctly: unable to unmarshall response document.", ex);
                    throw new CstlServiceException("Unable to unmarshall the Catalog Service response", ex);
                } finally {
                    mpool.recycle(unmarshaller);
                }

                return harvested;
            }
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        } catch (IOException ioex) {
            throw new CstlServiceException("Unable to obtain the response from the remote web service.", ioex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }
        }
        //TODO: confirm that we do not close the connection but let it be GCed

        return null;

    }

    /**
     * This method returns the parameter value from an url, it is useful for web services requests.
     * @param param
     * @param url
     * @return
     */
    private static String getParameterValue(final String param, final String url) {
        if (param == null || url == null || url.isEmpty()) {
            return null;
        }
        final Pattern patternParam = Pattern.compile("(?i)" + param + "=");
        final Matcher matcherParam = patternParam.matcher(url);
        if (matcherParam.find()) {
            final String subst = url.substring(url.lastIndexOf(matcherParam.group()));
            String result;
            if (subst.contains("&")) {
                result = subst.substring(subst.indexOf('=') + 1, subst.indexOf('&'));
            } else {
                result = subst.substring(subst.indexOf('=') + 1);
            }
            return result;
        }
        return "";
    }
}
