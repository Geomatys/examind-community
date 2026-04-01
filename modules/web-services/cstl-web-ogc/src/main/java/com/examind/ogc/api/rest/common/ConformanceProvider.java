package com.examind.ogc.api.rest.common;

import org.geotoolkit.ogcapi.model.common.Link;
import java.util.List;

public interface ConformanceProvider {
    List<Link> getConformances();
}
