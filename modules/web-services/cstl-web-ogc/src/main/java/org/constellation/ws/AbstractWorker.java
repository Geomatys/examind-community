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
package org.constellation.ws;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.sis.util.privy.UnmodifiableArrayList;
import org.apache.sis.util.Version;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.api.WorkerState;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.exception.ConfigurationException;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.security.SimplePDP;
import org.geotoolkit.ows.xml.AbstractCapabilitiesCore;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import org.geotoolkit.util.StringUtilities;
import org.opengis.util.CodeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

/**
 * Abstract definition of a {@code Web Map Service} worker called by a facade
 * to perform the logic for a particular WMS instance.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 * 
 * @param <A> Specific worker configuration object class.
 */
public abstract class AbstractWorker<A extends AbstractConfigurationObject> implements Worker {

    /**
     * The default logger.
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.ws");

    /**
     * A flag indicating if the worker is correctly started.
     */
    private WorkerState currentState;

    /**
     * A message keeping the reason of the start error of the worker.
     */
    protected String startError;

    /**
     * Contains the service url used in capabilities document.
     */
    protected String serviceUrl = null;

    /**
     * A map containing the Capabilities Object already loaded from file.
     */
    private final Map<String, Details> capabilities = Collections.synchronizedMap(new HashMap<>());

    /**
     * Output responses of a GetCapabilities request.
     */
    private static final Map<String,AbstractCapabilitiesCore> CAPS_RESPONSE = new HashMap<>();

    /**
     * The identifier of the worker.
     */
    protected final String id;

    private final Integer serviceId;

    /**
     * The specification for this worker.
     */
    protected final Specification specification;

    protected UnmodifiableArrayList<ServiceDef> supportedVersions;

    /**
     * use this flag to enable cache for capabilities document.
     */
    protected boolean cacheCapabilities = true;

    /**
     * A Policy Decision Point (PDP) if some security constraints have been defined.
     */
    protected SimplePDP pdp = null;

    private List<Schema> schemas = null;

    private long currentUpdateSequence = System.currentTimeMillis();

    @Autowired
    protected IServiceBusiness serviceBusiness;

    @Autowired
    private IWSEngine wsengine;

    protected A configuration;

    protected boolean isTransactional;

    private final boolean acceptNullConfig;

    public AbstractWorker(final String id, final Specification specification) {
        this(id, specification, false);
    }

    public AbstractWorker(final String id, final Specification specification, final boolean acceptNullConfig) {
        this.id = id;
        this.acceptNullConfig = acceptNullConfig;
        this.specification = specification;
        SpringHelper.injectDependencies(this);
        this.serviceId = serviceBusiness.getServiceIdByIdentifierAndType(specification.name(), id);
        start();
        try {
            applySupportedVersion();
            //we look if the configuration have been specified
            configuration = (A) serviceBusiness.getConfiguration(specification.name(), id);
            if (!acceptNullConfig && configuration == null) {
                startError("The configuration object is null.", null);
            }
            this.isTransactional = getTransactionalProperty();
        } catch (ClassCastException ex) {
            startError("The configuration object is malformed.", null);
        } catch (ConfigurationException ex) {
            startError(ex.getMessage(), ex);
        }
    }

    /**
     * this method initialize the supproted version of the worker.
     *
     * @throws org.constellation.ws.CstlServiceException if a version in the property "supported_versions" is not supported.
     */
    private void applySupportedVersion() throws ConfigurationException {
        final ServiceComplete service = serviceBusiness.getServiceById(serviceId, null);
        if (service != null) {
            final List<ServiceDef> definitions = new ArrayList<>();
            final StringTokenizer tokenizer = new StringTokenizer(service.getVersions(), "µ");
            while (tokenizer.hasMoreTokens()) {
                final String version = tokenizer.nextToken();
                final ServiceDef def = ServiceDef.getServiceDefinition(specification, version);
                if (def == null) {
                    throw new ConfigurationException("Unable to find a service specification for:" + specification.name() + " version:" + version);
                } else {
                    definitions.add(def);
                }
            }
            setSupportedVersion(definitions);
        } else {
            setSupportedVersion(ServiceDef.getAllSupportedVersionForSpecification(specification));
        }
    }

