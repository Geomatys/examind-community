/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.DataEnvelopeRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function4;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.data_envelope
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DataEnvelope extends TableImpl<DataEnvelopeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.data_envelope</code>
     */
    public static final DataEnvelope DATA_ENVELOPE = new DataEnvelope();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DataEnvelopeRecord> getRecordType() {
        return DataEnvelopeRecord.class;
    }

    /**
     * The column <code>admin.data_envelope.data_id</code>.
     */
    public final TableField<DataEnvelopeRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_envelope.dimension</code>.
     */
    public final TableField<DataEnvelopeRecord, Integer> DIMENSION = createField(DSL.name("dimension"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.data_envelope.min</code>.
     */
    public final TableField<DataEnvelopeRecord, Double> MIN = createField(DSL.name("min"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>admin.data_envelope.max</code>.
     */
    public final TableField<DataEnvelopeRecord, Double> MAX = createField(DSL.name("max"), SQLDataType.DOUBLE.nullable(false), this, "");

    private DataEnvelope(Name alias, Table<DataEnvelopeRecord> aliased) {
        this(alias, aliased, null);
    }

    private DataEnvelope(Name alias, Table<DataEnvelopeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.data_envelope</code> table reference
     */
    public DataEnvelope(String alias) {
        this(DSL.name(alias), DATA_ENVELOPE);
    }

    /**
     * Create an aliased <code>admin.data_envelope</code> table reference
     */
    public DataEnvelope(Name alias) {
        this(alias, DATA_ENVELOPE);
    }

    /**
     * Create a <code>admin.data_envelope</code> table reference
     */
    public DataEnvelope() {
        this(DSL.name("data_envelope"), null);
    }

    public <O extends Record> DataEnvelope(Table<O> child, ForeignKey<O, DataEnvelopeRecord> key) {
        super(child, key, DATA_ENVELOPE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DataEnvelopeRecord> getPrimaryKey() {
        return Keys.DATA_ENVELOPE_PK;
    }

    @Override
    public List<ForeignKey<DataEnvelopeRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATA_ENVELOPE__DATA_ENVELOPE_DATA_FK);
    }

    private transient Data _data;

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.DATA_ENVELOPE__DATA_ENVELOPE_DATA_FK);

        return _data;
    }

    @Override
    public DataEnvelope as(String alias) {
        return new DataEnvelope(DSL.name(alias), this);
    }

    @Override
    public DataEnvelope as(Name alias) {
        return new DataEnvelope(alias, this);
    }

    @Override
    public DataEnvelope as(Table<?> alias) {
        return new DataEnvelope(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DataEnvelope rename(String name) {
        return new DataEnvelope(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataEnvelope rename(Name name) {
        return new DataEnvelope(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DataEnvelope rename(Table<?> name) {
        return new DataEnvelope(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, Integer, Double, Double> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function4<? super Integer, ? super Integer, ? super Double, ? super Double, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function4<? super Integer, ? super Integer, ? super Double, ? super Double, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
