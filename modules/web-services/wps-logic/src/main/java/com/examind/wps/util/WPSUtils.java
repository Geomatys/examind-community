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
package com.examind.wps.util;

import com.examind.wps.api.IOParameterException;
import static com.examind.wps.util.WPSConstants.IDENTIFIER_PARAMETER;
import static com.examind.wps.util.WPSConstants.MAX_MB_INPUT_COMPLEX;
import static com.examind.wps.util.WPSConstants.PROCESS_PREFIX;
import static com.examind.wps.util.WPSConstants.URN_SEPARATOR;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.measure.Unit;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProviders;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.feature.xml.jaxb.JAXBFeatureTypeWriter;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.ows.xml.v200.AdditionalParameter;
import org.geotoolkit.ows.xml.v200.AdditionalParametersType;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.DomainMetadataType;
import org.geotoolkit.ows.xml.v200.LanguageStringType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.io.WPSEncoding;
import org.geotoolkit.wps.io.WPSIO;
import org.geotoolkit.wps.io.WPSIO.FormatSupport;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v200.ComplexData;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.Format;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.xsd.xml.v2001.Schema;
import org.geotoolkit.xsd.xml.v2001.XSDMarshallerPool;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Algorithm;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.util.InternationalString;
import org.opengis.util.NoSuchIdentifierException;


/**
 * Set of utilities method used by WPS worker.
 *
 * @author Quentin Boileau (Geomatys).
 */
public class WPSUtils {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.utils");

    private WPSUtils() {}

    /**
     * Return the process descriptor from a process identifier
     *
     * @param identifier like "urn:ogc:cstl:wps:math:add"
     * @return ProcessDescriptor
     * @throws CstlServiceException in case of an unknown process identifier.
     */
    public static ProcessDescriptor getProcessDescriptor(final String identifier) throws CstlServiceException {
        try {
            final Identifier id = parseProcessIdentifier(identifier);
            return ProcessFinder.getProcessDescriptor(id.getCodeSpace(), id.getCode());

        } catch (NoSuchIdentifierException | IllegalArgumentException | NullArgumentException ex) {
            throw new CstlServiceException("The process " + IDENTIFIER_PARAMETER.toLowerCase() + " : " + identifier + " does not exist.",
                    INVALID_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase());
        }
    }

    /**
     * Get the type of the input/output parameter of a given process
     * @param processIdentifier identifier of the desired process
     * @param inOutIdentifier identifier of the desired input/output
     * @return the type of the researched input/output
     * @throws CstlServiceException when the given process identifier can not be found
     * @throws ParameterNotFoundException when the input/output identifier can not be found
     */
    public static Class getIOClassFromIdentifier(final String processIdentifier, final String inOutIdentifier) throws CstlServiceException, ParameterNotFoundException {
        ProcessDescriptor processDescriptor = getProcessDescriptor(processIdentifier);
        GeneralParameterDescriptor gpd;
        String ioCode = extractProcessIOCode(processDescriptor, inOutIdentifier);

        // We first try to get a descriptor from the InputDescriptor
        try {
            gpd = processDescriptor.getInputDescriptor().descriptor(ioCode);
        }
        catch (ParameterNotFoundException ex) {
            // Then if the InputDescriptor returns nothing we try on the OutputDescriptor
            gpd = processDescriptor.getOutputDescriptor().descriptor(ioCode);
        }

        // If the parameter descriptor is a group
        // We don't go through the group hierarchy
        if (gpd instanceof ParameterDescriptorGroup) {
            ParameterDescriptorGroup pdg = ((ParameterDescriptorGroup)gpd);
            throw new UnsupportedOperationException("Not implemented yet : the desired input/output is a group");
        }
        else {
            // It should be a parameter descriptor
            assert gpd instanceof ParameterDescriptor;

            ParameterDescriptor paramDescriptor = (ParameterDescriptor)gpd;
            return paramDescriptor.getValueClass();
        }
    }

    /**
     * Return a brief description of the process from his process descriptor.
     *
     * @param processDesc
     * @param loc
     * @return ProcessSummary
     */
    public static ProcessSummary generateProcessBrief(final ProcessDescriptor processDesc, Locale loc) {
        return generateProcessBrief(processDesc, loc, true);
    }

