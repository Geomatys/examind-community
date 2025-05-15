package com.examind.ogc.api.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Quentin BIALOTA
 * @author Guilhem LEGAL
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extent {

    @JsonProperty("crs")
    private String crs;

    @JsonProperty("spatial")
    private SpatialCRS spatial;

    @JsonProperty("trs")
    private String trs;

    @JsonProperty("temporal")
    private TemporalCRS temporal;

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public SpatialCRS getSpatial() {
        return spatial;
    }

    public void setSpatial(SpatialCRS spatial) {
        this.spatial = spatial;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

    public TemporalCRS getTemporal() {
        return temporal;
    }

    public void setTemporal(TemporalCRS temporal) {
        this.temporal = temporal;
    }

    private String buildCompoundUri(List<String> components) {
        StringBuilder uri = new StringBuilder("http://www.opengis.net/def/crs-compound?");
        for (int i = 0; i < components.size(); i++) {
            uri.append(i + 1).append("=").append(components.get(i));
            if (i < components.size() - 1) uri.append("&");
        }
        return uri.toString();
    }

    public void setFromCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        List<String> components = new ArrayList<>();

        if (crs instanceof CompoundCRS compoundCRS) {
            for (CoordinateReferenceSystem component : compoundCRS.getComponents()) {
                if (component instanceof org.opengis.referencing.crs.TemporalCRS) {
                    this.trs = opengisCRS(component);
                } else {
                    components.add(opengisCRS(component));
                }
            }
            // Handle spatial + vertical combinations
            if (!components.isEmpty()) {
                this.crs = components.size() > 1
                        ? buildCompoundUri(components)
                        : components.get(0);
            }
        } else {
            this.crs = opengisCRS(crs);
        }
    }

    //TODO : add others CRS and they links to opengis
    private String opengisCRS(CoordinateReferenceSystem crs) {
        Identifier name = crs.getName();

        // Spatial CRSs
        if (crs instanceof GeographicCRS || crs instanceof ProjectedCRS) {
            if ("WGS 84".equalsIgnoreCase(name.getCode())) {
                return "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
            } else if ("ETRS89-extended / LAEA Europe".equalsIgnoreCase(name.getCode())) {
                return "https://www.opengis.net/def/crs/EPSG/0/3035";
            }
        }
        // Vertical CRSs
        else if (crs instanceof VerticalCRS) {
            if ("NAVD88".equalsIgnoreCase(name.getCode())) {
                return "https://www.opengis.net/def/crs/EPSG/0/5703";
            }
        }
        // Temporal CRSs
        else if (crs instanceof TemporalCRS) {
            if ("Java time".equalsIgnoreCase(name.getCode())) {
                return "https://www.opengis.net/def/crs/OGC/0/UnixTime";
            }
        }

        // Fallback: Use EPSG code if available
        return crs.getIdentifiers().isEmpty()
                ? name.getCode()
                : "https://www.opengis.net/def/crs/EPSG/0/" + crs.getIdentifiers().iterator().next().getCode();
    }

    public String getSrs() {
        List<String> parts = new ArrayList<>();
        if (crs != null) parts.add(crs);
        if (trs != null) parts.add(trs);

        return parts.isEmpty()
                ? null
                : (parts.size() > 1) ? buildCompoundUri(parts) : parts.get(0);
    }
}
