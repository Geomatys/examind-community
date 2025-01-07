package com.examind.ogc.api.rest.common;

import com.examind.ogc.api.rest.common.dto.Collection;
import com.examind.ogc.api.rest.common.dto.Collections;
import com.examind.ogc.api.rest.common.dto.Conformance;
import com.examind.ogc.api.rest.common.dto.Extent;
import com.examind.ogc.api.rest.common.dto.LandingPage;
import com.examind.ogc.api.rest.common.dto.SpatialCRS;
import com.examind.ogc.api.rest.common.dto.TemporalCRS;
import jakarta.servlet.http.HttpServletRequest;
import org.constellation.api.rest.AbstractRestAPI;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.ExceptionReport;
import org.constellation.dto.Filter;
import org.constellation.dto.PagedSearch;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.DataProviders;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.constellation.util.Util.uri;

/**
 * @author Quentin BIALOTA
 */
@RestController
@RequestMapping("/ogc")
public class OGCCommonAPI extends AbstractRestAPI {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.api.rest");
    public static final String SPECIFICATION_URL = "https://docs.ogc.org/is/19-072/19-072.html";
    private static final String API_PATH = "/API/ogc/";
    private final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final List<Link> CONFORMS = java.util.Collections.unmodifiableList(Arrays.asList( //TODO : Conformity with Part 1 html, Part 2 simple-query & html
            new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core", null, null, null),
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/landing-page", null, null, null),
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json", null, null, null),
            //new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html", null, null, null), Doesn't conform
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas30", null, null, null),

            new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/collections", null, null, null),
            //new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/simple-query", null, null, null), Doesn't conform
            new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/json", null, null, null)
            //new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html", null, null, null), Doesn't conform
    ));

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private List<ConformanceProvider> conformanceProviders;

    /**
     * GET /
     * <p>
     * reference : https://developer.ogc.org/api/common/index.html#tag/server/operation/getLandingPage
     *
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity landingPage(HttpServletRequest req, @RequestParam(name = "f", defaultValue = "json") String f) {

        String url = getServiceURL(req, false);
        LandingPage response = new LandingPage();
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
     * GET /api
     * <p>
     * reference : https://developer.ogc.org/api/common/index.html#tag/server/paths/~1api/get
     *
     * @return
     */
    @RequestMapping(value = "/api", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RedirectView api() {
        return new RedirectView(SPECIFICATION_URL);
    }

    /**
     * GET /conformance
     * <p>
     * reference : https://developer.ogc.org/api/coverages/index.html#tag/Coverage/operation/getCoverageMetadata
     *
     * @param f, "json" or "html"
     *           The optional f parameter indicates the output format which the server
     *           shall provide as part of the response document. It has preference
     *           over the HTTP Accept header. The default format is JSON.
     * @return
     */
    @RequestMapping(value = "/conformance", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity conformance(@RequestParam(value = "f", required = false, defaultValue = "json") String f) {

        Conformance response = new Conformance();
        List<Link> conformances = new ArrayList<>(OGCCommonAPI.CONFORMS);

        //TODO: Uncomment when api ogc will be global (and no longer per service)
//        for (ConformanceProvider provider : conformanceProviders) {
//            conformances.addAll(provider.getConformances());
//        }

        response.setConformsTo(conformances);
        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(response, mt, HttpStatus.OK).getResponseEntity();
    }

    /**
     * GET /collections
     * <p>
     * reference : https://developer.ogc.org/api/common/index2.html#tag/OGC-API-Common/operation/getCollections
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
     */
    @RequestMapping(value = "/collections", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity collections(HttpServletRequest req,
                              @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
                              @RequestParam(value = "bbox", required = false, defaultValue = "") String bbox,
                              @RequestParam(value = "time", required = false, defaultValue = "") String time,
                              @RequestParam(value = "f", required = false, defaultValue = "json") String f) {

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
     * GET /collections/{collectionId}
     * <p>
     * reference : https://developer.ogc.org/api/common/index2.html#tag/OGC-API-Common/operation/describeCollection
     *
     * @param collectionId Identifier (name) of a specific collection
     * @return
     */
    @RequestMapping(value = "/collections/{collectionId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCoverageMetadata(HttpServletRequest req,
                                              @PathVariable(value = "collectionId") int collectionId,
                                              @RequestParam(value = "f", required = false, defaultValue = "json") String f) throws ConstellationException {

        if (!dataBusiness.existsById(collectionId)) {
            String responseCode = Integer.toString(HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(new ExceptionReport("Cannot find collection with is : " + collectionId, responseCode), HttpStatus.NOT_FOUND);
        }
        String url = getServiceURL(req, false);
        DataBrief data = dataBusiness.getDataBrief(collectionId, true, false);
        final Collection collection = getCollection(data, url);
        MediaType mt = MediaType.APPLICATION_JSON;
        if (f.equals("xml")) {
            mt = MediaType.APPLICATION_XML;
        }
        return new ResponseObject(collection, mt, HttpStatus.OK).getResponseEntity();
    }

    private Collection getCollection(DataBrief data, String url) throws ConfigurationException, ConstellationStoreException {

        Collection collection = new Collection();
        final Integer dataId = data.getId();
        collection.setId(String.valueOf(dataId));
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
            SpatialCRS spatialCRS = new SpatialCRS();
            double[] boundingBox = dataDescription.getBoundingBox();
            double[][] globalBoundingBox = new double[1][boundingBox.length];
            System.arraycopy(boundingBox, 0, globalBoundingBox[0], 0, boundingBox.length);
            spatialCRS.setBbox(globalBoundingBox);
            extent.setSpatial(spatialCRS);
        }

        // Fill time dimensions if it exist
        final SortedSet availableTimes = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName()).getAvailableTimes();
        if (availableTimes != null && !availableTimes.isEmpty()) {
            List<String> times = new ArrayList<>();
            for (Object time : availableTimes) {
                times.add(ISO8601_FORMAT.format(time));
            }
            extent.setTrs("http://www.opengis.net/def/uom/ISO-8601/0/Gregorian");
            TemporalCRS temporalCRS = new TemporalCRS();
            String[][] temporalInterval = new String[1][2];
            temporalInterval[0][0] = times.get(0);
            temporalInterval[0][1] = times.get(times.size()-1);
            temporalCRS.setInterval(temporalInterval);
            extent.setTemporal(temporalCRS);
        }

        collection.setExtent(extent);
        collection.setCrs(Arrays.asList("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));

        collection.setStorageCrs("http://www.opengis.net/def/crs/OGC/1.3/CRS84");

        return collection;
    }
}
