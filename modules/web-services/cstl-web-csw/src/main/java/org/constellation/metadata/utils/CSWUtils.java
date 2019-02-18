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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.geotoolkit.csw.xml.AbstractCswRequest;

import static org.constellation.metadata.core.CSWConstants.ACCEPTED_OUTPUT_FORMATS;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.ogc.xml.FilterXmlFactory;
import org.geotoolkit.ops.xml.v110.OpenSearchDescription;
import org.geotoolkit.ops.xml.v110.Url;
import org.geotoolkit.ows.xml.AbstractDomain;
import org.geotoolkit.ows.xml.AbstractOperation;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Literal;
import org.opengis.temporal.Instant;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CSWUtils {

     /**
     * Return the request (or default) outputFormat (MIME type) of the response.
     * if the format is not supported it throws a WebService Exception.
     *
     * @param request
     * @return the outputFormat (MIME type) of the response.
     * @throws CstlServiceException
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
            }

            String term = getTerm(searchTerms);

            // filter for the term
            final List<Filter> termFilters = new ArrayList<>();
            termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:title",       "*" + term + "*", "*", "?", "\\"));
            termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:description", "*" + term + "*", "*", "?", "\\"));
            termFilters.add(FilterXmlFactory.buildPropertyIsLike(filterVersion, "dc:subject",     "*" + term + "*", "*", "?", "\\"));
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
        if (searchTerms.startsWith("\"")) {
            int endPos = searchTerms.indexOf('"', 1);
            if (endPos == -1) {
                throw new CstlServiceException("Invalid search terms, missing ending double quote.", INVALID_PARAMETER_VALUE, "q");
            }
            return searchTerms.substring(0, endPos);
        } else {
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
    }
}
