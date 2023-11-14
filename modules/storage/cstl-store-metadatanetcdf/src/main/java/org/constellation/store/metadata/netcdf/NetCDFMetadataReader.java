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
package org.constellation.store.metadata.netcdf;

import java.util.*;
import org.apache.sis.xml.util.LegacyNamespaces;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.XML;
import static org.constellation.api.CommonConstants.NETCDF_EXT;
import org.constellation.api.PathType;
import org.constellation.jaxb.MarshallWarnings;
import org.constellation.metadata.utils.Utils;
import org.geotoolkit.csw.xml.v202.*;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.constellation.metadata.io.DomMetadataReader;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;

/**
 *
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.8.4
 */
public class NetCDFMetadataReader extends DomMetadataReader {

    /**
     * The directory containing the data XML files.
     */
    private final Path dataDirectory;

    /**
     * A date formatter used to display the Date object for Dublin core translation.
     */
    private static final DateFormat FORMATTER;
    static {
        FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    private final String CURRENT_EXT;

    private final boolean usePathAsIdentifier;

    private static final TimeZone TZ = TimeZone.getTimeZone("GMT+2:00");

    private Locale locale = null;

    /**
     * Build a new CSW NetCDF File Reader.
     *
     * @param configuration A generic configuration object containing a directory path
     * in the configuration.dataDirectory field.
     * @param dataDirectory
     *
     * @throws MetadataIoException If the configuration object does
     * not contains an existing directory path in the configuration.dataDirectory field.
     * If the creation of a MarshallerPool throw a JAXBException.
     */
    public NetCDFMetadataReader(Map configuration, final Path dataDirectory, final Map<String, PathType> additionalQueryable) throws MetadataIoException {
        super(true, false, additionalQueryable);
        this.dataDirectory = dataDirectory;
        if (dataDirectory == null) {
            throw new MetadataIoException("cause: unable to find the data directory", NO_APPLICABLE_CODE);
        } else if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new MetadataIoException("cause: unable to create the unexisting data directory:" + dataDirectory.toString(), NO_APPLICABLE_CODE);
            }
        }
        if (configuration == null) configuration = new HashMap();
        final String extension = (String) configuration.get("netcdfExtension");
        if (extension != null) {
            CURRENT_EXT = extension;
        } else {
            CURRENT_EXT = NETCDF_EXT;
        }
        final String usePathAsIdentifierValue = (String) configuration.get("usePathAsIdentifier");
        if (usePathAsIdentifierValue != null) {
            usePathAsIdentifier = Boolean.valueOf(usePathAsIdentifierValue);
        } else {
            usePathAsIdentifier = false;
        }
        if (configuration.get("enable-thread") != null) {
            final boolean t = Boolean.parseBoolean((String) configuration.get("enable-thread"));
            if (t) {
                LOGGER.info("parrallele treatment enabled");
            }
            setIsThreadEnabled(t);
        }
        if (configuration.get("enable-cache") != null) {
            final boolean c = Boolean.parseBoolean((String) configuration.get("enable-cache"));
            if (!c) {
                LOGGER.info("cache system have been disabled");
            }
            setIsCacheEnabled(c);
        }
        final String localeString = (String) configuration.get("locale");
        if (localeString != null) {
            locale = Locale.forLanguageTag(localeString);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
        Object obj = null;
        if (isCacheEnabled()) {
            obj = getFromCache(identifier);
        }
        if (obj == null) {
            obj = getObjectFromFile(identifier);
        }
        MetadataType metadataMode;

        if (obj instanceof DefaultMetadata) {
            metadataMode = MetadataType.ISO_19115;
        } else if (obj instanceof RecordType) {
            metadataMode = MetadataType.DUBLINCORE_CSW202;
        } else if (obj instanceof org.geotoolkit.csw.xml.v300.RecordType) {
            metadataMode = MetadataType.DUBLINCORE_CSW300;
        } else {
            metadataMode = MetadataType.NATIVE;
        }
        // marshall to DOM
        if (obj != null) {
            final Node metadataNode = writeObjectInNode(obj);
            final Node n = convertAndApplyElementSet(metadataMode, mode, type, elementName, metadataNode);
            return new RecordInfo(identifier, n, metadataMode, mode);
        }
        return null;
    }

    @Override
    public boolean existMetadata(final String identifier) throws MetadataIoException {
        final Path metadataFile;
        if (usePathAsIdentifier) {
            metadataFile = getFileFromPathIdentifier(identifier, dataDirectory, CURRENT_EXT);
        } else {
            metadataFile = getFileFromIdentifier(identifier, dataDirectory, CURRENT_EXT);
        }
        return metadataFile != null && Files.exists(metadataFile);
    }

