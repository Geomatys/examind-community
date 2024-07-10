
package org.geotoolkit.wps.json;

import java.util.List;

/**
 *
 * @author glegal
 */
public class DescribeProcess implements WPSJSONResponse {
 
    private List<Input> inputs = null;
    
    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }
}
