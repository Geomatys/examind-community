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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.Attachment;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AttachmentRecord extends UpdatableRecordImpl<AttachmentRecord> implements Record4<Integer, byte[], String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.attachment.id</code>.
     */
    public AttachmentRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.attachment.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.attachment.content</code>.
     */
    public AttachmentRecord setContent(byte[] value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.attachment.content</code>.
     */
    public byte[] getContent() {
        return (byte[]) get(1);
    }

    /**
     * Setter for <code>admin.attachment.uri</code>.
     */
    public AttachmentRecord setUri(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.attachment.uri</code>.
     */
    @Size(max = 500)
    public String getUri() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.attachment.filename</code>.
     */
    public AttachmentRecord setFilename(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.attachment.filename</code>.
     */
    @Size(max = 500)
    public String getFilename() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, byte[], String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, byte[], String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Attachment.ATTACHMENT.ID;
    }

    @Override
    public Field<byte[]> field2() {
        return Attachment.ATTACHMENT.CONTENT;
    }

    @Override
    public Field<String> field3() {
        return Attachment.ATTACHMENT.URI;
    }

    @Override
    public Field<String> field4() {
        return Attachment.ATTACHMENT.FILENAME;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public byte[] component2() {
        return getContent();
    }

    @Override
    public String component3() {
        return getUri();
    }

    @Override
    public String component4() {
        return getFilename();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public byte[] value2() {
        return getContent();
    }

    @Override
    public String value3() {
        return getUri();
    }

    @Override
    public String value4() {
        return getFilename();
    }

    @Override
    public AttachmentRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public AttachmentRecord value2(byte[] value) {
        setContent(value);
        return this;
    }

    @Override
    public AttachmentRecord value3(String value) {
        setUri(value);
        return this;
    }

    @Override
    public AttachmentRecord value4(String value) {
        setFilename(value);
        return this;
    }

    @Override
    public AttachmentRecord values(Integer value1, byte[] value2, String value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AttachmentRecord
     */
    public AttachmentRecord() {
        super(Attachment.ATTACHMENT);
    }

    /**
     * Create a detached, initialised AttachmentRecord
     */
    public AttachmentRecord(Integer id, byte[] content, String uri, String filename) {
        super(Attachment.ATTACHMENT);

        setId(id);
        setContent(content);
        setUri(uri);
        setFilename(filename);
    }
}