    public static ProcessSummary generateProcessBrief(final ProcessDescriptor processDesc, Locale loc, final boolean withPrefix) {
        return new ProcessSummary(new CodeType(withPrefix? buildProcessIdentifier(processDesc) : processDesc.getIdentifier().getCode()),
                                  buildProcessTitle(processDesc, loc),
                                  Arrays.asList(capitalizeFirstLetter(processDesc.getProcedureDescription().toString(loc))),
                                  null,
                                  ((processDesc.getIdentifier().getVersion() == null) ? "1.0.0" : processDesc.getIdentifier().getVersion()));
    }

    /**
     * Build a Title for a process from his process descriptor.
     *
     * @param processDesc Descriptor to extract a title from.
     * @param lang Language to express title into.
     * @return The built process title.
     */
    public static LanguageStringType buildProcessTitle(final ProcessDescriptor processDesc, Locale lang) {
        ArgumentChecks.ensureNonNull("processDesc", processDesc);
        final String title;

        // Generate a title if the descriptor has no display name
        if (processDesc.getDisplayName() == null) {
            title = capitalizeFirstLetter(processDesc.getIdentifier().getAuthority().getTitle().toString()).getValue() + " : "
                  + capitalizeFirstLetter(processDesc.getIdentifier().getCode()).getValue();
        } else {
            title = processDesc.getDisplayName().toString(lang);
        }
        return new LanguageStringType(title, lang.toLanguageTag());
    }

    public static Stream<LanguageStringType> buildProcessDescription(final ProcessDescriptor process, final Locale lang) {
        final Collection<? extends Algorithm> algorithms = process.getAlgorithms();
        Stream<InternationalString> descStream = (algorithms == null? Stream.<Algorithm>empty() : algorithms.stream())
                .map(algo -> algo.getDescription());

        final InternationalString procedureDescription = process.getProcedureDescription();
        if (procedureDescription != null) {
            descStream = Stream.concat(descStream, Stream.of(procedureDescription));
        }

        // TODO : include documentation and software reference from Processing interface.

        return descStream
                .map(text -> new LanguageStringType(text.toString(lang), lang.toLanguageTag()));
    }



    /**
     * Build OGC URN unique identifier for a process from his process descriptor.
     *
     * @param processDesc
     * @return
     */
    public static String buildProcessIdentifier(final ProcessDescriptor processDesc) {
        ArgumentChecks.ensureNonNull("processDesc", processDesc);

        final String processId = processDesc.getIdentifier().getCode();
        // for some process name that are already urn, we don't transform them
        if (processId.startsWith("urn:")) {
            return processId;
        }
        final Identifier id = processDesc.getIdentifier();
        final StringJoiner urnBuilder = new StringJoiner(URN_SEPARATOR, PROCESS_PREFIX, "");

        addIdentifier(urnBuilder, id);

        return urnBuilder.toString();
    }

    private static void addIdentifier(final StringJoiner target, final Identifier id) {
        String codeSpace = id.getCodeSpace();
        if (codeSpace == null || (codeSpace = codeSpace.trim()).isEmpty()) {
            final Citation authority = id.getAuthority();
            codeSpace = authority == null? "" : Citations.toCodeSpace(authority);
        }
        target.add(codeSpace);

        final String version = id.getVersion();
        target.add(version == null? "" : version);

        target.add(id.getCode());
    }