    /**
     * Record the cause of the worker startup fail.
     *
     * @param msg An error message explaning the cause of the dailure to start.
     * @param ex
     */
    protected final void startError(final String msg, final Throwable ex) {
        startError = msg;
        currentState = WorkerState.ERROR;
        wsengine.updateWorkerStatus(specification.name(), id, currentState);
        LOGGER.log(Level.WARNING, "\nThe " + this.specification.name() + " worker is not running!\ncause: {0}", startError);
        if (ex != null) {
            LOGGER.log(Level.FINER, "\nThe " + this.specification.name() + " worker is not running!", ex);
        }
    }

    private void start() {
        this.currentState = WorkerState.STARTING;
        wsengine.updateWorkerStatus(specification.name(), id, currentState);
    }

    protected final void started() {
        if (!currentState.equals(WorkerState.ERROR)) {
            LOGGER.info(specification.name() + " worker \"" + id + "\" running\n");
            currentState = WorkerState.UP;
            wsengine.updateWorkerStatus(specification.name(), id, currentState);
        }
    }

    protected final void stopped() {
        this.currentState = WorkerState.DOWN;
        wsengine.updateWorkerStatus(specification.name(), id, currentState);
    }

    protected String getUserLogin() {
        final String userLogin = SecurityManagerHolder.getInstance().getCurrentUserLogin();
        return userLogin;
    }

    private void setSupportedVersion(final List<ServiceDef> supportedVersions) {
         this.supportedVersions = UnmodifiableArrayList.wrap(supportedVersions.toArray(new ServiceDef[supportedVersions.size()]));
    }

