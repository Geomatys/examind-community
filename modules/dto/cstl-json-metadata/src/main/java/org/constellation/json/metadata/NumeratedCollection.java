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
package org.constellation.json.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class NumeratedCollection implements Collection{

    private final Collection original;
    private final Map<Integer, Object> valueMap = new HashMap<>();
    
    public NumeratedCollection(final Collection original) {
        this.original = original;
    }
    
    public Object get(Integer i) {
        return valueMap.get(i);
    }
    
    public void put(Integer i, Object obj) {
        valueMap.put(i, obj);
    }
    
    public void replace(int ordinal, Object newValue) {
        if (original instanceof List) {
            List list = (List) original;
            list.set(ordinal, newValue);
        } else {
            final Iterator it = original.iterator();
            Object old = it.next();
            for (int i = 0; i < ordinal; i++) {
                old = it.next();
            }
            original.remove(old);
            original.add(newValue);
        }
    }
    
    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return original.contains(o);
    }

    @Override
    public Iterator iterator() {
        return valueMap.values().iterator();
    }

    @Override
    public Object[] toArray() {
        return original.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return original.toArray(a);
    }

    @Override
    public boolean add(Object e) {
        return original.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return original.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        return original.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
        return original.addAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        return original.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
        return original.retainAll(c);
    }

    @Override
    public void clear() {
        original.clear();
    }
    
}
