/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.wfs.core;

import java.util.List;
import static org.constellation.wfs.core.WFSConstants.GML_3_2_SF_MIME;
import org.constellation.ws.MimeType;
import org.geotoolkit.atom.xml.Link;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AtomLinkBuilder {

    public static void buildDescribedByLink(String wfsUrl, List<Link> links, String identifier) {
        String describeFeatureUrl = wfsUrl + "request=DescribeFeatureType&service=WFS&version=2.0.0";
        String titleSuffix = ".";
        if (identifier != null) {
            describeFeatureUrl = describeFeatureUrl + "&typeName=" + identifier;
            titleSuffix = " for "  + identifier + " type.";
        }

        links.add(new Link(describeFeatureUrl,                                             "describedBy", MimeType.APPLICATION_XML, "GML application schema"  + titleSuffix));
        links.add(new Link(describeFeatureUrl + "&outputformat=application/schema%2Bjson", "describedBy", MimeType.APP_JSON_SCHEMA, "JSON application schema" + titleSuffix));
    }

    public static void buildDocumentLinks(String url, boolean asJson, List<Link> links, boolean specialMime) {
        String xmlMime, jsonMime;
        if (specialMime) {
            xmlMime  = GML_3_2_SF_MIME;
            jsonMime = MimeType.APP_GEOJSON;
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
        links.add(new Link(url,                        "collection", MimeType.APP_GEOJSON,    "the collection document as JSON"));
        links.add(new Link(url + "?f=application/xml", "collection", MimeType.APPLICATION_XML, "the collection document as XML"));
    }

    public static void BuildItemsLink(String url, String identifier, String title, List<Link> links) {
        links.add(new Link(url + "/collections/" + identifier + "/items",                   "items", MimeType.APP_GEOJSON, title));
        links.add(new Link(url + "/collections/" + identifier + "/items?f=application/xml", "items", GML_3_2_SF_MIME,      title));
    }
}