    /**
     * Extract the factory name and the process name from a process identifier. e.g : urn:exa:wps:geotoolkit::jts:intersection
     * return math:add.
     *
     * @param identifier
     * @return factoryName:processName.
     */
    private static Identifier parseProcessIdentifier(String identifier) throws CstlServiceException {
        ArgumentChecks.ensureNonEmpty("Identifier", identifier);

        identifier = identifier.trim();

        // for a Geotk identifier
        if (identifier.startsWith(PROCESS_PREFIX)) {
            identifier = identifier.substring(PROCESS_PREFIX.length() - 1);
            /* Warning: we do not use String.split here, because we need to be
             * resilient concurrent empty pieces in the URN. For example, for
             * the following URN : ::MyProcess  would give a string array with
             * only one element, making it difficult to determine which piece is
             * missing.
             */
            final String[] parts = new String[3];
            int previousSeparator = 0, nextSeparator;
            for (int i = 0; i < parts.length -1; i++) {
                nextSeparator = identifier.indexOf(URN_SEPARATOR, previousSeparator + 1);
                if (nextSeparator < 0)
                    break;

                if (previousSeparator < nextSeparator) {
                    parts[i] = identifier.substring(previousSeparator + 1, nextSeparator);
                    previousSeparator = nextSeparator;
                }
            }
            parts[2] = identifier.substring(previousSeparator + 1);

            if (parts[2] != null) {
                return new ImmutableIdentifier(null, parts[0], parts[2], parts[1], null);
            }
        }
    // for any other process (identified directly by their identifier)
        return new ImmutableIdentifier(null, null, identifier, null, null);

    }

    /**
     * Generate process INPUT/OUPTUT identifiers based on process identifier. e.g :
     * urn:ogc:cstl:wps:math:add:input:number1, urn:ogc:cstl:wps:math:add:ouput:result
     *
     * @param procDesc
     * @param parameter
     * @param ioType
     * @return processIdentifier:ioType:paramName
     */
    public static String buildProcessIOIdentifiers(final ProcessDescriptor procDesc, final GeneralParameterDescriptor parameter,
            final WPSIO.IOType ioType) {
        if (parameter != null) {
            final String paramCode = parameter.getName().getCode();
            // for some parameters which are already urn, we don't transform them
            if (paramCode.startsWith("urn:")) {
                return parameter.getName().getCode();

            // General Geotk process behavior
            } else {
                if (ioType.equals(WPSIO.IOType.INPUT)) {
                    return buildProcessIdentifier(procDesc) + ":input:" + parameter.getName().getCode();
                } else {
                    return buildProcessIdentifier(procDesc) + ":output:" + parameter.getName().getCode();
                }
            }
        }
        return null;
    }

    /**
     * Extract the process INPUT/OUPTUT code. e.g : urn:ogc:geomatys:wps:math:add:input:number1 will return number1
     *
     * @param identifier Input/Output identifier.
     * @return string code.
     */
    public static String extractProcessIOCode(final ProcessDescriptor procDesc, String identifier) {
        ArgumentChecks.ensureNonNull("identifier", identifier);
        String processId = buildProcessIdentifier(procDesc);
        if (identifier.startsWith(processId + ':')) {
            identifier = identifier.substring(processId.length() + 1);
            if (identifier.startsWith("input:")) {
                return identifier.substring("input:".length());
            }
            if (identifier.startsWith("output:")) {
                return identifier.substring("output:".length());
            }
            return identifier;
        }
        // fall back, not safe
        return identifier.substring(identifier.lastIndexOf(URN_SEPARATOR) + 1, identifier.length());
    }

    /**
     * Build a Title for a process from his process descriptor.
     *
     * @param param Descriptor to extract a title from.
     * @param lang Language to express title into.
     * @return The built process title.
     */
    public static LanguageStringType buildProcessIOTitle(final GeneralParameterDescriptor param, Locale lang) {
        ArgumentChecks.ensureNonNull("param", param);
        String title = null;
        if (param instanceof ExtendedParameterDescriptor) {
            ExtendedParameterDescriptor extParam = (ExtendedParameterDescriptor) param;
            Map userMap = extParam.getUserObject();
            if (userMap != null && userMap.containsKey("Title")) {
                title = (String) userMap.get("Title");
            }
        }
        if (title == null) {
            title = WPSUtils.capitalizeFirstLetterStr(param.getName().getCode());
        }
        return new LanguageStringType(title, lang.toLanguageTag());
    }

    private static final Map<Locale, String> DEFAULT_DESCRIPTION = new HashMap<>();
    static {
        DEFAULT_DESCRIPTION.put(Locale.forLanguageTag(WPSConstants.WPS_LANG_EN), "No description available");
        DEFAULT_DESCRIPTION.put(Locale.forLanguageTag(WPSConstants.WPS_LANG_FR), "Pas de description disponible");
    }