    protected boolean isSupportedVersion(final String version) {
        final Version vv = new Version(version);
        for (ServiceDef sd : supportedVersions) {
            if (sd.version.equals(vv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify if the version is supported by this serviceType.
     * <p>
     * If the version is not accepted we send an exception.
     * </p>
     */
    @Override
    public void checkVersionSupported(final String versionNumber, final boolean getCapabilities) throws CstlServiceException {
        if (getVersionFromNumber(versionNumber) == null) {
            final StringBuilder messageb = new StringBuilder("The parameter ");
            for (ServiceDef vers : supportedVersions) {
                messageb.append("VERSION=").append(vers.version.toString()).append(" OR ");
            }
            messageb.delete(messageb.length()-4, messageb.length()-1);
            messageb.append(" must be specified");
            final CodeList code;
            if (getCapabilities) {
                code = VERSION_NEGOTIATION_FAILED;
            } else {
                code = INVALID_PARAMETER_VALUE;
            }
            throw new CstlServiceException(messageb.toString(), code, "version");
        }
    }

    /**
     * Return a Version Object from the version number.
     * if the version number is not correct return the default version.
     *
     * @param number the version number.
     * @return
     */
    @Override
    public ServiceDef getVersionFromNumber(final Version number) {
        if (number != null) {
            for (ServiceDef v : supportedVersions) {
                if (v.version.toString().equals(number.toString())){
                    return v;
                }
            }
        }
        return null;
    }

    /**
     * Return a Version Object from the version number.
     * if the version number is not correct return the default version.
     *
     * @param number the version number.
     * @return
     *
     */
    @Override
    public ServiceDef getVersionFromNumber(final String number) {
        for (ServiceDef v : supportedVersions) {
            if (v.version.toString().equals(number)){
                return v;
            }
        }
        return null;
    }

    /**
     * If the requested version number is not available we choose the best version to return.
     *
     * @param number A version number, which will be compared to the ones specified.
     *               Can be {@code null}, in this case the best version specified is just returned.
     * @return The best version (the highest one) specified for this web service.
     *
     */
    @Override
    public ServiceDef getBestVersion(final String number) {
        for (ServiceDef v : supportedVersions) {
            if (v.version.toString().equals(number)){
                return v;
            }
        }
        final ServiceDef firstSpecifiedVersion = supportedVersions.get(0);
        if (number == null || number.isEmpty()) {
            return firstSpecifiedVersion;
        }
        final Version wrongVersion = new Version(number);
        if (wrongVersion.compareTo(firstSpecifiedVersion.version) > 0) {
            return firstSpecifiedVersion;
        } else {
            if (wrongVersion.compareTo(supportedVersions.get(supportedVersions.size() - 1).version) < 0) {
                return supportedVersions.get(supportedVersions.size() - 1);
            }
        }
        return firstSpecifiedVersion;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized void setServiceUrl(final String serviceBaseUrl) {
        if (serviceBaseUrl != null) {
            serviceUrl = serviceBaseUrl;
            String separator = serviceUrl.endsWith("/") ? "" : "/";
            serviceUrl = serviceUrl + separator + specification.toString().toLowerCase() + '/' + id + '?';
        }
    }

    /**
     * return the current service URL.
     * @return
     */
    @Override
    public synchronized String getServiceUrl(){
        return serviceUrl;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final Integer getServiceId() {
        return serviceId;
    }

    protected String getProperty(final String propertyName) {
        if (configuration != null) {
            return configuration.getProperty(propertyName);
        }
        return null;
    }

    protected final boolean getBooleanProperty(final String propertyName, boolean defaultValue) {
        if (configuration != null) {
            String value = configuration.getProperty(propertyName);
            if (value != null) {
                return Boolean.parseBoolean(value);
            }
        }
        return defaultValue;
    }

    protected final Integer getIntegerProperty(final String propertyName, Integer defaultValue) {
        if (configuration != null) {
            String value = configuration.getProperty(propertyName);
            if (value != null) {
                return Integer.parseInt(value);
            }
        }
        return defaultValue;
    }

    /**
     * Extract the transactional profile of the service.
     *
     * @return {@code true} if the transactional profile is activated.
     */
    protected boolean getTransactionalProperty() {
        final String isTransactionnalProp = getProperty(TRANSACTIONAL);
        // 1) priority to configuration parameters properties
        if (isTransactionnalProp != null) {
           return Boolean.parseBoolean(isTransactionnalProp);
        } else {
            // 2) look into instance details
            boolean t = false;
            try {
                final Details details = serviceBusiness.getInstanceDetails(specification.toString().toLowerCase(), id, null);
                // default to false
                if (details != null) {
                    t = details.isTransactional();
                }
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, "Cannot determine if service is transactional", ex);
            }
            return t;
        }
    }

    /**
     * Returns the file where to read the capabilities document for each service.
     * If no such file is found, then this method returns {@code null}.
     * This method has a cache system, the object will be read from the file system only one time.
     *
     * @param service The service type identifier. example "WMS"
     * @param language The language of the metadata.
     *
     * @return The capabilities Object, or {@code null} if none.
     *
     * @throws CstlServiceException if an error occurs during the unmarshall of the document.
     */
    protected Details getStaticCapabilitiesObject(final String service, final String language) throws CstlServiceException {
        final String key;
        if (language == null) {
            key = getId() + service;
        } else {
            key = getId() + service + "-" + language;
        }
        Details details = capabilities.get(key);
        if (details == null) {
            try {
                details = serviceBusiness.getInstanceDetails(service, getId(), language);
                capabilities.put(key, details);
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, "An error occurred when trying to read the service metadata. Returning default capabilities.", ex);
            }
        }
        return details;
    }

    /**
     * Throw and exception if the service is not working
     *
     * @throws org.constellation.ws.CstlServiceException
     */
    protected void isWorking() throws CstlServiceException {
        switch (currentState) {
            case ERROR:    throw new CstlServiceException("The service is not running.\nCause:" + startError, OWSExceptionCode.NO_APPLICABLE_CODE);
            case DOWN:     throw new CstlServiceException("The service has been shutdown.", OWSExceptionCode.NO_APPLICABLE_CODE);
            case STARTING: throw new CstlServiceException("The service is starting up. retry later", OWSExceptionCode.NO_APPLICABLE_CODE);
            case UNKNOWN:  throw new CstlServiceException("The service is not running, Unknown Cause.", OWSExceptionCode.NO_APPLICABLE_CODE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkerState getState() {
        return currentState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPostRequestLog() {
        final String value = getProperty("postRequestLog");
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrintRequestParameter() {
        final String value = getProperty("printRequestParameter");
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    /**
     * A flag indicating if the transaction methods of the worker are securized.
     */
    protected boolean isTransactionSecurized() {
        final String value = getProperty(TRANSACTION_SECURIZED);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return true;
    }

    @Override
    public boolean isRequestValidationActivated() {
        final String value = getProperty("requestValidationActivated");
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    @Override
    public List<Schema> getRequestValidationSchema() {
        if (schemas == null) {
            final String value = getProperty("requestValidationSchema");
            schemas = new ArrayList<>();
            if (value != null) {
                final List<String> schemaPaths = StringUtilities.toStringList(value);
                LOGGER.info("Reading schemas. This may take some times ...");
                try {
                    final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                    sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    for (String schemaPath : schemaPaths) {
                        LOGGER.log(Level.INFO, "Reading {0}", schemaPath);
                        try {
                            schemas.add(sf.newSchema(new URL(schemaPath)));
                        } catch (SAXException ex) {
                            LOGGER.warning("SAX exception while adding the Validator to the JAXB unmarshaller");
                        } catch (MalformedURLException ex) {
                            LOGGER.warning("MalformedURL exception while adding the Validator to the JAXB unmarshaller");
                        }
                    }
                } catch (SAXException ex) {
                    LOGGER.log(Level.WARNING, "SAX exception while setting security property", ex);
                }
            }
        }
        return schemas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(final String ip, final String referer) {
        if (pdp == null) return true;
        return pdp.isAuthorized(ip, referer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return (pdp != null);
    }

    /**
     * @return the currentUpdateSequence
     */
    protected String getCurrentUpdateSequence() {
        return Long.toString(currentUpdateSequence);
    }

    /**
     * Set the current date to the updateSequence parameter
     */
    @Override
    public void refreshUpdateSequence() {
        currentUpdateSequence = System.currentTimeMillis();
    }

    protected boolean returnUpdateSequenceDocument(final String updateSequence) throws CstlServiceException {
        if (updateSequence == null) {
            return false;
        }
        try {
            final long sequenceNumber = Long.parseLong(updateSequence);
            if (sequenceNumber == currentUpdateSequence) {
                return true;
            } else if (sequenceNumber > currentUpdateSequence) {
                throw new CstlServiceException("The update sequence parameter is invalid (higher value than the current)", OWSExceptionCode.INVALID_UPDATE_SEQUENCE, "updateSequence");
            }
            return false;
        } catch(NumberFormatException ex) {
            throw new CstlServiceException("The update sequence must be an integer", ex, OWSExceptionCode.INVALID_PARAMETER_VALUE, "updateSequence");
        }
    }

    /**
     * Return a cached capabilities response.
     *
     * @param version
     * @return r
     */
    protected AbstractCapabilitiesCore getCapabilitiesFromCache(final String version, final String language) {
        final String keyCache = specification.name() + '-' + id + '-' + version + '-' + language;
        AbstractCapabilitiesCore cachedCapabilities = CAPS_RESPONSE.get(keyCache);
        if (cachedCapabilities != null) {
            cachedCapabilities.updateURL(getServiceUrl());
        }
        return cachedCapabilities;
    }

    /**
     * Add the capabilities object to the cache.
     *
     * @param version
     * @param language
     * @param capabilities
     */
    protected void putCapabilitiesInCache(final String version, final String language, final AbstractCapabilitiesCore capabilities) {
        if (cacheCapabilities) {
            final String keyCache = specification.name() + '-' + id + '-' + version + '-' + language;
            CAPS_RESPONSE.put(keyCache, capabilities);
        }
    }

    /**
     * Reset the capabilities cache.
     */
    @Override
    public void clearCapabilitiesCache() {
        final List<String> toClear = new ArrayList<>();
        for (String key: CAPS_RESPONSE.keySet()) {
            if (key.startsWith(specification.name() + '-')) {
                toClear.add(key);
            }
        }
        for (String key : toClear) {
            CAPS_RESPONSE.remove(key);
        }
    }

    @Override
    public void destroy() {
        this.currentState = WorkerState.STOPPING;
        wsengine.updateWorkerStatus(specification.name(), id, currentState);
        clearCapabilitiesCache();
    }

    @Override
    public A getConfiguration() {
        return configuration;
    }

    @Override
    public Object getCapabilities(String version) throws CstlServiceException {
        return null;
    }

    protected void assertTransactionnal(final String requestName) throws CstlServiceException {
        if (!isTransactional) {
            throw new CstlServiceException("The operation " + requestName + " is not supported by the service",
                    INVALID_PARAMETER_VALUE, "request");
        }
        if (isTransactionSecurized() && !SecurityManagerHolder.getInstance().isAuthenticated()) {
            throw new UnauthorizedException("You must be authentified to perform " + requestName + " request.");
        }
    }
}
