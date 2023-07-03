/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.geotoolkit.coverage.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author guilhem
 */
@XmlRootElement(name = "ConformsTo")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Conformance extends CoverageResponse {

    private List<String> conformsTo;

    public Conformance() {
        conformsTo = new ArrayList<>();
    }

    public Conformance(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    /**
     * @return the array list of link
     */
    public List<String> getConformsTo() {
        return conformsTo;
    }

    /**
     * @param conformsTo the array list to set
     */
    public void setConformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }
}
