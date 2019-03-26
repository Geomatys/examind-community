/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.json.view;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Bean view which reduce the number of fields serialized in json.
 * 
 * @author Johann Sorel (Geomatys)
 */
@JsonSerialize(using = JsonViewSerializer.class)
public class JsonView {
    
    final Object object;
    final JsonFilter filter;

    public JsonView(Object object, String ... includes) {
        this.object = object;
        this.filter = new JsonFilter();
        this.filter.put(includes);
    }
    
    private JsonView(Object object, JsonFilter filter) {
        this.object = object;
        this.filter = filter;
    }
    
    /**
     * Decorates a collection of beans.
     * 
     * @param records
     * @param includes
     * @return 
     */
    public static Collection map(Collection records, String ... includes) {
        if(includes==null ||includes.length==0) return records;
        
        final JsonFilter filter = new JsonFilter();
        filter.put(includes);
        return map(records,filter);
    }
    
    /**
     * Decorates a collection of beans.
     * 
     * @param records
     * @param filter
     * @return 
     */
    public static Collection map(final Collection records, final JsonFilter filter) {
        if(filter==null) return records;
        
        return new AbstractCollection() {
            @Override
            public Iterator iterator() {
                final Iterator ite = records.iterator();
                return new Iterator() {
                    @Override
                    public boolean hasNext() {
                        return ite.hasNext();
                    }

                    @Override
                    public Object next() {
                        return new JsonView(ite.next(), filter);
                    }
                };
            }
            @Override
            public int size() {
                return records.size();
            }
        };
    }
    
}
