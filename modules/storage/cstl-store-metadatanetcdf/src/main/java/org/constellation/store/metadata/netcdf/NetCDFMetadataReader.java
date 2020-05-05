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
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.XML;
import static org.constellation.api.CommonConstants.NETCDF_EXT;
import org.constellation.api.PathType;
import org.constellation.jaxb.MarshallWarnings;
import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_QUERYABLE;
import org.constellation.metadata.utils.Utils;
import org.constellation.store.metadata.CSWMetadataReader;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.*;
import static org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory.*;
import org.geotoolkit.dublincore.xml.v2.elements.SimpleLiteral;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.metadata.AbstractMetadataReader;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.*;
import org.geotoolkit.ows.xml.v100.BoundingBoxType;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.distribution.Distribution;
import org.opengis.metadata.distribution.Distributor;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.util.InternationalString;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.constellation.util.NodeUtilities;
import org.constellation.util.XpathUtils;
import org.geotoolkit.csw.xml.AbstractRecord;
import org.geotoolkit.csw.xml.Record;
import org.geotoolkit.csw.xml.Settable;

import static org.geotoolkit.metadata.TypeNames.METADATA_QNAME;
import static org.geotoolkit.dublincore.xml.v2.terms.ObjectFactory._Abstract_QNAME;
import static org.geotoolkit.dublincore.xml.v2.terms.ObjectFactory._Modified_QNAME;
import org.geotoolkit.metadata.RecordInfo;
import static org.geotoolkit.ows.xml.v100.ObjectFactory._BoundingBox_QNAME;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;

/**
 *
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.8.4
 */
public class NetCDFMetadataReader extends AbstractMetadataReader implements CSWMetadataReader {

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
    public NetCDFMetadataReader(Map configuration, final Path dataDirectory) throws MetadataIoException {
        super(true, false);
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
             if (mode == MetadataType.DUBLINCORE_CSW202 || mode == MetadataType.DUBLINCORE_CSW300) {
                obj = translateISOtoDC((DefaultMetadata)obj, type, elementName, mode);
             }
        } else if (obj instanceof RecordType) {
            metadataMode = MetadataType.DUBLINCORE_CSW202;
             if (mode == MetadataType.DUBLINCORE_CSW202) {
                obj = applyElementSet((RecordType)obj, type, elementName);
             }
        } else if (obj instanceof org.geotoolkit.csw.xml.v300.RecordType) {
            metadataMode = MetadataType.DUBLINCORE_CSW300;
            if (mode == MetadataType.DUBLINCORE_CSW300) {
                obj = applyElementSet((RecordType)obj, type, elementName);
            }
        } else {
            metadataMode = MetadataType.NATIVE;
        }

