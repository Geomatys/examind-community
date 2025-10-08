package com.examind.openeo.api.rest.capabilities;

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
}