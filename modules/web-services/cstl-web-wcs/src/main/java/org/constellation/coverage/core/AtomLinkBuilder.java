package org.constellation.coverage.core;

import org.constellation.ws.MimeType;
import org.geotoolkit.ogcapi.dto.common.Link;

import java.util.List;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class AtomLinkBuilder {

    public static void buildDocumentLinks(String url, List<Link> links) {
        links.add(new Link(url, "self", MimeType.APP_JSON, "en", "this document", null));
    }

    public static void buildCollectionLink(String url, List<Link> links) {
        links.add(new Link(url, "collection", MimeType.APP_JSON, "en", "the collection document as JSON", null));
    }

    public static void BuildCoverageLink(String url, String identifier, String title, List<Link> links) {
        links.add(new Link(url + "/collections/" + identifier + "/coverage",                    "items", MimeType.IMAGE_TIFF, "en", title, null));
        links.add(new Link(url + "/collections/" + identifier + "/coverage?f=image/tiff",       "items", MimeType.IMAGE_TIFF, "en", title, null));
        links.add(new Link(url + "/collections/" + identifier + "/coverage?f=application/x-netcdf", "items", MimeType.NETCDF, "en", title, null));
    }
}
