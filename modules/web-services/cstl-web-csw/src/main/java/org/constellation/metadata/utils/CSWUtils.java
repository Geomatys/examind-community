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
package org.constellation.metadata.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.geotoolkit.csw.xml.AbstractCswRequest;

import static org.constellation.metadata.core.CSWConstants.ACCEPTED_OUTPUT_FORMATS;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.GetRecordsRequest;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.dublincore.xml.v2.elements.SimpleLiteral;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.ogc.xml.FilterXmlFactory;
import org.geotoolkit.ops.xml.v110.OpenSearchDescription;
import org.geotoolkit.ops.xml.v110.Url;
import org.geotoolkit.ows.xml.AbstractDomain;
import org.geotoolkit.ows.xml.AbstractOperation;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.temporal.object.ISODateParser;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.w3._2005.atom.CategoryType;
import org.w3._2005.atom.DateTimeType;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.FeedType;
import org.w3._2005.atom.IdType;
import org.w3._2005.atom.LinkType;
import org.w3._2005.atom.PersonType;
import org.w3._2005.atom.TextType;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CSWUtils {

    public static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.utils");

    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String GET_RECORD_BY_ID;
    static {
        GET_RECORD_BY_ID =  "{SURL}request=GetRecordbyid&service=CSW&version=3.0.0&id={MID}&outputschema={OUT_SCHEME}";
    }

    private static final String GET_ATOM_BY_ID;
    static {
        GET_ATOM_BY_ID = "{SURL}/opensearch?service=CSW&version=3.0.0&recordIds={MID}&outputformat=application/atom%2Bxml";
    }

     /**
     * Return the request (or default) outputFormat (MIME type) of the response.
     * if the format is not supported it throws a WebService Exception.
     *
     * @return the outputFormat (MIME type) of the response.
     */
    public static String getOutputFormat(final AbstractCswRequest request) throws CstlServiceException {

        // we initialize the output format of the response
        final String format = request.getOutputFormat();
        if (format != null && ACCEPTED_OUTPUT_FORMATS.contains(format)) {
            return format;
        } else if (format != null && !ACCEPTED_OUTPUT_FORMATS.contains(format)) {
            final StringBuilder supportedFormat = new StringBuilder();
            for (String s: ACCEPTED_OUTPUT_FORMATS) {
                supportedFormat.append(s).append('\n');
            }
            throw new CstlServiceException("The server does not support this output format: " + format + '\n' +
                                             " supported ones are: " + '\n' + supportedFormat.toString(),
                                             INVALID_PARAMETER_VALUE, "outputFormat");
        } else {
            return MimeType.APPLICATION_XML;
        }
    }

    /**
     * Marshall an object into a DOM node.
     *
     * @param obj Object to transform in Node.
     * @param pool Marshaller Pool handling the object marshalling.
     */
    public static Node transformToNode(final Object obj, final MarshallerPool pool) throws CstlServiceException {
        if (obj instanceof Node) {
            return (Node) obj;
        } else {
            try {
                // special case for RecordType
                if (obj instanceof RecordType) {
                    return NodeUtilities.getNodeFromGeotkMetadata(obj, CSWMarshallerPool.getInstance());
                }
                return NodeUtilities.getNodeFromGeotkMetadata(obj, pool);
            } catch (JAXBException | ParserConfigurationException ex) {
                throw new CstlServiceException("Unable to parse the object.", ex);
            }
        }
    }

    public static void updateOpensearchURL(AbstractOperationsMetadata meta, String osUrl) {
        AbstractOperation op = meta.getOperation("GetRecords");
        if (op != null) {
            AbstractDomain cst = op.getConstraint("OpenSearchDescriptionDocument");
            if (cst != null) {
                cst.setValue(Arrays.asList(osUrl));
            }
        }
    }

    public static void updateCswURL(OpenSearchDescription desc, String cswUrl) {
        if (desc != null) {
            for (Url url : desc.getUrl()) {
                url.setTemplate(url.getTemplate().replace("{cswUrl}", cswUrl));
            }
        }
    }

    public static Filter BuildTemporalFilter(String filterVersion, String operator, Literal timeStart, Literal timeEnd) throws CstlServiceException {

        switch (operator) {
            case "During" :
            case "AnyInteracts" :
                // the records period included in the period
                Filter f1 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_begin", timeStart, false);
                Filter f2 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_end",   timeEnd,   false);
                Filter and1 = FilterXmlFactory.buildAnd(filterVersion, f1, f2);

                // the records instant included in the period
                f1 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_begin", timeEnd,   false);
                Filter f3 = FilterXmlFactory.buildPropertyIsNull(filterVersion,          "TemporalExtent_end");
                Filter and2 = FilterXmlFactory.buildAnd(filterVersion, f1, f2, f3);

                // the records period which overlaps the first bound
                f1 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_end",   timeEnd,   false);
                f3 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_end",   timeStart, false);
                Filter and3 = FilterXmlFactory.buildAnd(filterVersion, f1, f2, f3);

                // the records period which overlaps the second bound
                f1 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_end",   timeEnd,   false);
                f3 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_begin", timeEnd,   false);
                Filter and4 = FilterXmlFactory.buildAnd(filterVersion, f1, f2, f3);

                // the records period which overlaps the whole period
                f1 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,    "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_end",   timeEnd,   false);

                Filter and5 = FilterXmlFactory.buildAnd(filterVersion, f1, f2);

                return FilterXmlFactory.buildOr(filterVersion, and1, and2, and3, and4, and5);

            case "After" :
                return FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion,    "TemporalExtent_end",   timeEnd,   false);
            case "Before" :
                return FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion,       "TemporalExtent_end",   timeStart,   false);
            case "TEquals" :
                f1 = FilterXmlFactory.buildPropertyIsEquals(filterVersion, "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsEquals(filterVersion, "TemporalExtent_end",   timeEnd,   false);
                return FilterXmlFactory.buildAnd(filterVersion, f1, f2);
            case "Begins" :
            case "BegunBy" :
                return FilterXmlFactory.buildPropertyIsEquals(filterVersion, "TemporalExtent_begin", timeStart, false);
            case "EndedBy" :
            case "Ends" :
                return FilterXmlFactory.buildPropertyIsEquals(filterVersion, "TemporalExtent_end", timeEnd, false);
            case "TContains" :
            case "TOverlaps" :
                f1 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion, "TemporalExtent_end",   timeEnd,   false);
                return FilterXmlFactory.buildAnd(filterVersion, f1, f2);
            case "OverlappedBy" :
                f1 = FilterXmlFactory.buildPropertyIsLessThanOrEqualTo(filterVersion, "TemporalExtent_begin", timeStart, false);
                f2 = FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo(filterVersion, "TemporalExtent_end",   timeEnd,   false);
                return FilterXmlFactory.buildAnd(filterVersion, f1, f2);
            case "Meets" :
            case "MetBy" :
            default: throw new CstlServiceException("Temporal operator:" + operator + " is not supported yet.");
        }
    }

    public static Filter BuildSearchTermsFilter(String filterVersion, String searchTerms) throws CstlServiceException {

        final List<Filter> filters = new ArrayList<>();

        Filter previous = null;
        List<Filter> orFilters = new ArrayList<>();
        boolean orFilter = false;
        boolean exactFilter = false;

        while (!searchTerms.isEmpty()) {
            if (searchTerms.startsWith("+")) {
                searchTerms = searchTerms.substring(1);
                orFilter = true;
                orFilters.add(previous);

            } else if (searchTerms.startsWith(" ")) {

                // ending OR chain
                if (orFilter) {
                    orFilters.add(previous);
                    filters.add(FilterXmlFactory.buildOr(filterVersion, orFilters.toArray(new Object[orFilters.size()])));
                    orFilters = new ArrayList<>();
                    orFilter = false;
                } else {
                    filters.add(previous);
                }
                searchTerms = searchTerms.substring(1);

            } else if (searchTerms.startsWith("\"")) {
                int endPos = searchTerms.indexOf('"', 1);
                if (endPos == -1) {
                    throw new CstlServiceException("Invalid search terms, missing ending double quote.", INVALID_PARAMETER_VALUE, "q");
                }
                exactFilter = true;
                searchTerms =  searchTerms.substring(1, endPos);
            }

            String term = getTerm(searchTerms);

            // filter for the term
            final List<Filter> termFilters = new ArrayList<>();
            if (exactFilter) {
                termFilters.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "dc:title",       FilterXmlFactory.buildLiteral(filterVersion, term), true));
                termFilters.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "dc:description", FilterXmlFactory.buildLiteral(filterVersion, term), true));
                termFilters.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "dc:subject",     FilterXmlFactory.buildLiteral(filterVersion, term), true));
            } else {
                termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:title",       "*" + term + "*", "*", "?", "\\"));
                termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:description", "*" + term + "*", "*", "?", "\\"));
                termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:subject",     "*" + term + "*", "*", "?", "\\"));
            }
            previous = FilterXmlFactory.buildOr(filterVersion, termFilters.toArray(new Object[termFilters.size()]));
            searchTerms = searchTerms.substring(term.length());
        }

        if (orFilter) {
            orFilters.add(previous);
            filters.add(FilterXmlFactory.buildOr(filterVersion, orFilters.toArray(new Object[orFilters.size()])));
        } else {
            filters.add(previous);
        }

        if (filters.isEmpty()) {
            return null;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return FilterXmlFactory.buildAnd(filterVersion, filters.toArray(new Object[filters.size()]));
        }
    }

    private static String getTerm(String searchTerms) throws CstlServiceException {
        // determine the next separator
        int spacePos = searchTerms.indexOf(' ');
        int plusPos  = searchTerms.indexOf('+');

        // no more terms
        if (spacePos == -1 && plusPos == -1) {
            return searchTerms;
        } else if (spacePos == -1 && plusPos != -1) {
            return searchTerms.substring(0, plusPos);
        }  else if (spacePos != -1 && plusPos == -1) {
            return searchTerms.substring(0, spacePos);
        } else {
            int pos = Math.min(plusPos, spacePos);
            return searchTerms.substring(0, pos);
        }
    }

    /**
     * @deprecated remove when the information will be availlable in RecordInfo.isISOTransformable
     */
    @Deprecated
    private static boolean isISOconvertible(MetadataType mode) {
        // for now only DIF is applicable
        return  mode ==  MetadataType.DIF;
     }

    public static EntryType getEntryFromRecordInfo(String serviceUrl, RecordInfo record, String entrySearchLink) {

        ISODateParser parser = new ISODateParser();
        EntryType entry = new EntryType();
        final List<String> identifierValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:identifier");
        if (!identifierValues.isEmpty()) {
            entry.addId(new IdType(identifierValues.get(0)));
        }

        // add alternate links
        String openSearchUrl = serviceUrl.substring(0, serviceUrl.length() -1);
        String atomUrl = GET_ATOM_BY_ID.replace("{SURL}", openSearchUrl).replace("{MID}", record.identifier);
        entry.addLink(new LinkType(atomUrl, "Atom format", "alternate", "application/atom+xml"));

        String outputschema = record.originalFormat.namespace;
        String cswUrl = GET_RECORD_BY_ID.replace("{SURL}", serviceUrl).replace("{MID}", record.identifier).replace("{OUT_SCHEME}", outputschema);
        entry.addLink(new LinkType(cswUrl, "Native format", "alternate", "application/xml"));

        // if an iso transformation is available add alternate link
        if (isISOconvertible(record.originalFormat)) {
            String cswISOUrl = GET_RECORD_BY_ID.replace("{SURL}", serviceUrl).replace("{MID}", record.identifier).replace("{OUT_SCHEME}", MetadataType.ISO_19115.namespace);
            entry.addLink(new LinkType(cswISOUrl, "ISO format", "via", "application/vnd.iso.19139-2+xml"));
        }

        final List<String> relationValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:references");
        if (entrySearchLink != null) {
            entry.addLink(new LinkType(entrySearchLink, "Granule search", "search", "application/opensearchdescription+xml"));
        } else {
            for (String relationValue : relationValues) {
                entry.addLink(new LinkType(relationValue, "enclosure", null));
            }
        }

        final List<String> titleValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:title");
        if (!titleValues.isEmpty()) {
            entry.addTitle(new TextType(titleValues.get(0)));
        }
        final List<String> creatorValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:creator");
        if (!creatorValues.isEmpty()) {
            entry.addAuthor(new PersonType(creatorValues.get(0), null, null));
        }
        final List<String> subValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:subject");
        for (String sub : subValues) {
            entry.addCategory(new CategoryType(sub, null));
        }
        final List<String> absValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:abstract");
        if (!absValues.isEmpty()) {
            entry.addSummary(new TextType(absValues.get(0)));
        }
        final List<String> contValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:contributor");
        for (String cont : contValues) {
            entry.addContributor(new PersonType(cont, null, null));
        }
        final List<String> dateValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:date");
        if (!dateValues.isEmpty()) {
            try {
                GregorianCalendar c = new GregorianCalendar();
                c.setTime(parser.parseToDate(dateValues.get(0)));
                XMLGregorianCalendar xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
                entry.addUpdated(new DateTimeType(xgcal));
            } catch (DatatypeConfigurationException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }

        final List<String> beginValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/csw:TemporalExtent/csw:begin");
        final List<String> endValues   = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/csw:TemporalExtent/csw:end");

        org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory dcFactory = new org.geotoolkit.dublincore.xml.v2.elements.ObjectFactory();

        // add dc:identifier
        if (!identifierValues.isEmpty()) {
            entry.getAuthorOrCategoryOrContent().add(dcFactory.createIdentifier(new SimpleLiteral(identifierValues.get(0))));
        }

        // add dc:date
        if (!beginValues.isEmpty() && !endValues.isEmpty()) {
            Date start = parser.parseToDate(beginValues.get(0));
            Date end   = parser.parseToDate(endValues.get(0));
            synchronized(ISO8601_FORMAT) {
                String content = ISO8601_FORMAT.format(start) + "/" + ISO8601_FORMAT.format(end);
                entry.getAuthorOrCategoryOrContent().add(dcFactory.createDate(new SimpleLiteral(content)));
            }
        }

        // TODO source ?

        final List<String> languageValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:language");
        if (!languageValues.isEmpty()) {
            entry.setLang(languageValues.get(0));
        }
        final List<String> rightsValues = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/dc:rights");
        for (String sub : rightsValues) {
            entry.addRight(new TextType(sub));
        }
        final List<String> lowers = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/ows:BoundingBox/ows:LowerCorner");
        final List<String> uppers = NodeUtilities.getValuesFromPath(record.node, "/csw:Record/ows:BoundingBox/ows:UpperCorner");
        if (!lowers.isEmpty() && !uppers.isEmpty()) {
            String[] lo = lowers.get(0).split(" ");
            String[] up = uppers.get(0).split(" ");
            if (lo.length == 2 && up.length == 2) {
                entry.addBox(Arrays.asList(Double.parseDouble(lo[0]),
                                           Double.parseDouble(lo[1]),
                                           Double.parseDouble(up[0]),
                                           Double.parseDouble(up[1])));
            }
        }

        // TODO relation
        return entry;
    }

    public static void addNavigationRequest(GetRecordsRequest request, FeedType feed, String selfRequest) {
        int startPosition = request.getStartPosition();
        int count = request.getMaxRecords();
        int totalResult = feed.getTotalResults();

        // first
        String firstRequest = replaceStartPosition(selfRequest, 1);
        LinkType firstLink = new LinkType(firstRequest, "first", MimeType.APP_ATOM);
        feed.addLink(firstLink);

        // previous
        if ((startPosition - count) > 0) {
            String prevRequest = replaceStartPosition(selfRequest, startPosition - count);
            LinkType prevLink = new LinkType(prevRequest, "prev", MimeType.APP_ATOM);
            feed.addLink(prevLink);
        }

        //self
        LinkType selfLink = new LinkType(selfRequest, "self", MimeType.APP_ATOM);
        feed.addLink(selfLink);

        // next
        if ((startPosition + count) < totalResult) {
            String nextRequest = replaceStartPosition(selfRequest, startPosition + count);
            LinkType nextLink = new LinkType(nextRequest, "next", MimeType.APP_ATOM);
            feed.addLink(nextLink);
        }

        //last
        int lastStart = ((totalResult / count) * count) + 1;
        String lastRequest = replaceStartPosition(selfRequest, lastStart);
        LinkType lastLink = new LinkType(lastRequest, "last", MimeType.APP_ATOM);
        feed.addLink(lastLink);

    }

    private static final String START_PARAM = "startPosition=";

    private static String replaceStartPosition(String request, int startPosition) {
        int index = request.indexOf(START_PARAM);
        if (index != -1) {
            String firstPart = request.substring(0, index + START_PARAM.length());
            int index2 = request.indexOf("&", index);
            if (index2 != -1) {
                String lastPart = request.substring(index2);
                return firstPart + startPosition + lastPart;
            } else {
                return firstPart + startPosition;
            }
        } else {
            return request + '&' + START_PARAM + startPosition;
        }
    }
}