    public static LanguageStringType buildProcessIODescription(final GeneralParameterDescriptor param, final Locale lang) {
        String _abstract = Stream.of(param.getDescription(), param.getRemarks())
                .filter(Objects::nonNull)
                .map(is -> is.toString(lang))
                .collect(Collectors.joining(System.lineSeparator()));

        if (_abstract.isEmpty()) {
            _abstract = DEFAULT_DESCRIPTION.get(lang);
        }

        return new LanguageStringType(_abstract, lang.toLanguageTag());
    }

    public static List<AdditionalParametersType> buildAdditionalParams(final GeneralParameterDescriptor param) {
        ArgumentChecks.ensureNonNull("param", param);

        String role = null;
        List<AdditionalParameter> additionalParams = null;
        if (param instanceof ExtendedParameterDescriptor) {
            ExtendedParameterDescriptor extParam = (ExtendedParameterDescriptor) param;
            Map<String, Object> userMap = extParam.getUserObject();
            if (userMap != null) {
                additionalParams = new ArrayList<>();
                for (Entry<String, Object> e : userMap.entrySet()) {
                    if (e.getValue() instanceof String) {
                        if ("role".equals(e.getKey())) {
                            role = (String) e.getValue();
                        } else if (!"Title".equals(e.getKey())) {
                            additionalParams.add(new AdditionalParameter(new CodeType(e.getKey()), Arrays.asList(e.getValue())));
                        }
                    }
                }
            }
        }
        if ((additionalParams != null && !additionalParams.isEmpty()) || role != null) {
            AdditionalParametersType additionalParamers = new AdditionalParametersType(role, additionalParams);
            return Arrays.asList(additionalParamers);
        }
        return null;
    }


    /**
     * Return the given String with the first letter to upper case.
     *
     * @param version WPS version
     * @param value
     * @return LanguageStringType
     */
    private static LanguageStringType capitalizeFirstLetter(final String value) {
        if (value != null && !value.isEmpty()) {
            final StringBuilder result = new StringBuilder(value);
            result.replace(0, 1, result.substring(0, 1).toUpperCase());
            return new LanguageStringType(result.toString());
        }
        return new LanguageStringType(value);
    }

    /**
     * Return the given String with the first letter to upper case.
     *
     * @param value
     * @return LanguageStringType
     */
    public static String capitalizeFirstLetterStr(final String value) {
        if (value != null && !value.isEmpty()) {
            final StringBuilder result = new StringBuilder(value);
            result.replace(0, 1, result.substring(0, 1).toUpperCase());
            return result.toString();
        }
        return value;
    }

    /**
     * Generate supported UOM (Units) for a given ParameterDescriptor. If this descriptor have default unit, supported
     * UOM returned will be all the compatible untis to the default one.
     *
     * @param param
     * @return SupportedUOMsType or null if the parameter does'nt have any default unit.
     */
    static public DomainMetadataType generateUOMs(final ParameterDescriptor param) {
        if (param != null && param.getUnit() != null) {
            final Unit unit = param.getUnit();
            Set<? extends Unit<?>> units = javax.measure.spi.ServiceProvider.current().getSystemOfUnitsService().getSystemOfUnits().getUnits();

            List<DomainMetadataType> uoms = new ArrayList<>();
            for (Unit u : units) {
                if (unit.isCompatible(u)) {
                    uoms.add(new DomainMetadataType(u.toString(), null));
                }
            }

            // ici on perd tout ce qu'il y avait dans la V1
            DomainMetadataType _default = new DomainMetadataType(unit.toString(), null);
            return _default;
        }
        return null;
    }

    /**
     * Test if a process is supported by the WPS.
     *
     * @param descriptor
     * @return true if process is supported, false if is not.
     */
    public static boolean isSupportedProcess(final ProcessDescriptor descriptor) {

        //Inputs
        final GeneralParameterDescriptor inputDesc = descriptor.getInputDescriptor();
        if(!isSupportedParameter(inputDesc, WPSIO.IOType.INPUT)) {
            return false;
        }

        //Outputs
        GeneralParameterDescriptor outputDesc = descriptor.getOutputDescriptor();
        return isSupportedParameter(outputDesc, WPSIO.IOType.OUTPUT);
    }