        // marshall to DOM
        if (obj != null) {
            return new RecordInfo(identifier, writeObjectInNode(obj, mode), metadataMode, mode);
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
            }
        }
        throw new MetadataIoException("The netcdf file : " + identifier + ".nc is not present", INVALID_PARAMETER_VALUE);
    }

    /**
     * Apply the elementSet (Brief, Summary or full) or the custom elementSetName on the specified record.
     *
     * @param record A dublinCore record.
     * @param type The ElementSetType to apply on this record.
     * @param elementName A list of QName corresponding to the requested attribute. this parameter is ignored if type is not null.
     *
     * @return A record object.
     * @throws MetadataIoException If the type and the element name are null.
     */
    private Record applyElementSet(final Record record, final ElementSetType type, final List<QName> elementName) throws MetadataIoException {

        if (type != null) {
            switch (type) {
                case SUMMARY:
                    if (record instanceof Settable) {
                        return (Record) ((Settable)record).toSummary();
                    }
                case BRIEF:
                    if (record instanceof Settable) {
                        return (Record) ((Settable)record).toBrief();
                    }
                default:
                    return record;
            }
        } else if (elementName != null) {
            final RecordType customRecord = new RecordType();
            for (QName qn : elementName) {
                if (qn != null) {
                    try {
                        final Method getter = ReflectionUtilities.getGetterFromName(qn.getLocalPart(), RecordType.class);
                        final Object param  = ReflectionUtilities.invokeMethod(record, getter);

                        final Method setter;
                        if (param != null) {
                            setter = ReflectionUtilities.getSetterFromName(qn.getLocalPart(), param.getClass(), RecordType.class);
                        } else {
                            continue;
                        }

                        if (setter != null) {
                            ReflectionUtilities.invokeMethod(setter, customRecord, param);
                        } else {
                            final String paramDesc = param.getClass().getSimpleName();
                            LOGGER.warning("No setter have been found for attribute " + qn.getLocalPart() +" of type " + paramDesc + " in the class RecordType");
                        }

                    } catch (IllegalArgumentException ex) {
                        LOGGER.log(Level.WARNING, "illegal argument exception while invoking the method for attribute{0} in the classe RecordType", qn.getLocalPart());
                    }
                } else {
                    LOGGER.warning("An elementName was null.");
                }
            }
            return customRecord;
        } else {
            throw new MetadataIoException("No ElementSet or Element name specified");
        }
    }

    private AbstractRecord translateISOtoDC(final DefaultMetadata metadata, final ElementSetType type, final List<QName> elementName, MetadataType mode) {
        switch(mode) {
            case DUBLINCORE_CSW202: return translateISOtoDC200(metadata, type, elementName);
            case DUBLINCORE_CSW300: return translateISOtoDC300(metadata, type, elementName);
            default: throw new IllegalArgumentException("Unexpected dublincore version");
        }
    }
    /**
     * Translate A ISO 19139 object into a DublinCore representation.
     * The elementSet (Brief, Summary or full) or the custom elementSetName is applied.
     */
    private AbstractRecordType translateISOtoDC200(final DefaultMetadata metadata, final ElementSetType type, final List<QName> elementName) {
        if (metadata != null) {

            final RecordType customRecord = new RecordType();

            /*
             * BRIEF part
             */
            final SimpleLiteral identifier = new SimpleLiteral(metadata.getFileIdentifier());
            if (elementName != null && elementName.contains(_Identifier_QNAME)) {
                customRecord.setIdentifier(identifier);
            }

            SimpleLiteral title = null;
            //TODO see for multiple identification
            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification.getCitation() != null && identification.getCitation().getTitle() != null) {
                    title = new SimpleLiteral(identification.getCitation().getTitle().toString());
                }
            }
            if (elementName != null && elementName.contains(_Title_QNAME)) {
                customRecord.setTitle(title);
            }

            SimpleLiteral dataType = null;
            //TODO see for multiple hierarchyLevel
            for (ScopeCode code: metadata.getHierarchyLevels()) {
                dataType = new SimpleLiteral(code.identifier());
            }
            if (elementName != null && elementName.contains(_Type_QNAME)) {
                customRecord.setType(dataType);
            }

            final List<BoundingBoxType> bboxes = new ArrayList<>();

            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification instanceof DataIdentification) {
                    final DataIdentification dataIdentification = (DataIdentification) identification;
                    for (Extent extent : dataIdentification.getExtents()) {
                        for (GeographicExtent geoExtent :extent.getGeographicElements()) {
                            if (geoExtent instanceof GeographicBoundingBox) {
                                final GeographicBoundingBox bbox = (GeographicBoundingBox) geoExtent;
                                // TODO find CRS
                                bboxes.add(new BoundingBoxType("EPSG:4326",
                                                                bbox.getWestBoundLongitude(),
                                                                bbox.getSouthBoundLatitude(),
                                                                bbox.getEastBoundLongitude(),
                                                                bbox.getNorthBoundLatitude()));
                            }
                        }
                    }
                }
            }
            if (elementName != null && elementName.contains(_BoundingBox_QNAME)) {
                customRecord.setSimpleBoundingBox(bboxes);
            }

            if (type != null && type.equals(ElementSetType.BRIEF))
                return new BriefRecordType(identifier, title, dataType, bboxes);

            /*
             *  SUMMARY part
             */
            final List<SimpleLiteral> abstractt = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification.getAbstract() != null) {
                    abstractt.add(new SimpleLiteral(identification.getAbstract().toString()));
                }
            }
            if (elementName != null && elementName.contains(_Abstract_QNAME)) {
                customRecord.setAbstract(abstractt);
            }

            final List<SimpleLiteral> subjects = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Keywords kw :identification.getDescriptiveKeywords()) {
                    for (InternationalString str : kw.getKeywords()) {
                        subjects.add(new SimpleLiteral(str.toString()));
                    }
                }
                if (identification instanceof DataIdentification) {
                    final DataIdentification dataIdentification = (DataIdentification) identification;
                    for (TopicCategory tc : dataIdentification.getTopicCategories()) {
                        subjects.add(new SimpleLiteral(tc.identifier()));
                    }
                }
            }
            if (elementName != null && elementName.contains(_Subject_QNAME)) {
                customRecord.setSubject(subjects);
            }


            List<SimpleLiteral> formats = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Format f :identification.getResourceFormats()) {
                    if (f == null || f.getName() == null) {
                        continue;
                    }
                    formats.add(new SimpleLiteral(f.getName().toString()));
                }
            }
            if (formats.isEmpty()) {
                formats = null;
            }
            if (elementName != null && elementName.contains(_Format_QNAME)) {
                customRecord.setFormat(formats);
            }

            final SimpleLiteral modified;
            if (metadata.getDateStamp() != null) {
                String dateValue;
                synchronized (FORMATTER) {
                    dateValue = FORMATTER.format(metadata.getDateStamp());
                }
                dateValue = dateValue.substring(0, dateValue.length() - 2);
                dateValue = dateValue + ":00";
                modified = new SimpleLiteral(dateValue);
                if (elementName != null && elementName.contains(_Modified_QNAME)) {
                    customRecord.setModified(modified);
                }
            } else {
                modified = null;
            }


            if (type != null && type.equals(ElementSetType.SUMMARY))
                return new SummaryRecordType(identifier, title, dataType, bboxes, subjects, formats, modified, abstractt);

            final SimpleLiteral date    = modified;
            if (elementName != null && elementName.contains(_Date_QNAME)) {
                customRecord.setDate(date);
            }


            List<SimpleLiteral> creator = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Responsibility rp :identification.getPointOfContacts()) {
                    if (Role.ORIGINATOR.equals(rp.getRole())) {
                        creator.add(new SimpleLiteral(((ResponsibleParty) rp).getOrganisationName().toString()));
                    }
                }
            }
            if (creator.isEmpty()) creator = null;

            if (elementName != null && elementName.contains(_Creator_QNAME)) {
                customRecord.setCreator(creator);
            }


            // TODO multiple
            SimpleLiteral distributor = null;
            for (final Distribution distribution : metadata.getDistributionInfo()) {
                for (Distributor dis :distribution.getDistributors()) {
                    final ResponsibleParty disRP = (ResponsibleParty) dis.getDistributorContact();
                    if (disRP != null) {
                        InternationalString name = disRP.getOrganisationName();
                        if (name != null) {
                            distributor = new SimpleLiteral(name.toString());
                            break;
                        }
                    }
                }
            }
            if (elementName != null && elementName.contains(_Publisher_QNAME)) {
                customRecord.setPublisher(distributor);
            }

            final SimpleLiteral language;
            if (metadata.getLanguage() != null) {
                language = new SimpleLiteral(metadata.getLanguage().getISO3Language());
                if (elementName != null && elementName.contains(_Language_QNAME)) {
                    customRecord.setLanguage(language);
                }
            } else {
                language = null;
            }

            // TODO
            final SimpleLiteral spatial = null;
            final SimpleLiteral references = null;
            if (type != null && type.equals(ElementSetType.FULL))
                return new RecordType(identifier, title, dataType, subjects, formats, modified, date, abstractt, bboxes, creator, distributor, language, spatial, references);

            return customRecord;
        }
        return null;
    }

    private org.geotoolkit.csw.xml.v300.AbstractRecordType translateISOtoDC300(final DefaultMetadata metadata, final ElementSetType type, final List<QName> elementName) {
        if (metadata != null) {

            final org.geotoolkit.csw.xml.v300.RecordType customRecord = new org.geotoolkit.csw.xml.v300.RecordType();

            /*
             * BRIEF part
             */
            final SimpleLiteral identifier = new SimpleLiteral(metadata.getFileIdentifier());
            if (elementName != null && elementName.contains(_Identifier_QNAME)) {
                customRecord.setIdentifier(identifier);
            }

            SimpleLiteral title = null;
            //TODO see for multiple identification
            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification.getCitation() != null && identification.getCitation().getTitle() != null) {
                    title = new SimpleLiteral(identification.getCitation().getTitle().toString());
                }
            }
            if (elementName != null && elementName.contains(_Title_QNAME)) {
                customRecord.setTitle(title);
            }

            SimpleLiteral dataType = null;
            //TODO see for multiple hierarchyLevel
            for (ScopeCode code: metadata.getHierarchyLevels()) {
                dataType = new SimpleLiteral(code.identifier());
            }
            if (elementName != null && elementName.contains(_Type_QNAME)) {
                customRecord.setType(dataType);
            }

            final List<org.geotoolkit.ows.xml.v200.BoundingBoxType> bboxes = new ArrayList<>();

            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification instanceof DataIdentification) {
                    final DataIdentification dataIdentification = (DataIdentification) identification;
                    for (Extent extent : dataIdentification.getExtents()) {
                        for (GeographicExtent geoExtent :extent.getGeographicElements()) {
                            if (geoExtent instanceof GeographicBoundingBox) {
                                final GeographicBoundingBox bbox = (GeographicBoundingBox) geoExtent;
                                // TODO find CRS
                                bboxes.add(new org.geotoolkit.ows.xml.v200.BoundingBoxType("EPSG:4326",
                                                                bbox.getWestBoundLongitude(),
                                                                bbox.getSouthBoundLatitude(),
                                                                bbox.getEastBoundLongitude(),
                                                                bbox.getNorthBoundLatitude()));
                            }
                        }
                    }
                }
            }
            if (elementName != null && elementName.contains(_BoundingBox_QNAME)) {
                final org.geotoolkit.ows.xml.v200.ObjectFactory owsFactory = new org.geotoolkit.ows.xml.v200.ObjectFactory();
                for(org.geotoolkit.ows.xml.v200.BoundingBoxType bb : bboxes) {
                    customRecord.getBoundingBox().add(owsFactory.createBoundingBox(bb));
                }
            }

            if (type != null && type.equals(ElementSetType.BRIEF)) {
                return new org.geotoolkit.csw.xml.v300.BriefRecordType(identifier, title, dataType, bboxes);
            }
            /*
             *  SUMMARY part
             */
            final List<SimpleLiteral> abstractt = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                if (identification.getAbstract() != null) {
                    abstractt.add(new SimpleLiteral(identification.getAbstract().toString()));
                }
            }
            if (elementName != null && elementName.contains(_Abstract_QNAME)) {
                customRecord.setAbstract(abstractt);
            }

            final List<SimpleLiteral> subjects = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Keywords kw :identification.getDescriptiveKeywords()) {
                    for (InternationalString str : kw.getKeywords()) {
                        subjects.add(new SimpleLiteral(str.toString()));
                    }
                }
                if (identification instanceof DataIdentification) {
                    final DataIdentification dataIdentification = (DataIdentification) identification;
                    for (TopicCategory tc : dataIdentification.getTopicCategories()) {
                        subjects.add(new SimpleLiteral(tc.identifier()));
                    }
                }
            }
            if (elementName != null && elementName.contains(_Subject_QNAME)) {
                customRecord.setSubject(subjects);
            }


            List<SimpleLiteral> formats = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Format f :identification.getResourceFormats()) {
                    if (f == null || f.getName() == null) {
                        continue;
                    }
                    formats.add(new SimpleLiteral(f.getName().toString()));
                }
            }
            if (formats.isEmpty()) {
                formats = null;
            }
            if (elementName != null && elementName.contains(_Format_QNAME)) {
                customRecord.setFormat(formats);
            }

            final SimpleLiteral modified;
            if (metadata.getDateStamp() != null) {
                String dateValue;
                synchronized (FORMATTER) {
                    dateValue = FORMATTER.format(metadata.getDateStamp());
                }
                dateValue = dateValue.substring(0, dateValue.length() - 2);
                dateValue = dateValue + ":00";
                modified = new SimpleLiteral(dateValue);
                if (elementName != null && elementName.contains(_Modified_QNAME)) {
                    customRecord.setModified(modified);
                }
            } else {
                modified = null;
            }


            if (type != null && type.equals(ElementSetType.SUMMARY))
                return new org.geotoolkit.csw.xml.v300.SummaryRecordType(identifier, title, dataType, bboxes, subjects, formats, modified, abstractt);

            final SimpleLiteral date    = modified;
            if (elementName != null && elementName.contains(_Date_QNAME)) {
                customRecord.setDate(date);
            }


            List<SimpleLiteral> creator = new ArrayList<>();
            for (Identification identification: metadata.getIdentificationInfo()) {
                for (Responsibility rp :identification.getPointOfContacts()) {
                    if (Role.ORIGINATOR.equals(rp.getRole())) {
                        creator.add(new SimpleLiteral(((ResponsibleParty) rp).getOrganisationName().toString()));
                    }
                }
            }
            if (creator.isEmpty()) creator = null;

            if (elementName != null && elementName.contains(_Creator_QNAME)) {
                customRecord.setCreator(creator);
            }


            // TODO multiple
            SimpleLiteral distributor = null;
            for (final Distribution distribution : metadata.getDistributionInfo()) {
                for (Distributor dis :distribution.getDistributors()) {
                    final ResponsibleParty disRP = (ResponsibleParty) dis.getDistributorContact();
                    if (disRP != null) {
                        InternationalString name = disRP.getOrganisationName();
                        if (name != null) {
                            distributor = new SimpleLiteral(name.toString());
                            break;
                        }
                    }
                }
            }
            if (elementName != null && elementName.contains(_Publisher_QNAME)) {
                customRecord.setPublisher(distributor);
            }

            final SimpleLiteral language;
            if (metadata.getLanguage() != null) {
                language = new SimpleLiteral(metadata.getLanguage().getISO3Language());
                if (elementName != null && elementName.contains(_Language_QNAME)) {
                    customRecord.setLanguage(language);
                }
            } else {
                language = null;
            }

            // TODO
            final SimpleLiteral spatial = null;
            final SimpleLiteral references = null;
            if (type != null && type.equals(ElementSetType.FULL)) {
                org.geotoolkit.csw.xml.v300.RecordType record = new org.geotoolkit.csw.xml.v300.RecordType();
                record.setIdentifier(identifier);
                record.setTitle(title);
                record.setType(dataType);
                record.setSubject(subjects);
                record.setFormat(formats);
                record.setModified(modified);
                record.setDate(date);
                record.setAbstract(abstractt);
                record.setCreator(creator);
                record.setPublisher(distributor);
                record.setLanguage(language);
                record.setSpatial(spatial);
                record.setReferences(references);
                final org.geotoolkit.ows.xml.v200.ObjectFactory owsFactory = new org.geotoolkit.ows.xml.v200.ObjectFactory();
                for(org.geotoolkit.ows.xml.v200.BoundingBoxType bb : bboxes) {
                    customRecord.getBoundingBox().add(owsFactory.createBoundingBox(bb));
                }
            }

            return customRecord;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DomainValues> getFieldDomainofValues(final String propertyNames) throws MetadataIoException {
        final List<DomainValues> responseList = new ArrayList<>();
        final StringTokenizer tokens          = new StringTokenizer(propertyNames, ",");

        while (tokens.hasMoreTokens()) {
            final String token       = tokens.nextToken().trim();
            final List<String> paths = getPathForQueryable(token);
            if (!paths.isEmpty()) {
                final List<String> values         = getAllValuesFromPaths(paths, dataDirectory, null);
                final DomainValuesType value      = new DomainValuesType(null, token, values, METADATA_QNAME);
                responseList.add(value);
            } else {
                throw new MetadataIoException("The property " + token + " is not queryable for now",
                        INVALID_PARAMETER_VALUE, "propertyName");
            }
        }
        return responseList;
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        final List<String> paths = getPathForQueryable(token);
        if (!paths.isEmpty()) {
            return getAllValuesFromPaths(identifier, paths);
        } else {
            throw new MetadataIoException("The property " + token + " is not queryable for now",
                    INVALID_PARAMETER_VALUE, "propertyName");
        }
    }

    /**
     * Return a list of metadata path for the specified queryable.
     *
     * @param token a queryable.
     */
    protected List<String> getPathForQueryable(String token) throws MetadataIoException {
        final List<String> paths;
        if (ISO_QUERYABLE.get(token) != null) {
            paths = ISO_QUERYABLE.get(token).paths;
        } else if (DUBLIN_CORE_QUERYABLE.get(token) != null) {
            paths = DUBLIN_CORE_QUERYABLE.get(token).paths;
       /*} else if (additionalQueryable.get(token) != null) { // TODO when additional queryable will be added
                paths = additionalQueryable.get(token).paths;
        */} else {
            throw new MetadataIoException("The property " + token + " is not queryable",
                    INVALID_PARAMETER_VALUE, "propertyName");
        }
        return XpathUtils.xpathToMDPath(paths);
    }

    /**
     * Return all the String values corresponding to the specified list of path through the metadata.
     */
    private List<String> getAllValuesFromPaths(final List<String> paths, final Path directory, final String parentIdentifierPrefix) throws MetadataIoException {
        final String identifierPrefix    = computeIdentifierPrefix(directory, parentIdentifierPrefix);
        final List<String> result        = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path metadataFile : stream) {

                final String fileName = metadataFile.getFileName().toString();
                if (fileName.endsWith(CURRENT_EXT)) {
                    try {
                        final String identifier = computeIdentifier(fileName, identifierPrefix);
                        final Object metadata = getObjectFromFile(identifier);
                        Utils.setIdentifier(identifier, metadata);

                        final List<Object> value = Utils.extractValues(metadata, paths);
                        if (value != null && !value.equals(Arrays.asList("null"))) {
                            for (Object obj : value) {
                                result.add(obj.toString());
                            }
                        }
                        //continue to the next file
                    } catch (MetadataIoException ex) {
                        LOGGER.log(Level.WARNING, "The netcdf file : {0} can not be read\ncause: {1}", new Object[]{fileName, ex.getMessage()});
                    }

                } else if (Files.isDirectory(metadataFile)) {
                    result.addAll(getAllValuesFromPaths(paths, metadataFile, identifierPrefix));
                } else {
                    //do not throw exception just skipping
                    //throw new MetadataIoException(METAFILE_MSG + f.getPath() + " does not ands with " + CURRENT_EXT + " or is not a directory", INVALID_PARAMETER_VALUE);
                }
            }
        } catch (IOException e) {
            //TODO should we rise a MetadataIoException instead ?
            LOGGER.log(Level.WARNING, "An error occurs during directory scanning", e);
        }
        Collections.sort(result);
        return result;
    }

    protected List<String> getAllValuesFromPaths(final String metadataID, final List<String> paths) throws MetadataIoException {
        final List<String> result = new ArrayList<>();
        final Object metadata = getObjectFromFile(metadataID);
        final List<Object> value = Utils.extractValues(metadata, paths);
        if (value != null && !value.equals(Arrays.asList("null"))) {
            for (Object obj : value) {
                result.add(obj.toString());
            }
        }
        Collections.sort(result);
        return result;
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

    /**
     * Return the list of Additional queryable element.
     */
    @Override
    public List<QName> getAdditionalQueryableQName() {
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PathType> getAdditionalQueryablePathMap() {
        return new HashMap<>();
    }

    protected Node writeObjectInNode(final Object obj, final MetadataType mode) throws MetadataIoException {
        final boolean replace = mode == MetadataType.ISO_19115;
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            NodeUtilities.secureFactory(dbf);
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