    /**
     * Try to find a file named identifier.nc or identifier recursively
     * in the specified directory and its sub-directories.
     *
     * @param identifier The metadata identifier.
     * @param directory The current directory to explore.
     * @param ext file extension.
     */
    public static Path getFileFromIdentifier(final String identifier, final Path directory, final String ext) {
        // 1) try to find the file in the current directory
        Path metadataFile = directory.resolve(identifier + ext);
        // 2) trying without the extension
        if (!Files.exists(metadataFile)) {
            metadataFile = directory.resolve(identifier);
        }
        // 3) trying by replacing ':' by '-' (for windows platform who don't accept ':' in file name)
        if (!Files.exists(metadataFile)) {
            final String windowsIdentifier = identifier.replace(':', '-');
            metadataFile = directory.resolve(windowsIdentifier + ext);
        }

        if (Files.exists(metadataFile)) {
            return metadataFile;
        } else {

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
                for (Path child : dirStream) {
                    if (Files.isDirectory(child)) {
                        final Path result = getFileFromIdentifier(identifier, child, ext);
                        if (result != null && Files.exists(result)) {
                            return result;
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "An error occurs during metadata search on data directory.", ex);
                return null;
            }
        }
        return null;
    }

    /**
     * Try to find a file named identifier.nc or identifier recursively
     * in the specified directory and its sub-directories.
     *
     * @param identifier The metadata identifier.
     * @param directory The current directory to explore.
     * @param ext File extension.
     */
    public static Path getFileFromPathIdentifier(final String identifier, final Path directory, final String ext) {

        // if where are in the final directory
        if (identifier.indexOf(':') == -1) {
            // 1) try to find the file in the current directory
            Path metadataFile = directory.resolve(identifier + ext);
            // 2) trying without the extension
            if (!Files.exists(metadataFile)) {
                metadataFile = directory.resolve(identifier);
            }
            // 3) trying by replacing ':' by '-' (for windows platform who don't accept ':' in file name)
            if (!Files.exists(metadataFile)) {
                final String windowsIdentifier = identifier.replace(':', '-');
                metadataFile = directory.resolve(windowsIdentifier + ext);
            }

            if (Files.exists(metadataFile)) {
                return metadataFile;
            } else {
                LOGGER.warning("unable to find the metadata:" + identifier + " in the directory:" + directory.toAbsolutePath().toString());
                return null;
            }
        } else {
            final int separator = identifier.indexOf(':');
            final String directoryName = identifier.substring(0, separator);
            final Path child = directory.resolve(directoryName);
            if (Files.isDirectory(child)) {
                final String childIdentifier = identifier.substring(separator + 1);
                return getFileFromPathIdentifier(childIdentifier, child, ext);
            } else {
                LOGGER.log(Level.WARNING, "{0} is not a  directory.", child.toAbsolutePath().toString());
                return null;
            }
        }
    }

    /**
     * Unmarshall The file designed by the path dataDirectory/identifier.nc
     * If the file is not present or if it is impossible to unmarshall it it return an exception.
     *
     * @param identifier the metadata identifier
     * @return A unmarshalled metadata object.
     */
    private Object getObjectFromFile(final String identifier) throws MetadataIoException {
        final Path metadataFile;
        if (usePathAsIdentifier) {
            metadataFile = getFileFromPathIdentifier(identifier, dataDirectory, CURRENT_EXT);
        } else {
            metadataFile = getFileFromIdentifier(identifier, dataDirectory, CURRENT_EXT);
        }
        if (metadataFile != null && Files.exists(metadataFile)) {

            final DataStoreProvider factory = DataStores.getProviderById("NetCDF");
            LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
            final StorageConnector sc = new StorageConnector(metadataFile);
            try (DataStore store = factory.open(sc)){
                Object obj = store.getMetadata();
                if (obj instanceof DefaultMetadata) {
                    MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
                    obj = (DefaultMetadata) copier.copy(Metadata.class, (DefaultMetadata)obj);
                    ((DefaultMetadata)obj).setFileIdentifier(identifier);
                } else {
                    Utils.setIdentifier(identifier, obj);
                }
                return obj;

            } catch (DataStoreException | IllegalArgumentException ex) {
                throw new MetadataIoException("The netcdf file : " + metadataFile.getFileName().toString() + " can not be read\ncause: " + ex.getMessage(), ex, INVALID_PARAMETER_VALUE);
            } finally {
                try {
                    sc.closeAllExcept(null);
                } catch (DataStoreException e) {
                    LOGGER.warning("A storage connector cannot be properly closed: "+e.getMessage());
                }
            }
        }
        throw new MetadataIoException("The netcdf file : " + identifier + ".nc is not present", INVALID_PARAMETER_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] executeEbrimSQLQuery(final String sqlQuery) throws MetadataIoException {
        throw new MetadataIoException("Ebrim query are not supported int the FILESYSTEM mode.", OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RecordInfo> getAllEntries() throws MetadataIoException {
        return getAllEntries(dataDirectory, null);
    }

    private List<RecordInfo> getAllEntries(final Path directory, final String parentIdentifierPrefix) throws MetadataIoException {
        final String identifierPrefix = computeIdentifierPrefix(directory, parentIdentifierPrefix);
        final List<RecordInfo> results = new ArrayList<>();
        //if (locale != null) {
        //    reader.setLocale(locale);
        //}

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path metadataFile : stream) {
                final String fileName = metadataFile.getFileName().toString();
                if (fileName.endsWith(CURRENT_EXT)) {
                    final String identifier = computeIdentifier(fileName, identifierPrefix);
                    results.add(getMetadata(identifier, MetadataType.NATIVE));
                } else if (Files.isDirectory(metadataFile)) {
                    results.addAll(getAllEntries(metadataFile, identifierPrefix));
                } else {
                    //do not throw exception just skipping
                    //throw new MetadataIoException(METAFILE_MSG + f.getPath() + " does not ands with " + CURRENT_EXT + " or is not a directory", INVALID_PARAMETER_VALUE);
                }
            }
        } catch (IOException e) {
            //TODO should we rise a MetadataIoException instead ?
            LOGGER.log(Level.WARNING, "An error occurs during directory scanning", e);
        }
        return results;
    }

    @Override
    public Iterator<String> getIdentifierIterator() throws MetadataIoException {
        final List<String> results = getAllIdentifiers();
        return results.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        return getAllIdentifiers(dataDirectory, null);
    }

    @Override
    public int getEntryCount() throws MetadataIoException {
        return getAllIdentifiers(dataDirectory, null).size();
    }

    /**
     * find recursively the files names used as record identifier.
     */
    private List<String> getAllIdentifiers(final Path directory, final String parentIdentifierPrefix) throws MetadataIoException {
        final String identifierPrefix = computeIdentifierPrefix(directory, parentIdentifierPrefix);
        final List<String> results = new ArrayList<>();
        if (directory != null && Files.isDirectory(directory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path metadataFile : stream) {
                    final String fileName = metadataFile.getFileName().toString();
                    if (fileName.endsWith(CURRENT_EXT)) {
                        results.add(computeIdentifier(fileName, identifierPrefix));
                    } else if (Files.isDirectory(metadataFile)) {
                        results.addAll(getAllIdentifiers(metadataFile, identifierPrefix));
                    } else {
                        //do not throw exception just skipping
                        //throw new MetadataIoException(METAFILE_MSG + f.getPath() + " does not ands with " + CURRENT_EXT + " or is not a directory", INVALID_PARAMETER_VALUE);
                    }
                }
            } catch (IOException e) {
                //TODO should we rise a MetadataIoException instead ?
                LOGGER.log(Level.WARNING, "An error occurs during directory scanning", e);
            }
        }
        return results;
    }

    private String computeIdentifier(final String fileName, final String identifierPrefix) {
        if (usePathAsIdentifier) {
            return identifierPrefix + ':' + fileName.substring(0, fileName.lastIndexOf(CURRENT_EXT));
        } else {
            return fileName.substring(0, fileName.lastIndexOf(CURRENT_EXT));
        }
    }

    private String computeIdentifierPrefix(final Path directory, final String identifierPrefix) {
        if (usePathAsIdentifier) {
            if (identifierPrefix == null) {
                return "";
            } else {
                return identifierPrefix + ':' + directory.getFileName().toString();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetadataType> getSupportedDataTypes() {
        return Arrays.asList(
                MetadataType.ISO_19115,
                MetadataType.DUBLINCORE_CSW202,
                MetadataType.DUBLINCORE_CSW300,
                MetadataType.EBRIM_300,
                MetadataType.ISO_19110);
    }

    protected Node writeObjectInNode(final Object obj) throws MetadataIoException {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            NodeUtilities.secureFactory(dbf);//NOSONAR
            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            final Document document = docBuilder.newDocument();
            final Marshaller marshaller = EBRIMMarshallerPool.getInstance().acquireMarshaller();
            final MarshallWarnings warnings = new MarshallWarnings();
            marshaller.setProperty(XML.CONVERTER, warnings);
            marshaller.setProperty(XML.TIMEZONE, TZ);
//          marshaller.setProperty(LegacyNamespaces.APPLY_NAMESPACE_REPLACEMENTS, replace);
            marshaller.setProperty(XML.GML_VERSION, LegacyNamespaces.VERSION_3_2_1);
            if (locale != null) {
                marshaller.setProperty(XML.LOCALE, locale);
            }
            marshaller.marshal(obj, document);

            return document.getDocumentElement();
        } catch (ParserConfigurationException | JAXBException ex) {
            throw new MetadataIoException(ex);
        }
    }
}
