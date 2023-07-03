/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.api.rest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.Filter;
import org.constellation.dto.PagedSearch;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProviders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.constellation.dto.ExceptionReport;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.coverage.xml.Collection;
import org.geotoolkit.coverage.xml.Collections;
import org.geotoolkit.coverage.xml.Conformance;
import org.geotoolkit.coverage.xml.Extent;
import org.geotoolkit.coverage.xml.LandingPage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <em>WARNING:</em> Beware of transaction management. For now, rather use {@link SpringHelper#executeInTransaction(TransactionCallback) manual transactions}
 * than automated annotation-based transactions, because:
 * <ul>
 *     <li>Transaction annotation might not be processed, because this component is setup by a servlet context, not
 *     by the main application context.</li>
 *     <li>Default transaction annotation configuration only triggers rollback upon runtime exception or fatal error.</li>
 * </ul>
 *
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class CoverageAPI extends AbstractRestAPI {

    public static final String SPECIFICATION_URL = "https://docs.ogc.org/DRAFTS/19-087.html";
    private static final String SERVICE_TITLE = "Examind OGC Coverage API service";
    private static final String SERVICE_DESCRIPTION = "Access Examind coverage data via a Web API that conforms to the OGC Coverage API specification";
    private static final String API_PATH = "/API/coverages/";
    private final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final List<String> CONFORMS = Arrays.asList(
            "https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core",
            "https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/collections",
            "https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas3",
            "https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html",
            "https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/geojson",
            "https://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/core"
    );

    @Autowired
    private IDataBusiness dataBusiness;

    /**
     * GET /coverages/
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-coverages/index.html#tag/Capabilities
     *
     * @return
     */
    @RequestMapping(value = "/coverages", method = GET)
    public ResponseEntity landingPage(HttpServletRequest req, @RequestParam(name = "f", defaultValue = "json") String f) {
        String url = getServiceURL(req, false);
        LandingPage response = new LandingPage();
        response.setTitle(SERVICE_TITLE);
        response.setDescription(SERVICE_DESCRIPTION);
        List<Link> links = new ArrayList<>();
        Link self = new Link();
        self.setHref(uri(url, API_PATH));
        self.setRel("self");
        self.setType("application/json");
        self.setTitle("this document");
        links.add(self);

        Link serviceDesc = new Link();
        serviceDesc.setHref(uri(url, API_PATH, "/api"));
        serviceDesc.setRel("service-desc");
        serviceDesc.setType("application/openapi+json;version=3.0");
        serviceDesc.setTitle("the API definition");
        links.add(serviceDesc);

        Link conformance = new Link();
        conformance.setHref(uri(url, API_PATH, "/conformance"));
        conformance.setRel("conformance");
        conformance.setType("application/json");
        conformance.setTitle("OGC conformance classes implemented by this API");
        links.add(conformance);

        Link collections = new Link();
        collections.setRel("http://www.opengis.net/def/rel/ogc/1.0/data");
        collections.setType("application/json");
        collections.setHref(uri(url, API_PATH, "/collections"));
        collections.setTitle("Metadata about the resource collections");
        links.add(collections);

        response.setLinks(links);

        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(response, mt, HttpStatus.OK).getResponseEntity();
    }

    /**
     * GET /coverages/api
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-coverages/index.html#tag/Capabilities/paths/~1api/get
     *
     * @return
     * @throws ConstellationException
     */
    @RequestMapping(value = "/coverages/api", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RedirectView api() throws ConstellationException {
        return new RedirectView(SPECIFICATION_URL);
    }

    /**
     * GET /coverages/conformance
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-coverages/index.html#tag/Capabilities/paths/~1conformance/get
     *
     * @param f, "json" or "html"
     *           The optional f parameter indicates the output format which the server
     *           shall provide as part of the response document. It has preference
     *           over the HTTP Accept header. The default format is JSON.
     * @return
     */
    @RequestMapping(value = "/coverages/conformance", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity conformance(@RequestParam(value = "f", required = false, defaultValue = "json") String f) {
        Conformance response = new Conformance();
        response.setConformsTo(CONFORMS);
        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(response, mt, HttpStatus.OK).getResponseEntity();
    }

    /**
     * GET /coverages/collections
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-coverages/index.html#operation/describeCollections
     *
     * @param limit [ 1 .. 10000 ],
     *              The optional limit parameter limits the number of collections that are presented in the response document.
     *              Only items are counted that are on the first level of the collection in the response document.
     *              Nested objects contained within the explicitly requested items shall not be counted.
     * @param bbox  [ 4 .. 6 ] items,
     *              Only collections that have a geometry that intersects the bounding box are selected.
     *              The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (elevation or depth):
     *              Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Lower left corner, coordinate axis 3 (optional)
     *              Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis 2 * Upper right corner, coordinate axis 3 (optional)
     *              The coordinate reference system of the values is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate reference system is specified in the parameter bbox-crs.
     *              For WGS84 longitude/latitude the values are in most cases the sequence of minimum longitude, minimum latitude, maximum longitude and maximum latitude.
     *              However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge).
     *              If a collection has multiple spatial geometry properties, it is the decision of the server whether only a single spatial geometry property is used to determine the extent or all relevant geometries.
     * @param time  Either a date-time or a period string that adheres to RFC 3339. Examples:
     *              A date-time: "2018-02-12T23:20:50Z"
     *              A period: "2018-02-12T00:00:00Z/2018-03-18T12:31:12Z" or "2018-02-12T00:00:00Z/P1M6DT12H31M12S"
     *              Only collections that have a temporal property that intersects the value of time are selected.
     *              If a collection has multiple temporal properties, it is the decision of the server whether only a single temporal property is used to determine the extent or all relevant temporal properties.
     * @param f,    "json" or "html"
     *              The optional f parameter indicates the output format which the server
     *              shall provide as part of the response document. It has preference
     *              over the HTTP Accept header. The default format is JSON.
     * @return
     * @throws ConstellationException
     */
    @RequestMapping(value = "/coverages/collections", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity collections(
            HttpServletRequest req,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(value = "bbox", required = false, defaultValue = "") String bbox,
            @RequestParam(value = "time", required = false, defaultValue = "") String time,
            @RequestParam(value = "f", required = false, defaultValue = "json") String f) throws ConstellationException {

        String url = getServiceURL(req, false);
        Collections response = new Collections();
        List<Collection> collections = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        Link link1 = new Link();
        link1.setTitle("this document");
        link1.setHref(uri(url, API_PATH, "/collections"));
        link1.setRel("self");
        link1.setType("application/json");
        links.add(link1);

        // Create and fill PagedSearch Object
        PagedSearch pagedSearch = new PagedSearch();
        pagedSearch.setPage(1);
        pagedSearch.setSize(limit);
        List<Filter> filters = new ArrayList<>();
        Filter filter = new Filter();
        filter.setField("type");
        filter.setValue("COVERAGE");
        filters.add(filter);
        pagedSearch.setFilters(filters);

        //filters
        final Map<String, Object> filterMap = prepareFilters(pagedSearch, req);
        //pagination
        final int pageNumber = pagedSearch.getPage();
        final int rowsPerPage = pagedSearch.getSize();

        // Perform search.
        final Map.Entry<Integer, List<DataBrief>> entry = dataBusiness.filterAndGetBrief(filterMap, null, pageNumber, rowsPerPage);

        List<DataBrief> results = entry.getValue();
        for (DataBrief data : results) {
            try {
                final Collection collection = getCollection(data, url);
                collections.add(collection);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error while getting collection from data:" + data.getId(), ex);
            }
        }

        response.setCollections(collections);
        response.setLinks(links);
        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(response, mt, HttpStatus.OK).getResponseEntity();
    }

    /**
     * GET /coverages/collections/{coverageId}
     * <p>
     * reference : https://opengeospatial.github.io/architecture-dwg/api-coverages/index.html#operation/describeCollections
     *
     * @param coverageId Identifier (name) of a specific collection
     * @return
     * @throws ConstellationException
     */
    @RequestMapping(value = "/coverages/collections/{coverageId}", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCoverageMetadata(HttpServletRequest req,
                                              @PathVariable(value = "coverageId") int coverageId,
                                              @RequestParam(value = "f", required = false, defaultValue = "json") String f) throws ConstellationException {
        if (!dataBusiness.existsById(coverageId)) {
            String responseCode = Integer.toString(HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(new ExceptionReport("Cannot find coverage data with is : " + coverageId, responseCode), HttpStatus.NOT_FOUND);
        }
        String url = getServiceURL(req, false);
        DataBrief data = dataBusiness.getDataBrief(coverageId, true, false);
        final Collection collection = getCollection(data, url);
        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(collection, mt, HttpStatus.OK).getResponseEntity();
    }


    @Override
    protected Map.Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        Map.Entry<String, Object> result = super.transformFilter(f, req);
        if (result != null) {
            return result;
        }
        String value = f.getValue();
        if (value == null || "_all".equals(value)) {
            return null;
        }
        if ("hasVectorData".equals(f.getField()) || "hasCoverageData".equals(f.getField()) || "hasLayerData".equals(f.getField()) ||
                "hasSensorData".equals(f.getField()) || "excludeEmpty".equals(f.getField())) {

            return new AbstractMap.SimpleEntry<>(f.getField(), Boolean.valueOf(value));

        } else if ("id".equals(f.getField())) {
            try {
                final int parentId = Integer.parseInt(value);
                return new AbstractMap.SimpleEntry<>("id", parentId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Filter by " + f.getField() + " value should be an integer: " + ex.getLocalizedMessage(), ex);
                return null;
            }

            // just here to list the existing filter
        } else if ("term".equals(f.getField())) {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    private Collection getCollection(DataBrief data, String url) throws ConfigurationException, ConstellationStoreException {
        Collection collection = new Collection();
        final Integer dataId = data.getId();
        collection.setId(dataId);
        collection.setName(data.getName());
        collection.setTitle(data.getTitle());
        collection.setDescription(data.getTitle());
        List<Link> linksData = new ArrayList<>();
        Link l1 = new Link();
        l1.setHref(uri(url, API_PATH, "/collections/", dataId.toString()) + "?f=json");
        l1.setRel("self");
        l1.setType("application/json");
        l1.setTitle(data.getTitle());
        linksData.add(l1);

        Link l2 = new Link();
        l2.setHref(uri(url, API_PATH, "/collections/", dataId.toString(), "/coverage"));
        l2.setRel("coverage");
        l2.setType("application/json");
        l2.setTitle(data.getTitle());
        linksData.add(l2);
        collection.setLinks(linksData);
        Extent extent = new Extent();
        extent.setCrs("http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        DataDescription dataDescription = data.getDataDescription();
        if (dataDescription != null) {
            extent.setSpatial(dataDescription.getBoundingBox());
        }
        // Fill time dimensions if it exist
        final SortedSet availableTimes = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName()).getAvailableTimes();
        if (availableTimes != null && !availableTimes.isEmpty()) {
            List<String> times = new ArrayList<>();
            for (Object time : availableTimes) {
                times.add(ISO8601_FORMAT.format(time));
            }
            extent.setTrs("http://www.opengis.net/def/uom/ISO-8601/0/Gregorian");
            extent.setTemporal(times);
        }
        collection.setExtent(extent);
        collection.setCrs(Arrays.asList("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        return collection;
    }

    /**
     * Very specific workaround that builds a URI from a root (Ex: http://localhost:8080:myServer/) and the list of
     * path fragments (concatenated in order).
     * The only cleanup/check done is verifying that no concatenation of path fragment produces a doublon slash ('//').
     * <em>Warning:</em> path fragment content is <em>not</em> verified.
     *
     * @param base          URI root/start. Must not be null.
     * @param pathFragments Path fragments to append/concatenate to base URI. They must embed all necessary '/'
     *                      separators. If null or empty, base is directly returned.
     * @return Concatenation of input strings.
     * @see UriComponentsBuilder#path(String)
     */
    static String uri(final String base, String... pathFragments) {
        ensureNonNull("URI base", base);
        if (pathFragments == null || pathFragments.length < 1) return base;

        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);
        for (String fragment : pathFragments) builder.path(fragment);
        return builder.build(true).toUriString();
    }
}
