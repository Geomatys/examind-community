/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2016, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.metadata.index.elasticsearch;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Sort {
    
    private final String field;
    
    private final String order;

    public Sort(final String field, String order) {
        this.order = order;
        this.field = field;
    }
    
    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the order
     */
    public String getOrder() {
        return order;
    }
}
