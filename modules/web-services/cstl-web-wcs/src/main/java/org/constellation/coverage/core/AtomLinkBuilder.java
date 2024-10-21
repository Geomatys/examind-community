package org.constellation.coverage.core;

import org.constellation.ws.MimeType;
import org.geotoolkit.atom.xml.Link;

import java.util.List;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class AtomLinkBuilder {

    public static void buildDocumentLinks(String url, boolean asJson, List<Link> links, boolean specialMime) {
        String xmlMime, jsonMime;
        if (specialMime) {
            xmlMime  = MimeType.APP_XML;
            jsonMime = MimeType.APP_JSON;
        } else {
            xmlMime  = MimeType.APP_XML;
            jsonMime = MimeType.APP_JSON;
        }
        Link linkSelf = new Link(url,                        "self", jsonMime, "this document");
        Link linkAlt  = new Link(url + "?f=application/xml", "self", xmlMime,  "this document");
        String titleSuffix;
        if (asJson) {
            titleSuffix = " as XML";
        } else {
            Link tmp = linkSelf;
            linkSelf = linkAlt;
            linkAlt = tmp;
            titleSuffix =  " as JSON";
        }
        linkAlt.setRel("alternate");
        linkAlt.setTitle(linkAlt.getTitle() + titleSuffix);
        links.add(linkSelf);
        links.add(linkAlt);
    }

    public static void buildCollectionLink(String url, List<Link> links) {
        links.add(new Link(url,                        "collection", MimeType.APP_JSON,    "the collection document as JSON"));
        links.add(new Link(url + "?f=application/xml", "collection", MimeType.APPLICATION_XML, "the collection document as XML"));
    }

    public static void BuildCoverageLink(String url, String identifier, String title, List<Link> links) {
        links.add(new Link(url + "/collections/" + identifier + "/coverage",                    "items", MimeType.IMAGE_TIFF, title));
        links.add(new Link(url + "/collections/" + identifier + "/coverage?f=image/tiff",       "items", MimeType.IMAGE_TIFF, title));
        links.add(new Link(url + "/collections/" + identifier + "/coverage?f=application/x-netcdf", "items", MimeType.NETCDF, title));
    }
}
