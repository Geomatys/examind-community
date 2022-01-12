/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private byte[]  content;
    private String  uri;
    private String  filename;

    public Attachment() {}

    public Attachment(Attachment value) {
        this.id = value.id;
        this.content = value.content;
        this.uri = value.uri;
        this.filename = value.filename;
    }

    public Attachment(
        Integer id,
        byte[]  content,
        String  uri,
        String  filename
    ) {
        this.id = id;
        this.content = content;
        this.uri = uri;
        this.filename = filename;
    }

    /**
     * Getter for <code>admin.attachment.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.attachment.id</code>.
     */
    public Attachment setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.attachment.content</code>.
     */
    public byte[] getContent() {
        return this.content;
    }

    /**
     * Setter for <code>admin.attachment.content</code>.
     */
    public Attachment setContent(byte[] content) {
        this.content = content;
        return this;
    }

    /**
     * Getter for <code>admin.attachment.uri</code>.
     */
    @Size(max = 500)
    public String getUri() {
        return this.uri;
    }

    /**
     * Setter for <code>admin.attachment.uri</code>.
     */
    public Attachment setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * Getter for <code>admin.attachment.filename</code>.
     */
    @Size(max = 500)
    public String getFilename() {
        return this.filename;
    }

    /**
     * Setter for <code>admin.attachment.filename</code>.
     */
    public Attachment setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Attachment (");

        sb.append(id);
        sb.append(", ").append("[binary...]");
        sb.append(", ").append(uri);
        sb.append(", ").append(filename);

        sb.append(")");
        return sb.toString();
    }
}
