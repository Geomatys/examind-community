/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015-2016 Geomatys.
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
package org.constellation.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Mehdi Sidhoum (Geomatys).
 */
public class Page<T> implements Serializable {

    private int number;

    private int size;

    private long total;

    private Collection<T> content;


    public Page() {
        this(1, 0, 0, new ArrayList<T>());
    }

    public Page(int number, int size, long total, List<T> content) {
        this.number = number;
        this.size = size;
        this.total = total;
        this.content = content;
    }

    public int getNumber() {
        return number;
    }

    public Page<T> setNumber(int number) {
        this.number = number;
        return this;
    }

    public int getSize() {
        return size;
    }

    public Page<T> setSize(int size) {
        this.size = size;
        return this;
    }

    public long getTotal() {
        return total;
    }

    public Page<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public Collection<T> getContent() {
        return content;
    }

    public Page<T> setContent(Collection<T> content) {
        this.content = content;
        return this;
    }
}