    /**
     * A function which test if the given parameter can be proceed by the WPS.
     * @param toTest The descriptor of the parameter to test.
     * @param type The parameter type (input or output).
     * @return true if the WPS can work with this parameter, false otherwise.
     */
    public static boolean isSupportedParameter(GeneralParameterDescriptor toTest, WPSIO.IOType type) {
        boolean isClean = false;
        if (toTest instanceof ParameterDescriptorGroup) {
            final List<GeneralParameterDescriptor> descs = ((ParameterDescriptorGroup) toTest).descriptors();
            if (descs.isEmpty()) {
                isClean = true;
            } else {
                for (GeneralParameterDescriptor desc : descs) {
                    isClean = isSupportedParameter(desc, type);
                    if (!isClean) {
                        break;
                    }
                }
            }
        } else if (toTest instanceof ParameterDescriptor) {
            final ParameterDescriptor param = (ParameterDescriptor) toTest;
            final Class paramClass = param.getValueClass();

            isClean = (type.equals(WPSIO.IOType.INPUT))
                    ? WPSIO.isSupportedInputClass(paramClass)
                    : WPSIO.isSupportedOutputClass(paramClass);
        }
        return isClean;
    }

    /**
     * Return the SupportedComplexDataInputType for the given class.
     *
     * @param attributeClass The java class to get complex type from.
     * @param ioType The type of parameter to describe (input or output).
     * @param type The complex type (complex, reference, etc.).
     * @param userData A map containing user's options for type support.
     * @return SupportedComplexDataInputType
     */
    public static ComplexData describeComplex(final Class attributeClass, final WPSIO.IOType ioType, final WPSIO.FormChoice type, final Map<String, Object> userData) {

        //Set MaximumMegabyte only for the complex input description
        Integer maximumMegabytes = null;
        if (ioType == WPSIO.IOType.INPUT) {
            maximumMegabytes = MAX_MB_INPUT_COMPLEX;
        }

        final List<Format> formats = new ArrayList<>();
        List<FormatSupport> infos = null;
        String schema = null;

        if (userData != null) {
            schema = (String) userData.get(WPSIO.SCHEMA_KEY);
            infos = getCustomIOFormats(userData, ioType);
        }

        if (infos == null) {
            infos = WPSIO.getFormats(attributeClass, ioType);
        }

        Format defaultFormat = null;
        if (infos != null) {
            for (FormatSupport inputClass : infos) {

                String encoding = inputClass.getEncoding();
                String mimetype = inputClass.getMimeType();
                String schemaf  = schema != null ? schema : inputClass.getSchema(); //URL to xsd schema

                Format format = new Format(encoding, mimetype, schemaf, maximumMegabytes);

                if (inputClass.isDefaultFormat()) {
                    if (defaultFormat != null) {
                        //multiple default format
                        //prefere the one with base64 encoding
                        if (WPSEncoding.BASE64.getValue().equals(encoding) && !WPSEncoding.BASE64.getValue().equals(defaultFormat.getEncoding())) {
                            format.setDefault(true);
                            defaultFormat = format;
                            formats.add(format);
                        } else {
                            formats.add(format);
                        }
                    } else {
                        format.setDefault(true);
                        defaultFormat = format;
                        formats.add(format);
                    }
                } else {
                    formats.add(format);
                }
            }
        }

        // set for XML test purpose
        Collections.sort(formats, new FormatComparator());

        // set default for the first format => TODO find a real default
        if (defaultFormat == null && !formats.isEmpty()) {
            formats.get(0).setDefault(Boolean.TRUE);
        }
        return new ComplexData(formats, maximumMegabytes);
    }

