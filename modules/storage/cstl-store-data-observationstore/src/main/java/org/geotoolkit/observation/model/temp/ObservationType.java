
package org.geotoolkit.observation.model.temp;

/**
 *
 * @author glegal
 */
public enum ObservationType {
    
    SIMPLE,       // no main field
    TIMESERIES,   // time main field
    PROFILE,       // double main field (usually depth or pressure),
    TRAJECTORY,
    GRID
    
}
