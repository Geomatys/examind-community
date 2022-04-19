package org.constellation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * DTO for Scene3D web API.
 *
 * @author Johann Sorel (Geomatys)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scene {
    
    protected int publicationId;
    protected String title;
    protected Integer mapContext;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    protected Integer[] surface;
    protected String surfaceParameters;
    protected double surfaceFactor = 1.0;
    protected String format;
    protected String status;
    protected Date creationDate;
    protected int minLod = 0;
    protected int maxLod = 0;
    protected Double bboxMinX;
    protected Double bboxMinY;
    protected Double bboxMaxX;
    protected Double bboxMaxY;
    protected String time;
    protected String extras;
    protected Double vectorSimplifyFactor;

    private Integer dataId;
    private Integer layerId;

    public Scene() {

    }

    public Scene(Scene that) {
        if (that != null) {
            this.publicationId = that.publicationId;
            this.title = that.title;
            this.mapContext = that.mapContext;
            this.surface = that.surface;
            this.surfaceParameters = that.surfaceParameters;
            this.surfaceFactor = that.surfaceFactor;
            this.format  = that.format;
            this.status = that.status;
            this.creationDate = that.creationDate;
            this.minLod = that.minLod;
            this.maxLod = that.maxLod;
            this.bboxMinX = that.bboxMinX;
            this.bboxMinY = that.bboxMinY;
            this.bboxMaxX = that.bboxMaxX;
            this.bboxMaxY = that.bboxMaxY;
            this.time = that.time;
            this.extras = that.extras;
            this.vectorSimplifyFactor = that.vectorSimplifyFactor;
            this.dataId = that.dataId;
            this.layerId = that.layerId;
        }
    }

    public int getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(int publicationId) {
        this.publicationId = publicationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getMapContext() {
        return mapContext;
    }

    public void setMapContext(Integer mapContext) {
        this.mapContext = mapContext;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public int getMinLod() {
        return minLod;
    }

    public void setMinLod(int minLod) {
        this.minLod = minLod;
    }

    public int getMaxLod() {
        return maxLod;
    }

    public void setMaxLod(int maxLod) {
        this.maxLod = maxLod;
    }

    public Double getBboxMinX() {
        return bboxMinX;
    }

    public void setBboxMinX(Double bboxMinX) {
        this.bboxMinX = bboxMinX;
    }

    public Double getBboxMinY() {
        return bboxMinY;
    }

    public void setBboxMinY(Double bboxMinY) {
        this.bboxMinY = bboxMinY;
    }

    public Double getBboxMaxX() {
        return bboxMaxX;
    }

    public void setBboxMaxX(Double bboxMaxX) {
        this.bboxMaxX = bboxMaxX;
    }

    public Double getBboxMaxY() {
        return bboxMaxY;
    }

    public void setBboxMaxY(Double bboxMaxY) {
        this.bboxMaxY = bboxMaxY;
    }

    public Integer[] getSurface() {
        return surface;
    }

    public void setSurface(Integer[] surface) {
        this.surface = surface;
    }

    public Double getVectorSimplifyFactor() {
        return vectorSimplifyFactor;
    }

    public void setVectorSimplifyFactor(Double vectorSimplifyFactor) {
        this.vectorSimplifyFactor = vectorSimplifyFactor;
    }

    public String getSurfaceParameters() {
        return surfaceParameters;
    }

    public void setSurfaceParameters(String surfaceParameters) {
        this.surfaceParameters = surfaceParameters;
    }

    public String getTime() {
        return time;
    }

    /**
     * @return Exaggeration factor (scale) applied to the {@link #getSurface() surface (landscape)}.
     */
    public double getSurfaceFactor() {
        return surfaceFactor;
    }

    public void setSurfaceFactor(double surfaceFactor) {
        this.surfaceFactor = surfaceFactor;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getExtras() {
        return extras;
    }

    public void setExtras(String extras) {
        this.extras = extras;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.dataId);
        hash = 31 * hash + Objects.hashCode(this.layerId);
        hash = 31 * hash + Objects.hashCode(this.publicationId);
        hash = 31 * hash + Objects.hashCode(this.title);
        hash = 31 * hash + Objects.hashCode(this.mapContext);
        hash = 31 * hash + Objects.hashCode(this.surface);
        hash = 31 * hash + Objects.hashCode(this.surfaceParameters);
        hash = 31 * hash + Objects.hashCode(this.format);
        hash = 31 * hash + Objects.hashCode(this.status);
        hash = 31 * hash + Objects.hashCode(this.creationDate);
        hash = 31 * hash + this.minLod;
        hash = 31 * hash + this.maxLod;
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.bboxMinX) ^ (Double.doubleToLongBits(this.bboxMinX) >>> 32));
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.bboxMinY) ^ (Double.doubleToLongBits(this.bboxMinY) >>> 32));
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.bboxMaxX) ^ (Double.doubleToLongBits(this.bboxMaxX) >>> 32));
        hash = 31 * hash + (int) (Double.doubleToLongBits(this.bboxMaxY) ^ (Double.doubleToLongBits(this.bboxMaxY) >>> 32));
        hash = 31 * hash + Objects.hashCode(this.time);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Scene other = (Scene) obj;
        if (this.minLod != other.minLod) {
            return false;
        }
        if (this.maxLod != other.maxLod) {
            return false;
        }
        if (Double.doubleToLongBits(this.bboxMinX) != Double.doubleToLongBits(other.bboxMinX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.bboxMinY) != Double.doubleToLongBits(other.bboxMinY)) {
            return false;
        }
        if (Double.doubleToLongBits(this.bboxMaxX) != Double.doubleToLongBits(other.bboxMaxX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.bboxMaxY) != Double.doubleToLongBits(other.bboxMaxY)) {
            return false;
        }
        if (!Objects.equals(this.publicationId, other.publicationId)) {
            return false;
        }
        if (!Objects.equals(this.title, other.title)) {
            return false;
        }
        if (!Objects.equals(this.mapContext, other.mapContext)) {
            return false;
        }
        if (!Arrays.equals(this.surface, other.surface)) {
            return false;
        }
        if (!Objects.equals(this.surfaceParameters, other.surfaceParameters)) {
            return false;
        }
        if (!Objects.equals(this.format, other.format)) {
            return false;
        }
        if (!Objects.equals(this.status, other.status)) {
            return false;
        }
        if (!Objects.equals(this.creationDate, other.creationDate)) {
            return false;
        }
        if (!Objects.equals(this.time, other.time)) {
            return false;
        }
        if (!Objects.equals(this.extras, other.extras)) {
            return false;
        }
        return true;
    }

    /**
     * @return the dataId
     */
    public Integer getDataId() {
        return dataId;
    }

    /**
     * @param dataId the dataId to set
     */
    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    /**
     * @return the layerId
     */
    public Integer getLayerId() {
        return layerId;
    }

    /**
     * @param layerId the layerId to set
     */
    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }

}
