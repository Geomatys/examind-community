
package org.geotoolkit.coverage.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 *
 * @author guilhem
 */
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extent {

    private String crs;

    private double[] spatial;

    private String trs;

    private List<String> temporal;

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public double[] getSpatial() {
        return spatial;
    }

    public void setSpatial(double[] spatial) {
        this.spatial = spatial;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

    public List<String> getTemporal() {
        return temporal;
    }

    public void setTemporal(List<String> temporal) {
        this.temporal = temporal;
    }
}
