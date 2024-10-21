package com.examind.ogc.api.rest.common;

import org.geotoolkit.atom.xml.Link;

import java.util.List;

public interface ConformanceProvider {
    List<Link> getConformances();
}
