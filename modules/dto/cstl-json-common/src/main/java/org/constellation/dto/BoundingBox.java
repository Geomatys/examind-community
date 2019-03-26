package org.constellation.dto;

import java.io.Serializable;

/**
 * Simple DTO that expose coordinates of extent.
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class BoundingBox implements Serializable {

    private double minx;
    private double miny;
    private double maxx;
    private double maxy;

    private CoordinateReferenceSystem crs;

    public BoundingBox() {

    }

    public BoundingBox(double minx, double miny, double maxx, double maxy, CoordinateReferenceSystem crs) {
        this.minx = minx;
        this.miny = miny;
        this.maxx = maxx;
        this.maxy = maxy;
        this.crs = crs;
    }

    public double getMaxx() {
        return maxx;
    }

    public double getMaxy() {
        return maxy;
    }

    public double getMinx() {
        return minx;
    }

    public double getMiny() {
        return miny;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public void setMaxx(double maxx) {
        this.maxx = maxx;
    }

    public void setMaxy(double maxy) {
        this.maxy = maxy;
    }

    public void setMinx(double minx) {
        this.minx = minx;
    }

    public void setMiny(double miny) {
        this.miny = miny;
    }
}