    public static List<FormatSupport> getCustomIOFormats(final Map<String, Object> userData, final WPSIO.IOType ioType) {
        if (userData != null) {
            try {
                // try first way of defining custom format (not marshallable)
                List<FormatSupport> infos = (List<FormatSupport>) userData.get(WPSIO.SUPPORTED_FORMATS_KEY);

                // try second way of defining custom format (used by dynamic chain for example)
                if (infos == null) {
                    List<Map> userFormats = (List<Map>) userData.get("formats");
                    if (userFormats != null) {
                        infos = new ArrayList<>();
                        for (Map userFormat : userFormats) {
                            FormatSupport format = new FormatSupport(null,
                                    ioType,
                                    (String) userFormat.get("mimetype"),
                                    (String) userFormat.get("encoding"),
                                    (String) userFormat.get("schema"),
                                    true);
                            infos.add(format);
                        }
                    }
                }
                return infos;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "A parameter custom format definition cannot be read.", e);
            }
        }
        return null;
    }

    public static List<Format> getWPSCustomIOFormats(final Map<String, Object> userData, final WPSIO.IOType ioType) {
        List<FormatSupport> infos = getCustomIOFormats(userData, ioType);
        if (infos != null) {
            final List<Format> formats = new ArrayList<>();
            for (FormatSupport inputClass : infos) {

                String encoding = inputClass.getEncoding();
                String mimetype = inputClass.getMimeType();
                String schemaf  = inputClass.getSchema(); //URL to xsd schema

                Format format = new Format(encoding, mimetype, schemaf, null);
                formats.add(format);
            }
            // set for XML test purpose
            Collections.sort(formats, new FormatComparator());

            // set default for the first format => TODO find a real default
            if (!formats.isEmpty()) {
                formats.get(0).setDefault(Boolean.TRUE);
            }
            return formats;
        }
        return null;
    }

   static class FormatComparator implements Comparator<Format> {

        @Override
        public int compare(Format o1, Format o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {
                return 1;
            }

            // Special case : we check if one of the formats is a default format.
            if (!Objects.equals(o1.isDefault(), o2.isDefault())) {
                if (Boolean.TRUE.equals(o1.isDefault())) return -1;
                if (Boolean.TRUE.equals(o2.isDefault())) return 1;
            }

            if (Objects.equals(o1.getMimeType(), o2.getMimeType())) {
                if (Objects.equals(o1.getEncoding(), o2.getEncoding())) {
                    if (Objects.equals(o1.getSchema(), o2.getSchema())) {
                        return 0;
                    } else if (o1.getSchema() == null) {
                        return 1;
                    } else if (o2.getSchema() == null) {
                        return -1;
                    } else {
                        return o1.getSchema().compareTo(o2.getSchema());
                    }
                } else if (o1.getEncoding() == null) {
                    return 1;
                } else if (o2.getEncoding() == null) {
                    return -1;
                } else {
                    return o1.getEncoding().compareTo(o2.getEncoding());
                }
            } else if (o1.getMimeType() == null) {
                return 1;
            } else if (o2.getMimeType() == null) {
                return -1;
            } else {
                return o1.getMimeType().compareTo(o2.getMimeType());
            }
        }
    }


    /**
     * Check if all requested inputs/outputs are present in the process descriptor. Check also if all mandatory
     * inputs/outputs are specified. If an non allowed input/output is requested or if a mandatory input/output is
     * missing, an {@link CstlServiceException CstlServiceException} will be throw.
     *
     * @param processDesc
     * @param request
     * @throws IOParameterException if an non allowed input/output is requested or if a mandatory input/output is
     * missing.
     */
    public static void checkValidInputOuputRequest(final ProcessDescriptor processDesc, final Execute request, final boolean withPrefix) throws IOParameterException {

        //check inputs
        final List<String> inputIdentifiers = extractRequestInputIdentifiers(request);
        final ParameterDescriptorGroup inputDescriptorGroup = processDesc.getInputDescriptor();
        final Map<String, Boolean> inputDescMap = descriptorsAsMap(inputDescriptorGroup, processDesc, WPSIO.IOType.INPUT, withPrefix);
        checkIOIdentifiers(inputDescMap, inputIdentifiers, WPSIO.IOType.INPUT);

        //check outputs
        final List<String> outputIdentifiers = extractRequestOutputIdentifiers(request);
        final ParameterDescriptorGroup outputDescriptorGroup = processDesc.getOutputDescriptor();
        final Map<String, Boolean> outputDescMap = descriptorsAsMap(outputDescriptorGroup, processDesc, WPSIO.IOType.OUTPUT, withPrefix);
        checkIOIdentifiers(outputDescMap, outputIdentifiers, WPSIO.IOType.OUTPUT);

    }

    /**
     * Extract the list of identifiers {@code String} requested in input.
     *
     * @param request
     * @return a list of identifiers
     */
    public static List<String> extractRequestInputIdentifiers(final Execute request) {

        final List<String> identifiers = new ArrayList<>();
        if (request != null && request.getInput() != null) {
            for (final DataInput in : request.getInput()) {
                identifiers.add(in.getId());
            }
        }
        return identifiers;
    }

    /**
     * Extract the list of identifiers {@code String} requested in output.
     *
     * @param request
     * @return a list of identifiers
     */
    public static List<String> extractRequestOutputIdentifiers(final Execute request) {
        final List<String> identifiers = new ArrayList<>();
        if (request != null && request.getOutput()!= null) {
            for (OutputDefinition out : request.getOutput()) {
                identifiers.add(out.getIdentifier());
            }
        }
        return identifiers;
    }

    /**
     * Build a {@code Map} from a {@link ParameterDescriptorGroup ParameterDescriptorGroup}. The map keys are the
     * parameter identifier as code and the boolean value the mandatory of the parameter.
     *
     * @param descGroup
     * @return all parameters code and there mandatory value as map.
     */
    private static Map<String, Boolean> descriptorsAsMap(final ParameterDescriptorGroup descGroup, final ProcessDescriptor procDesc,
            final WPSIO.IOType iOType, boolean withPrefix) {

        final Map<String, Boolean> map = new HashMap<>();
        if (descGroup != null && descGroup.descriptors() != null) {
            final List<GeneralParameterDescriptor> descriptors = descGroup.descriptors();

            for (final GeneralParameterDescriptor geneDesc : descriptors) {
                final String id = withPrefix? buildProcessIOIdentifiers(procDesc, geneDesc, iOType) : geneDesc.getName().getCode();
                final boolean required;
                if (geneDesc instanceof ParameterDescriptor && ((ParameterDescriptor)geneDesc).getDefaultValue() != null) {
                    required = false;
                } else {
                    required = geneDesc.getMinimumOccurs() > 0;
                }
                map.put(id, required);
            }
        }
        return map;
    }

    /**
     * Confronts the process parameters {@code Map} to the list of requested identifiers for an type of IO
     * (Input,Output). If there is a missing mandatory parameter in the list of requested identifiers, an {@link CstlServiceException CstlServiceException}
     * will be throw. If an unknown parameter is requested, it also throw an {@link CstlServiceException CstlServiceException}
     *
     * @param descMap - {@code Map} contain all parameters with their mandatory attributes for INPUT or OUTPUT.
     * @param requestIdentifiers - {@code List} of requested identifiers in INPUT or OUTPUT.
     * @param iotype - {@link WPSIO.IOType type}.
     * @throws IOParameterException for missing or unknown parameter.
     */
    private static void checkIOIdentifiers(final Map<String, Boolean> descMap, final List<String> requestIdentifiers, final WPSIO.IOType iotype)
            throws IOParameterException {

        final String type = iotype == WPSIO.IOType.INPUT ? "input" : "output";

        if (descMap.isEmpty() && !requestIdentifiers.isEmpty()) {
            throw new IOParameterException("This process have no inputs.", "input"); //process have no input
        } else {
            //check for Unknown parameter.
            for (final String identifier : requestIdentifiers) {

                if (identifier == null || identifier.isEmpty()) {
                    throw new IOParameterException("Empty " + type + " Identifier.", null);
                }

                if (!descMap.containsKey(identifier)) {
                    throw new IOParameterException("Unknown " + type + " parameter : " + identifier + ".", identifier);
                }
            }
            //check for missing parameters.
            if (descMap.containsValue(Boolean.TRUE)) {
                for (Map.Entry<String, Boolean> entry : descMap.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        if (!requestIdentifiers.contains(entry.getKey())) {
                            throw new IOParameterException("Mandatory " + type + " parameter " + entry.getKey() + " is missing.", false, true, entry.getKey());
                        }
                    }
                }
            }
        }
    }

     /**
     * Store the given object into a temorary file specified by the given fileName into the temporary folder. The object
     * to store is marshalled by the {@link WPSMarshallerPool}. If the temporary file already exist he will be
     * overwrited.
     *
     * @param obj object to marshalle and store to a temporary file.
     * @param fileName temporary file name.
     * @return
     */
    public static boolean storeResponse(final Object obj, final URI folderPath, final String fileName) {
        ArgumentChecks.ensureNonNull("obj", obj);

        final MarshallerPool marshallerPool = WPSMarshallerPool.getInstance();
        boolean success = false;

        final Path outputFile = Paths.get(folderPath).resolve(fileName);
        try (OutputStream stream = Files.newOutputStream(outputFile, CREATE, WRITE, TRUNCATE_EXISTING)){
            final Marshaller marshaller = marshallerPool.acquireMarshaller();
            marshaller.marshal(obj, stream);
            marshallerPool.recycle(marshaller);
            success = Files.exists(outputFile);

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error during unmarshalling", ex);
        }
        return success;
    }

    /**
     * Return tuple toString mime/encoding/schema. "[mimeType, encoding, schema]".
     * @param requestedOuptut DocumentOutputDefinitionType
     * @return tuple string.
     */
    public static String outputDefinitionToString(final OutputDefinition requestedOuptut) {
        final StringBuilder builder = new StringBuilder();
        final String begin = "[";
        final String end = "]";
        final String separator = ", ";

        builder.append(begin);

        builder.append("mimeType=");
        builder.append(requestedOuptut.getMimeType());
        builder.append(separator);

        builder.append("encoding=");
        builder.append(requestedOuptut.getEncoding());
        builder.append(separator);

        builder.append("schema=");
        builder.append(requestedOuptut.getSchema());

        builder.append(end);
        return builder.toString();
    }

    /**
     * A function to retrieve a Feature schema, and store it into the given file
     * as an xsd.
     *
     * @param source The feature to get schema from.
     * @param destination The file where we want to save our feature schema.
     * @throws JAXBException If we can't parse / write the schema properly.
     */
    public static void storeFeatureSchema(FeatureType source, Path destination) throws JAXBException, IOException {

        JAXBFeatureTypeWriter writer = new JAXBFeatureTypeWriter();
        Schema s = writer.getSchemaFromFeatureType(source);
        MarshallerPool pool = XSDMarshallerPool.getInstance();
        try (OutputStream stream = Files.newOutputStream(destination, CREATE, WRITE, TRUNCATE_EXISTING)) {
            Marshaller marsh = pool.acquireMarshaller();
            marsh.marshal(s, stream);
            pool.recycle(marsh);
        }
    }


    /**
     * @return the current time in an XMLGregorianCalendar.
     */
    public static XMLGregorianCalendar getCurrentXMLGregorianCalendar(){
        XMLGregorianCalendar xcal = null;
        try {
            final GregorianCalendar c = new GregorianCalendar();
            c.setTime(new Date());
            xcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            LOGGER.log(Level.INFO, "Cannot create the creation time of the status.");
        }
        return xcal;
    }

    public static void restartWMS(Map<String, Object> parameters) {

        final String wmsInstance = (String) parameters.get(WPSConvertersUtils.WMS_INSTANCE_NAME);
        final String providerId = (String) parameters.get(WPSConvertersUtils.WMS_STORAGE_ID);

        try {
            //restart provider
            DataProviders.getProvider(providerId).reload();
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error during WMS " + wmsInstance + " restart.", ex);
        }

        //restart WMS worker
        try {
            IServiceBusiness serviceBusiness = SpringHelper.getBean(IServiceBusiness.class);
            ServiceComplete def = serviceBusiness.getServiceByIdentifierAndType("WMS", wmsInstance);
            if (def != null) {
                serviceBusiness.restart(def.getId());
            }
        } catch (ConfigurationException e) {
            LOGGER.log(Level.WARNING, "Error during WMS " + wmsInstance + " restart.", e);
        }
    }
}