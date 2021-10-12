package org.constellation.dto.wms;

import org.constellation.dto.BoundingBox;

import java.io.Serializable;
import java.util.List;

/**
 * Simple DTO that represents layer.
 *
 * @author Mehdi Sidhoum (Geomatys).
 */
public class WMSLayer implements Serializable {

    private String name;
    private String title;
    private String resume;
    private List<StyleDTO> styles;
    private BoundingBox boundingBox;
    private String version;

    public WMSLayer() {
    }

    public WMSLayer(String name, String title, String resume, List<StyleDTO> styles, BoundingBox boundingBox,String version) {
        this.name = name;
        this.title = title;
        this.resume = resume;
        this.styles = styles;
        this.boundingBox = boundingBox;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public List<StyleDTO> getStyles() {
        return styles;
    }

    public void setStyles(List<StyleDTO> styles) {
        this.styles = styles;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
