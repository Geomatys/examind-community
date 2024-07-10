
package org.geotoolkit.wps.xml.v200;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import org.geotoolkit.ows.xml.v200.CodeType;

/**
 *
 * @author glegal
 */
public class ProcessInput {
 
    @XmlElement(name = "Identifier", namespace = "http://www.opengis.net/ows/2.0", required = true)
    private CodeType identifier;
    
    private List<DataInput> input;
    
    public ProcessInput() {
        
    }
    
    public ProcessInput(CodeType identifier, List<DataInput> input) {
        this.identifier = identifier;
        this.input = input;
    }
    
    public CodeType getIdentifier() {
        return identifier;
    }

    public void setIdentifier(CodeType identifier) {
        this.identifier = identifier;
    }

    public List<DataInput> getInput() {
        return input;
    }

    public void setInput(List<DataInput> input) {
        this.input = input;
    }
}
